from fastapi import FastAPI
from pydantic import BaseModel, Field
import uvicorn
import os
import httpx
import asyncio
import json
from typing import List, Optional, Dict, Any

app = FastAPI()

# Environment configuration
ANALYZER_BACKEND = os.getenv("ANALYZER_BACKEND", "mock").lower()  # mock | ollama
OLLAMA_HOST = os.getenv("OLLAMA_HOST", "http://host.docker.internal:11434")
DEFAULT_MODEL = os.getenv("OLLAMA_MODEL", "tinyllama")
BATCH_SIZE = int(os.getenv("ANALYZER_BATCH_SIZE", "20"))
REQUEST_TIMEOUT = float(os.getenv("ANALYZER_TIMEOUT_SECONDS", "25"))

SUPPORTED_MODELS = [
    "tinyllama",
    "phi",
    "phi3:mini",
    "mistral",
    "llama2",
    "llama3.2:3b-instruct",
]

class RequestPayload(BaseModel):
    payload: str
    provider: Optional[str] = Field(default=None, description="Provider name, e.g., 'ollama'")
    model: Optional[str] = Field(default=None, description="Model name if provider supports it")
    gpu_enabled: Optional[bool] = Field(default=None, description="If true, hint GPU usage; if false, force CPU")

class AnalysisResponse(BaseModel):
    is_malicious: bool
    category: str
    reason: str

class BatchRequest(BaseModel):
    payloads: List[str]
    provider: Optional[str] = None
    model: Optional[str] = None
    gpu_enabled: Optional[bool] = None

class SingleResult(BaseModel):
    payload: str
    is_malicious: bool
    category: str
    reason: str

class BatchStats(BaseModel):
    total: int
    malicious: int
    safe: int
    byCategory: Dict[str, int]

class BatchResponse(BaseModel):
    results: List[SingleResult]
    stats: BatchStats

class PullModelRequest(BaseModel):
    model: str

# In-memory model pull progress tracking
PULL_STATE: Dict[str, Dict[str, Any]] = {}

async def _track_pull_progress(model: str):
    # Initialize state
    PULL_STATE[model] = {"status": "starting", "percent": 0, "completed": 0, "total": 0}
    try:
        async with httpx.AsyncClient(timeout=None) as client:
            # Ollama streams NDJSON with progress
            async with client.stream("POST", f"{OLLAMA_HOST}/api/pull", json={"name": model, "stream": True}) as r:
                async for line in r.aiter_lines():
                    if not line:
                        continue
                    try:
                        obj = json.loads(line)
                    except Exception:
                        continue
                    status = obj.get("status") or obj.get("status_text") or "pulling"
                    total = obj.get("total") or obj.get("size") or obj.get("total_bytes") or 0
                    completed = obj.get("completed") or obj.get("done") or obj.get("completed_bytes") or 0
                    percent = 0
                    try:
                        if total and isinstance(total, (int, float)) and isinstance(completed, (int, float)):
                            percent = int((completed / total) * 100)
                    except Exception:
                        percent = PULL_STATE.get(model, {}).get("percent", 0)
                    PULL_STATE[model] = {
                        "status": status,
                        "percent": max(0, min(100, percent)),
                        "completed": int(completed) if isinstance(completed, (int, float)) else 0,
                        "total": int(total) if isinstance(total, (int, float)) else 0,
                    }
        # Mark as completed if stream ends cleanly
        PULL_STATE[model]["status"] = "completed"
        PULL_STATE[model]["percent"] = 100
    except Exception as e:
        PULL_STATE[model]["status"] = "error"
        PULL_STATE[model]["message"] = str(e)

async def _ollama_generate(prompt: str, model: str, gpu_enabled: Optional[bool] = None) -> str:
    url = f"{OLLAMA_HOST}/api/generate"
    options: Dict[str, Any] = {"temperature": 0}
    # If gpu_enabled is explicitly provided, set num_gpu accordingly (0 for CPU).
    if gpu_enabled is False:
        options["num_gpu"] = 0
    elif gpu_enabled is True:
        # Hint to use GPU; number may be adjusted by Ollama/runtime
        options["num_gpu"] = 1
    body = {
        "model": model,
        "prompt": prompt,
        "stream": False,
        "options": options
    }
    async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT) as client:
        r = await client.post(url, json=body)
        r.raise_for_status()
        data = r.json()
        return (data.get("response") or "").strip()

