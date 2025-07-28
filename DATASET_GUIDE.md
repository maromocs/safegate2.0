# Dataset Testing Guide for SafeGate WAF

This guide provides comprehensive information on how to use the dataset testing feature of the SafeGate Web Application Firewall (WAF). The feature allows you to test large collections of attack payloads against your WAF rules to evaluate their effectiveness.

## Overview

The dataset testing feature:
- Supports multiple file formats (CSV, JSON, XML, TXT, TSV)
- Automatically detects file formats
- Provides detailed reporting on test results
- Works efficiently by testing payloads directly against rules in memory

## How the WAF Testing Works

The dataset testing feature **does not** use HTTP requests to test the WAF. Instead:

1. It extracts payloads from your uploaded file
2. It directly tests each payload against the WAF rules in memory
3. It records which payloads were blocked and which passed

This is more efficient than sending actual HTTP requests, especially for large datasets.

## Using the Dataset Testing Feature

### Web Interface

1. **Access the Testing Dashboard**:
   - Navigate to the "Testing" page in the SafeGate WAF interface

2. **Upload a Dataset**:
   - In the "Automated Dataset Test" section, click "Choose File" and select your dataset file
   - The system will automatically detect the file format

3. **Configure the Test**:
   - Format: Select "Auto-detect" (recommended) or specify a format
   - Attack Type: Enter a tag to identify the attack type (e.g., "SQLi", "XSS")
   - Sampling: Choose "All" or a sampling option (e.g., "Random 100", "Random 1,000")

4. **Start the Test**:
   - Click "Start New Test"
   - The system will process the dataset and test payloads against your active WAF rules

5. **View Results**:
   - After the test completes, you'll see detailed results in the "Completed Test Runs" section
   - The results will show which payloads were blocked and which passed
   - You can use this information to improve your WAF rules

### Command Line Testing

You can also test datasets using curl:

```bash
curl -X POST http://localhost:8080/api/tests/start-dataset-test \
  -F "file=@/path/to/your/dataset.csv" \
  -F "attackTypeTag=SQLi" \
  -F "samplingSize=Random 100"
```

For more details on the API, see the [API Documentation](API_DOCUMENTATION.md).

## Supported File Formats

### CSV Format

The system expects CSV files to have:

1. **Header Row**: The first row must contain column names
   - Column names can be with or without quotes (e.g., `"payload"` or `payload`)
   - The system looks for a column named "payload" (case-insensitive)

2. **Data Rows**: Each subsequent row contains data, with values separated by commas
   - Values can be with or without quotes
   - Quotes are required if the value contains commas (e.g., `"value, with, commas"`)

3. **Payload Column**: The system looks for a column named "payload" to extract attack payloads
   - If no "payload" column is found, the system will use the last column
   - The payload column should contain the attack payloads you want to test

Example CSV format:
```csv
"index","method","url","protocol","userAgent","pragma","cacheControl","accept","acceptEncoding","acceptCharset","acceptLanguage","host","connection","contentLength","contentType","cookie","payload","label"
"0","GET","http://localhost:8080/tienda1/publico/anadir.jsp","HTTP/1.1","Mozilla/5.0","no-cache","no-cache","text/xml","x-gzip","utf-8","en","localhost:8080","close","null","null","JSESSIONID=123","id=2","anom"
"0","GET","http://localhost:8080/tienda1/publico/anadir.jsp","HTTP/1.1","Mozilla/5.0","no-cache","no-cache","text/xml","x-gzip","utf-8","en","localhost:8080","close","null","null","JSESSIONID=123","nombre=Jam%F3n+Ib%E9rico","anom"
"0","GET","http://localhost:8080/tienda1/publico/anadir.jsp","HTTP/1.1","Mozilla/5.0","no-cache","no-cache","text/xml","x-gzip","utf-8","en","localhost:8080","close","null","null","JSESSIONID=123","precio=85","anom"
"0","GET","http://localhost:8080/tienda1/publico/anadir.jsp","HTTP/1.1","Mozilla/5.0","no-cache","no-cache","text/xml","x-gzip","utf-8","en","localhost:8080","close","null","null","JSESSIONID=123","cantidad=%27%3B+DROP+TABLE+usuarios%3B+SELECT+*+FROM+datos+WHERE+nombre+LIKE+%27%25","anom"
```

