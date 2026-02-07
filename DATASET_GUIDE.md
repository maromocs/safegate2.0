# Dataset Testing Guide

Complete guide for preparing, uploading, and testing security datasets with SafeGate.

## Table of Contents

- [Overview](#overview)
- [Supported Formats](#supported-formats)
- [Dataset Preparation](#dataset-preparation)
- [Testing Datasets](#testing-datasets)
- [Interpreting Results](#interpreting-results)
- [Example Datasets](#example-datasets)
- [Best Practices](#best-practices)

---

## Overview

SafeGate allows you to test LLM detection capabilities against security datasets containing attack payloads. The system:

1. **Extracts** payloads from your uploaded file
2. **Analyzes** each payload with the configured LLM model
3. **Records** which payloads were detected as malicious vs. safe
4. **Reports** detection rates, category breakdowns, and missed payloads

**Important**: SafeGate does NOT send HTTP requests. It directly analyzes the payload strings with the LLM.

---

## Supported Formats

### CSV (Comma-Separated Values)

**Best for**: Structured datasets with metadata

**Requirements**:
- Must have a header row
- Must contain a column named `payload` (case-insensitive)
- Values can be quoted or unquoted

**Example**:
```csv
id,payload,attack_type,label
1,"' OR 1=1 --",SQLi,malicious
2,"<script>alert(1)</script>",XSS,malicious
3,"../../etc/passwd",PathTraversal,malicious
4,"normal query",None,benign
```

**Alternative**: If no `payload` column exists, the **last column** will be used.

### JSON

**Best for**: Programmatically generated datasets

**Format 1 - Array of Strings**:
```json
[
  "' OR 1=1 --",
  "<script>alert(1)</script>",
  "../../etc/passwd"
]
```

**Format 2 - Array of Objects**:
```json
[
  {"payload": "' OR 1=1 --", "type": "SQLi"},
  {"payload": "<script>alert(1)</script>", "type": "XSS"},
  {"payload": "../../etc/passwd", "type": "PathTraversal"}
]
```

### XML

**Best for**: Legacy datasets or SOAP-based attack collections

**Format 1 - Element Content**:
```xml
<payloads>
  <payload>' OR 1=1 --</payload>
  <payload><script>alert(1)</script></payload>
  <payload>../../etc/passwd</payload>
</payloads>
```

**Format 2 - Attributes**:
```xml
<attacks>
  <attack payload="' OR 1=1 --" type="SQLi"/>
  <attack payload="&lt;script&gt;alert(1)&lt;/script&gt;" type="XSS"/>
  <attack payload="../../etc/passwd" type="PathTraversal"/>
</attacks>
```

### TXT (Plain Text)

**Best for**: Simple payload lists

**Format**: One payload per line
```
' OR 1=1 --
<script>alert(1)</script>
../../etc/passwd
../../../windows/system32
```

**Note**: Empty lines are skipped

### TSV (Tab-Separated Values)

**Best for**: Excel exports or tab-delimited data

**Requirements**: Same as CSV but uses tabs instead of commas
```tsv
id	payload	attack_type
1	' OR 1=1 --	SQLi
2	<script>alert(1)</script>	XSS
```

---

## Dataset Preparation

### Step 1: Choose Your Format

- **CSV**: Best for most use cases, widely supported
- **JSON**: Best for programmatic generation
- **TXT**: Simplest, one payload per line
- **XML**: For compatibility with existing XML datasets
- **TSV**: For Excel/spreadsheet exports

### Step 2: Structure Your Data

#### CSV Example

```csv
"index","payload","attack_type","label"
"1","' OR 1=1 --","SQLi","malicious"
"2","admin' --","SQLi","malicious"
"3","<script>alert(XSS)</script>","XSS","malicious"
"4","normal_value","None","benign"
```

**Column names can be anything**, but `payload` column is required!

#### JSON Example

```json
[
  {
    "id": 1,
    "payload": "' OR 1=1 --",
    "type": "SQLi",
    "source": "manual"
  },
  {
    "id": 2,
    "payload": "<script>alert(XSS)</script>",
    "type": "XSS",
    "source": "fuzzdb"
  }
]
```

### Step 3: Validate Your Dataset

Before uploading, verify:

✅ **CSV/TSV**: Has header row with `payload` column
✅ **JSON**: Valid JSON syntax (use [JSONLint](https://jsonlint.com/))
✅ **XML**: Well-formed XML (matching open/close tags)
✅ **TXT**: UTF-8 encoding, one payload per line
✅ **All formats**: File size < 100 MB

### Step 4: Test a Sample

Start with a small sample (10-100 payloads) to:
- Verify format is correct
- Check LLM detection accuracy
- Estimate processing time for full dataset

---

## Testing Datasets

### Via Web Interface

1. **Navigate** to http://localhost:8080/testing.html

2. **Upload Dataset**
   - Click "Choose File"
   - Select your dataset file

3. **Configure Test**
   - **Format**: Select "Auto-detect" (recommended) or specify format
   - **Attack Type**: Enter a tag (e.g., "SQLi", "XSS", "CSIC2010")
   - **Sampling**: Choose sample size
     - `All` - Test every payload
     - `Random 100` - Random sample of 100
     - `Random 1,000` - Random sample of 1,000
     - `Random 10,000` - Random sample of 10,000

4. **Optional: Deterministic Sampling**
   - Expand "Advanced Options"
   - Enter a seed number (e.g., `42`, `12345`)
   - Same seed = same sample (reproducible results)

5. **Start Test**
   - Click "Start Dataset Test"
   - Monitor progress (batch processing)
   - Wait for completion

6. **View Results**
   - Results appear in "Completed Test Runs" section
   - Click "View Details" to see:
     - Detection breakdown by category
     - Passed payloads (what LLM missed)
     - LLM statistics

### Via API (Automation)

```bash
curl -X POST http://localhost:8080/api/tests/start-dataset-test \
  -F "file=@/path/to/dataset.csv" \
  -F "datasetFormat=AUTO" \
  -F "attackTypeTag=SQLi" \
  -F "samplingSize=Random 100" \
  -F "seed=42"
```

**Response**:
```json
{
  "testRun": {
    "id": 123,
    "startTime": "2026-02-06T10:30:00",
    "endTime": "2026-02-06T10:31:00",
    "totalPassed": 30,
    "totalBlocked": 70,
    "datasetFileName": "sqli-payloads.csv",
    "datasetFormat": "CSV",
    "attackTypeTag": "SQLi",
    "samplingSize": "Random 100"
  },
  "llmStats": {
    "total": 30,
    "malicious": 25,
    "safe": 5,
    "byCategory": {
      "SQLi": 20,
      "SQLI": 5
    }
  }
}
```

---

## Interpreting Results

### Test Run Summary

| Field | Description |
|-------|-------------|
| **Total Tested** | Number of payloads analyzed |
| **Blocked** | Payloads detected as malicious |
| **Passed** | Payloads classified as safe (potential bypasses) |
| **Block Rate** | Percentage of blocked payloads |

### LLM Analysis (if enabled)

| Field | Description |
|-------|-------------|
| **Total Malicious Requests** | How many payloads LLM analyzed |
| **LLM Blocked** | Payloads LLM classified as malicious |
| **LLM Detection Rate** | Percentage LLM caught |
| **By Category** | Breakdown by attack type (SQLi, XSS, etc.) |

### Detection Breakdown

Shows which detection categories caught payloads:

```
Detection Breakdown:
- LLM:SQLi: 45 requests
- LLM:XSS: 20 requests
- LLM:CMDI: 5 requests
```

### Passed Payloads

**Critical for security research!**

These are payloads the LLM classified as "safe" - potential bypasses or false negatives. Review these to:
- Identify LLM blind spots
- Create evasion datasets
- Improve prompts
- Fine-tune models

---

## Example Datasets

### SQLi Payloads (CSV)

```csv
payload,description
"' OR '1'='1",Classic SQLi
"admin'--",Comment-based bypass
"1' UNION SELECT NULL--",Union-based injection
"' AND 1=1--",Boolean-based blind
"'; DROP TABLE users--",Stacked query
```

### XSS Payloads (TXT)

```
<script>alert(1)</script>
<img src=x onerror=alert(1)>
<svg onload=alert(1)>
javascript:alert(1)
<iframe src="javascript:alert(1)">
```

### Mixed Attacks (JSON)

```json
[
  {"payload": "' OR 1=1 --", "type": "SQLi", "severity": "high"},
  {"payload": "<script>alert(XSS)</script>", "type": "XSS", "severity": "high"},
  {"payload": "../../etc/passwd", "type": "PathTraversal", "severity": "critical"},
  {"payload": "; cat /etc/passwd", "type": "CMDI", "severity": "critical"}
]
```

### CSIC 2010 Dataset

The [CSIC 2010 HTTP Dataset](http://www.isi.csic.es/dataset/) is a popular WAF testing dataset.

**Format**: CSV with these columns:
- index, method, url, protocol, userAgent, pragma, cacheControl, accept, acceptEncoding, acceptCharset, acceptLanguage, host, connection, contentLength, contentType, cookie, **payload**, label

**To use**:
1. Download CSV from CSIC 2010
2. Upload to SafeGate
3. Select "Auto-detect" format
4. Tag as "CSIC2010" or "HTTP"
5. Choose sampling (dataset is large: ~36,000 entries)

**Sampling recommendations**:
- `Random 100` - Quick test
- `Random 1,000` - Standard benchmark
- `All` - Comprehensive evaluation (takes time)

---

## Best Practices

### 1. Start Small

- Begin with 10-100 payloads
- Verify format is correct
- Check LLM is detecting appropriately
- Estimate time for full dataset

### 2. Use Sampling for Large Datasets

- Datasets > 1,000 payloads: use sampling
- `Random 100` for quick tests
- `Random 1,000` for benchmarks
- `All` only when needed

### 3. Tag Your Tests

Always use the "Attack Type" tag:
- Organizes results
- Enables comparisons
- Makes analysis easier

**Good tags**: `SQLi`, `XSS`, `CSIC2010`, `Fuzzdb-SQLi`, `Custom-Bypasses`

### 4. Use Deterministic Sampling for Reproducibility

For research or comparisons:
- Set a seed value (e.g., `42`)
- Same seed = same sample every time
- Document your seed in papers/reports

### 5. Review Passed Payloads

**Most important for security research!**

Passed payloads = potential bypasses. Always:
- Export passed payloads
- Analyze why they weren't detected
- Use them to improve detection
- Build bypass technique libraries

### 6. Compare Models

Test the same dataset with different models:

```
Dataset: sqli-1000.csv | Seed: 42

Model          | Block Rate | Time
---------------|-----------|------
tinyllama      | 65%       | 2min
phi3:mini      | 78%       | 5min
mistral        | 85%       | 8min
llama3.2:3b    | 89%       | 10min
```

### 7. Clean Your Data

Remove duplicates and normalize:

```python
# Python script to clean CSV
import pandas as pd

df = pd.read_csv('payloads.csv')
df = df.drop_duplicates(subset=['payload'])
df['payload'] = df['payload'].str.strip()
df.to_csv('payloads_clean.csv', index=False)
```

### 8. Version Your Datasets

Track dataset versions:
- `sqli-payloads-v1.csv`
- `sqli-payloads-v2-cleaned.csv`
- `sqli-payloads-v3-bypasses.csv`

### 9. Document Results

Create a testing log:

```
Date: 2026-02-06
Dataset: fuzzdb-sqli.csv (1000 payloads)
Model: mistral
Mode: TEST_ONLY
Seed: 42
Results: 850/1000 detected (85%)
Notes: Weak on Unicode encoding bypasses
```

### 10. Export and Share

SafeGate allows exporting results:
- Passed payloads → CSV
- Malicious payloads → CSV
- Full results → JSON (via API)

Share datasets with the community!

---

## Troubleshooting

### "No payloads extracted"

**Causes**:
- CSV missing `payload` column
- JSON not in expected format
- TXT file is empty
- Wrong format selected

**Solutions**:
- Check CSV has header row with `payload` column
- Validate JSON syntax
- Use "Auto-detect" format
- Check file encoding (should be UTF-8)

### "Format detection failed"

**Causes**:
- File format doesn't match extension
- Corrupted file
- Unsupported encoding

**Solutions**:
- Specify format manually
- Re-save file in correct format
- Convert to UTF-8 encoding

### "Processing too slow"

**Causes**:
- Large dataset without sampling
- Slow LLM model
- CPU mode (no GPU)

**Solutions**:
- Use sampling (`Random 1,000`)
- Switch to faster model (tinyllama)
- Enable GPU acceleration
- Process in batches

### "Out of memory"

**Causes**:
- Dataset too large
- Model too large for available RAM

**Solutions**:
- Use sampling
- Switch to smaller model
- Increase Docker memory limit
- Process in smaller batches

---

## Next Steps

- **[SETUP_GUIDE.md](SETUP_GUIDE.md)** - Configure LLM models and GPU
- **[README.md](README.md)** - Return to main documentation

---

## Example Workflow

**Complete example from start to finish:**

1. **Prepare dataset** (`sqli-test.csv`):
   ```csv
   id,payload,expected
   1,"' OR 1=1 --",malicious
   2,"admin'--",malicious
   3,"normal query",benign
   ```

2. **Upload to SafeGate**:
   - Go to http://localhost:8080/testing.html
   - Choose file: `sqli-test.csv`
   - Format: Auto-detect
   - Attack Type: `SQLi-Test`
   - Sampling: `All`

3. **Start test** and wait for completion

4. **View results** in http://localhost:8080/logs.html:
   - Test ID: 123
   - Blocked: 2/3 (66.7%)
   - Passed: 1/3

5. **Click "View Details"** to see which payload passed

6. **Export passed payloads** for analysis

7. **Try different model** and compare results