async def _ollama_pull_if_needed(model: str) -> Optional[str]:
    # Try a quick pull; if Ollama supports streaming progress, we don't consume it here; best-effort
    try:
        url = f"{OLLAMA_HOST}/api/pull"
        async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT) as client:
            await client.post(url, json={"name": model})
        return None
    except Exception as e:
        return str(e)

CATEGORIES = [
    "SQL_INJECTION","XSS","PATH_TRAVERSAL","OTHER","SAFE"
]

def _build_prompt(payload: str) -> str:
    return (
        "You are a precise web security classifier for HTTP request summaries.\n"
        "Decide a VERDICT and a CATEGORY.\n\n"
        "Rules:\n"
        "- VERDICT must be exactly SAFE or MALICIOUS.\n"
        "- CATEGORY must be one of:\n"
        "  - SQL_INJECTION\n  - XSS\n  - PATH_TRAVERSAL\n  - OTHER\n"
        "- If VERDICT is SAFE, do NOT provide a CATEGORY (leave it blank or omit it).\n"
        "- If VERDICT is MALICIOUS, CATEGORY should be SQL_INJECTION, XSS, or PATH_TRAVERSAL if it matches; otherwise OTHER.\n"
        "- Output format MUST be exactly two lines:\n"
        "  VERDICT:<SAFE|MALICIOUS>\n"
        "  CATEGORY:<ONE_OF_THE_ABOVE_OR_BLANK_IF_SAFE>\n\n"
        f"Request:\n{payload}\n"
    )

async def _classify_with_ollama(payload: str, model: str, gpu_enabled: Optional[bool] = None) -> AnalysisResponse:
    prompt = _build_prompt(payload)
    # Best-effort auto-pull before generation
    try:
        await _ollama_pull_if_needed(model)
    except Exception:
        pass
    try:
        text = await _ollama_generate(prompt, model, gpu_enabled)
    except httpx.HTTPStatusError as he:
        if he.response is not None and he.response.status_code in (400, 404, 500):
            _ = await _ollama_pull_if_needed(model)
            try:
                text = await _ollama_generate(prompt, model)
            except Exception as e2:
                return AnalysisResponse(is_malicious=False, category="SAFE", reason=f"Ollama error after pull attempt: {e2}")
        else:
            return AnalysisResponse(is_malicious=False, category="SAFE", reason=f"Ollama HTTP error: {he}")
    except Exception as e:
        return AnalysisResponse(is_malicious=False, category="SAFE", reason=f"Ollama error: {e}")

    # Parse strict two-line output
    verdict = None
    category = None
    lines = [ln.strip() for ln in text.strip().splitlines() if ln.strip()]
    for ln in lines[:2]:
        up = ln.upper()
        if up.startswith("VERDICT:"):
            val = up.split(":", 1)[1].strip()
            if val in ("SAFE", "MALICIOUS"):
                verdict = val
        if up.startswith("CATEGORY:"):
            val = up.split(":", 1)[1].strip()
            if val in CATEGORIES:
                category = val
    if verdict is None:
        # try to infer from any mention
        upall = text.upper()
        if "MALICIOUS" in upall and "SAFE" not in upall:
            verdict = "MALICIOUS"
        elif "SAFE" in upall:
            verdict = "SAFE"

    # If category is still None, try to infer from any mention in the text
    if category is None:
        upall = text.upper()
        # Prioritize specific malicious categories
        if "SQL_INJECTION" in upall:
            category = "SQL_INJECTION"
        elif "XSS" in upall:
            category = "XSS"
        elif "PATH_TRAVERSAL" in upall:
            category = "PATH_TRAVERSAL"
        elif "OTHER" in upall:
            category = "OTHER"

    # Rule: If category is one of the malicious ones, it is malicious
    if category in ("SQL_INJECTION", "XSS", "PATH_TRAVERSAL", "OTHER"):
        return AnalysisResponse(is_malicious=True, category=category, reason=f"LLM classified as {category}")

    if verdict == "MALICIOUS":
        return AnalysisResponse(is_malicious=True, category="OTHER", reason="LLM classified as MALICIOUS")

    if verdict == "SAFE":
        return AnalysisResponse(is_malicious=False, category="SAFE", reason="LLM classified as SAFE")

    # Fallback heuristic if parsing failed badly
    pl = payload.lower()
    if any(k in pl for k in ["<script", "javascript:"]):
        return AnalysisResponse(is_malicious=True, category="XSS", reason="Heuristic: XSS tokens present")
    if any(k in pl for k in ["union select", " or 1=1", "' or '1'='1", "-- ", " drop "]):
        return AnalysisResponse(is_malicious=True, category="SQL_INJECTION", reason="Heuristic: SQLi tokens present")
    if "../" in pl:
        return AnalysisResponse(is_malicious=True, category="PATH_TRAVERSAL", reason="Heuristic: path traversal")
    return AnalysisResponse(is_malicious=False, category="SAFE", reason=f"Unclear model response '{text[:40]}', defaulting SAFE")