### JSON Format

The system can handle JSON files in the following formats:

1. **Array of Strings**:
   ```json
   ["payload1", "payload2", "payload3"]
   ```

2. **Array of Objects with a "payload" Field**:
   ```json
   [
     {"payload": "payload1"},
     {"payload": "payload2"},
     {"payload": "payload3"}
   ]
   ```

### XML Format

The system looks for payloads in XML files in the following ways:

1. **Elements with a "payload" Tag**:
   ```xml
   <payloads>
     <payload>attack payload 1</payload>
     <payload>attack payload 2</payload>
   </payloads>
   ```

2. **Attributes Named "payload"**:
   ```xml
   <attacks>
     <attack payload="attack payload 1" />
     <attack payload="attack payload 2" />
   </attacks>
   ```

### TXT Format

For TXT files, the system expects:
- One payload per line
- No headers or special formatting

Example:
```
' OR 1=1 --
<script>alert(1)</script>
../../../etc/passwd
```

### TSV Format

For TSV (Tab-Separated Values) files, the system follows similar rules to CSV:
- Header row with column names separated by tabs
- Data rows with values separated by tabs
- Looks for a "payload" column

## Using the CSIC 2010 Dataset

The CSIC 2010 dataset is a common dataset for testing WAFs, and the dataset testing feature is designed to work well with it.

### Steps to Test with CSIC 2010 Dataset

1. **Upload the Dataset**:
   - Go to the "Testing" page in the SafeGate WAF interface
   - In the "Automated Dataset Test" section, click "Choose File" and select your CSIC 2010 CSV file
   - The system will automatically detect the file as CSV format

2. **Configure the Test**:
   - Format: Select "Auto-detect" (recommended)
   - Attack Type: Enter "HTTP" or another tag to identify these attacks
   - Sampling: Choose "Random 1,000" for a quick test or "All" for comprehensive testing

3. **Start the Test**:
   - Click "Start New Test"
   - The system will process the dataset, extract payloads from the "payload" column, and test them against your active WAF rules

4. **View Results**:
   - After the test completes, you'll see detailed results in the "Completed Test Runs" section
   - The results will show which payloads were blocked and which passed
   - You can use this information to improve your WAF rules

### Command Line Testing with CSIC Dataset

You can also test the CSIC dataset using curl:

```bash
curl -X POST http://localhost:8080/api/tests/start-dataset-test \
  -F "file=@output_http_csic_2010_weka_with_duplications_RAW-RFC2616_escd_v02_full.csv" \
  -F "attackTypeTag=HTTP" \
  -F "samplingSize=Random 1000"
```

## Troubleshooting

If you encounter issues with your dataset file:

1. **No headers found**: Ensure your CSV file has a header row with column names.
2. **Payload column not found**: Ensure one of your columns is named "payload" (case-insensitive).
3. **No payloads extracted**: Ensure the payload column contains non-empty values.
4. **Parsing errors**: Check for formatting issues like unbalanced quotes or special characters.
5. **File too large**: The maximum file size is 100MB. For larger files, consider splitting them.

You can open your file in a text editor to check its format, or use a tool like Excel to export it as a standard CSV.

## Using Auto-Detection

The "Auto-detect" format option is recommended for most cases. It will:
1. Analyze the file content to determine its format
2. Look for common patterns like commas, quotes, and headers
3. Automatically use the appropriate parsing logic

If auto-detection fails, you can explicitly select the format.

## Tips for Effective Testing

1. **Start with small samples**: Use "Random 100" to quickly test a subset of payloads
2. **Tag your tests**: Use the attackTypeTag parameter to categorize different types of tests
3. **Review passed payloads**: Focus on payloads that passed through the WAF to identify rule gaps
4. **Iterate on rules**: Update your WAF rules based on test results and run tests again
5. **Use multiple datasets**: Test with different datasets to cover various attack vectors

## Last Updated

2025-07-25