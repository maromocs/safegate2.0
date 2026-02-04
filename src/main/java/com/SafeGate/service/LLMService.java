package com.SafeGate.service;

import com.SafeGate.enums.LLMMode;
import com.SafeGate.model.LLMConfig;
import com.SafeGate.repository.LLMConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
public class LLMService {

    @Autowired
    private LLMConfigRepository configRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // Simple in-memory cache of the singleton config
    private volatile LLMConfig cachedConfig;

    public Optional<LLMConfig> getConfig() {
        // Return cached if available
        if (cachedConfig != null) {
            return Optional.of(cachedConfig);
        }
        Optional<LLMConfig> saved = configRepository.findAll().stream().findFirst();
        if (saved.isPresent()) {
            cachedConfig = saved.get();
            return saved;
        }
        // Build a default config from environment for convenience
        String url = System.getenv("LLM_ANALYZER_URL");
        LLMConfig cfg = new LLMConfig();
        if (url != null && !url.isBlank()) {
            cfg.setLlmApiUrl(url);
        }
        cachedConfig = cfg;
        return Optional.of(cfg);
    }

    public LLMConfig saveConfig(LLMConfig config) {
        configRepository.deleteAll(); // Singleton config
        LLMConfig saved = configRepository.save(config);
        cachedConfig = saved; // refresh cache immediately
        return saved;
    }

    public boolean isMalicious(String payload) {
        Map res = analyzeSingle(payload);
        if (res == null) return false;
        Object val = res.get("is_malicious");
        return (val instanceof Boolean) ? (Boolean) val : false;
    }

    /**
     * Analyze a single payload and return full response map: { is_malicious, category, reason }
     */
    public Map analyzeSingle(String payload) {
        Optional<LLMConfig> configOpt = getConfig();
        if (configOpt.isEmpty() || configOpt.get().getLlmApiUrl() == null) {
            return Map.of("is_malicious", false, "category", "SAFE", "reason", "LLM disabled");
        }
        LLMConfig config = configOpt.get();
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("payload", payload);
            if (config.getProvider() != null) request.put("provider", config.getProvider());
            if (config.getModel() != null) request.put("model", config.getModel());
            request.put("gpu_enabled", Boolean.TRUE.equals(config.getGpuEnabled()));
            Map response = restTemplate.postForObject(config.getLlmApiUrl(), request, Map.class);
            if (response == null) return Map.of("is_malicious", false, "category", "SAFE", "reason", "No response");
            return response;
        } catch (Exception e) {
            return Map.of("is_malicious", false, "category", "SAFE", "reason", "Analyzer error: "+e.getMessage());
        }
    }

    private String analyzerBaseUrl(String analyzeUrl) {
        if (analyzeUrl == null) return null;
        if (analyzeUrl.endsWith("/analyze")) return analyzeUrl.substring(0, analyzeUrl.length()-8);
        return analyzeUrl;
    }

    /**
     * Get available and recommended models from analyzer.
     */
    public Map getModels() {
        Optional<LLMConfig> configOpt = getConfig();
        String[] recommended = new String[] {"tinyllama","phi","phi3:mini","mistral","llama2","llama3.2:3b-instruct"};
        if (configOpt.isEmpty() || configOpt.get().getLlmApiUrl() == null) {
            return Map.of("available", List.of(), "recommended", List.of(recommended));
        }
        String base = analyzerBaseUrl(configOpt.get().getLlmApiUrl());
        try {
            Map res = restTemplate.getForObject(base + "/models", Map.class);
            if (res == null) return Map.of("available", List.of(), "recommended", List.of(recommended));
            Object avail = res.get("available");
            Object rec = res.get("recommended");
            List a = (avail instanceof List) ? (List) avail : List.of();
            List r = (rec instanceof List) ? (List) rec : List.of(recommended);
            return Map.of("available", a, "recommended", r);
        } catch (Exception e) {
            return Map.of("available", List.of(), "recommended", List.of(recommended));
        }
    }

    /**
     * Request analyzer to pull a model.
     */
    public Map pullModel(String model) {
        Optional<LLMConfig> configOpt = getConfig();
        if (configOpt.isEmpty() || configOpt.get().getLlmApiUrl() == null) {
            return Map.of("status", "error", "message", "Analyzer URL not configured");
        }
        String base = analyzerBaseUrl(configOpt.get().getLlmApiUrl());
        try {
            Map<String, Object> req = new HashMap<>();
            req.put("model", model);
            return restTemplate.postForObject(base + "/models/pull", req, Map.class);
        } catch (Exception e) {
            return Map.of("status", "error", "message", e.getMessage());
        }
    }

    /**
     * Get progress for a model pull from analyzer.
     */
    public Map getModelPullProgress(String model) {
        Optional<LLMConfig> configOpt = getConfig();
        if (configOpt.isEmpty() || configOpt.get().getLlmApiUrl() == null) {
            return Map.of("status", "unknown", "percent", 0, "completed", 0, "total", 0);
        }
        String base = analyzerBaseUrl(configOpt.get().getLlmApiUrl());
        try {
            String url = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(base + "/models/pull/progress")
                    .queryParam("model", model)
                    .build()
                    .encode()
                    .toUriString();

            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            return Map.of("status", "unknown", "percent", 0, "completed", 0, "total", 0);
        }
    }

    /**
     * Analyze a batch of payloads via analyzer /analyze/batch endpoint.
     * Returns a map containing results and stats per analyzer response.
     */
    public Map analyzeBatch(List<String> payloads) {
        Optional<LLMConfig> configOpt = getConfig();
        if (configOpt.isEmpty() || configOpt.get().getLlmApiUrl() == null) {
            return Map.of("results", List.of(), "stats", Map.of("total", 0, "malicious", 0, "safe", 0));
        }
        LLMConfig config = configOpt.get();
        String analyzeUrl = config.getLlmApiUrl();
        String batchUrl = analyzeUrl.endsWith("/analyze") ? analyzeUrl + "/batch" : analyzeUrl + "/batch";

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("payloads", payloads);
            if (config.getProvider() != null) request.put("provider", config.getProvider());
            if (config.getModel() != null) request.put("model", config.getModel());
            request.put("gpu_enabled", Boolean.TRUE.equals(config.getGpuEnabled()));
            Map response = restTemplate.postForObject(batchUrl, request, Map.class);
            if (response == null) {
                return Map.of("results", List.of(), "stats", Map.of("total", 0, "malicious", 0, "safe", 0));
            }
            return response;
        } catch (Exception e) {
            return Map.of("results", List.of(), "stats", Map.of("total", payloads.size(), "malicious", 0, "safe", payloads.size()));
        }
    }

    /**
     * Helper to decide if LLM should be used for dataset testing according to mode.
     */
    public boolean isLlmActiveForDataset() {
        LLMMode mode = getConfig().map(LLMConfig::getLlmMode).orElse(LLMMode.DISABLED);
        return mode == LLMMode.TEST_ONLY || mode == LLMMode.NORMAL_AND_TEST;
    }
}