async def analyze_with_backend(payload: str, provider: Optional[str], model: Optional[str], gpu_enabled: Optional[bool] = None) -> AnalysisResponse:
    provider = (provider or ("ollama" if ANALYZER_BACKEND == "ollama" else "mock")).lower()
    model = model or DEFAULT_MODEL

    if provider == "ollama" and ANALYZER_BACKEND == "ollama":
        # Clamp to supported models if provided
        chosen_model = model if model in SUPPORTED_MODELS else DEFAULT_MODEL
        return await _classify_with_ollama(payload, chosen_model, gpu_enabled)
    else:
        # mock backend: simple heuristic with categories
        pl = payload.lower()
        if any(k in pl for k in ["<script", "javascript:"]):
            return AnalysisResponse(is_malicious=True, category="XSS", reason="Mock heuristic: XSS tokens")
        if any(k in pl for k in ["union select", " or 1=1", "' or '1'='1", "-- ", " drop "]):
            return AnalysisResponse(is_malicious=True, category="SQL_INJECTION", reason="Mock heuristic: SQLi tokens")
        if "../" in pl:
            return AnalysisResponse(is_malicious=True, category="PATH_TRAVERSAL", reason="Mock heuristic: path traversal")
        return AnalysisResponse(is_malicious=False, category="SAFE", reason="Mock: appears safe")

@app.post("/analyze", response_model=AnalysisResponse)
async def analyze_payload(request: RequestPayload):
    return await analyze_with_backend(request.payload, request.provider, request.model, request.gpu_enabled)

@app.post("/analyze/batch", response_model=BatchResponse)
async def analyze_batch(request: BatchRequest):
    payloads = request.payloads or []
    provider = request.provider
    model = request.model
    gpu_enabled = request.gpu_enabled

    all_results: List[SingleResult] = []
    total = len(payloads)
    malicious = 0
    safe = 0
    by_cat: Dict[str, int] = {c: 0 for c in CATEGORIES}

    # process in chunks
    for i in range(0, total, max(1, BATCH_SIZE)):
        chunk = payloads[i:i + BATCH_SIZE]
        # Log progress
        print(f"Processed {min(i + len(chunk), total)}/{total} dataset payloads via LLM... provider={provider or ANALYZER_BACKEND}, model={model or DEFAULT_MODEL}")
        for p in chunk:
            res = await analyze_with_backend(p, provider, model, gpu_enabled)
            all_results.append(SingleResult(payload=p, is_malicious=res.is_malicious, category=res.category, reason=res.reason))
            if res.is_malicious:
                malicious += 1
            else:
                safe += 1
            # Count by category
            cat = (res.category or "OTHER").upper()
            if cat not in by_cat:
                by_cat["OTHER"] = by_cat.get("OTHER", 0) + 1
            else:
                by_cat[cat] = by_cat.get(cat, 0) + 1

    return BatchResponse(
        results=all_results,
        stats=BatchStats(total=total, malicious=malicious, safe=safe, byCategory=by_cat)
    )

@app.get("/models")
async def get_models():
    recommended = SUPPORTED_MODELS
    available: List[str] = []
    if ANALYZER_BACKEND != "ollama":
        return {"available": available, "recommended": recommended}
    try:
        async with httpx.AsyncClient(timeout=REQUEST_TIMEOUT) as client:
            r = await client.get(f"{OLLAMA_HOST}/api/tags")
            r.raise_for_status()
            data = r.json() or {}
            models = data.get("models") or []
            for m in models:
                name = m.get("name") or m.get("model")
                if isinstance(name, str):
                    available.append(name)
    except Exception:
        # Non-fatal; just return what we have
        pass
    return {"available": available, "recommended": recommended}

@app.post("/models/pull")
async def pull_model(req: PullModelRequest):
    model = req.model
    try:
        # start background progress tracking
        asyncio.create_task(_track_pull_progress(model))
        return {"status": "requested", "model": model}
    except Exception as e:
        return {"status": "error", "model": model, "message": str(e)}

@app.get("/models/pull/progress")
async def pull_progress(model: str):
    st = PULL_STATE.get(model)
    if not st:
        return {"status": "unknown", "percent": 0, "completed": 0, "total": 0}
    return st

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=5000)