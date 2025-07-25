# SafeGate WAF API Documentation

This document provides comprehensive documentation for the SafeGate Web Application Firewall (WAF) API endpoints.

## Core API Endpoints

### Security Endpoints

#### Get Blocked Requests
- **Endpoint**: `/api/security/blocked`
- **Method**: GET
- **Description**: Retrieves all blocked requests ordered by timestamp
- **Response**: List of blocked requests with details

#### Get Blocked Requests Count
- **Endpoint**: `/api/security/blocked/count`
- **Method**: GET
- **Description**: Gets the total count of blocked requests
- **Response**: Count of blocked requests

### Logs Endpoints

#### Get All Logs
- **Endpoint**: `/api/logs`
- **Method**: GET
- **Description**: Retrieves all logs of blocked requests
- **Response**: List of log entries

#### Get Recent Logs
- **Endpoint**: `/api/logs/recent`
- **Method**: GET
- **Description**: Gets recent logs (default: last 24 hours)
- **Response**: List of recent log entries

#### Get Log Statistics
- **Endpoint**: `/api/logs/stats`
- **Method**: GET
- **Description**: Gets statistics about blocked requests
- **Response**: Statistics including total count, last 24h count, attack patterns, top attacking IPs

#### Clear Logs
- **Endpoint**: `/api/logs`
- **Method**: DELETE
- **Description**: Clears all logs
- **Response**: Confirmation message

### Rules Endpoints

#### Get All Rules
- **Endpoint**: `/api/rules`
- **Method**: GET
- **Description**: Retrieves all signature rules
- **Response**: List of signature rules

#### Get Rule by ID
- **Endpoint**: `/api/rules/{id}`
- **Method**: GET
- **Description**: Gets a rule by ID
- **Response**: Signature rule details

#### Add New Rule
- **Endpoint**: `/api/rules`
- **Method**: POST
- **Description**: Adds a new signature rule
- **Request Body**: Rule details
- **Response**: Created rule

#### Update Rule
- **Endpoint**: `/api/rules/{id}`
- **Method**: PUT
- **Description**: Updates an existing rule
- **Request Body**: Updated rule details
- **Response**: Updated rule

#### Delete Rule
- **Endpoint**: `/api/rules/{id}`
- **Method**: DELETE
- **Description**: Deletes a rule
- **Response**: Confirmation message

#### Toggle Rule Status
- **Endpoint**: `/api/rules/{id}/toggle`
- **Method**: POST
- **Description**: Toggles a rule's enabled status
- **Response**: Updated rule status

## Performance Testing API

### Basic Testing Endpoints

#### Start Test Run
- **Endpoint**: `/api/tests/start`
- **Method**: POST
- **Description**: Starts a WAF test run
- **Response**: Test run details

#### Stop Test Run
- **Endpoint**: `/api/tests/stop`
- **Method**: POST
- **Description**: Stops the current test run and saves results
- **Response**: Test run results

#### Get Test Status
- **Endpoint**: `/api/tests/status`
- **Method**: GET
- **Description**: Gets the status of the current test run
- **Response**: Test run status

#### Get Test Results
- **Endpoint**: `/api/tests/results`
- **Method**: GET
- **Description**: Gets results of all test runs
- **Response**: List of test run results

## Dataset Testing API

The SafeGate WAF provides an API for testing datasets of attack payloads against the WAF.

### Start Dataset Test

- **Endpoint**: `/api/tests/start-dataset-test`
- **Method**: POST
- **Description**: Uploads a dataset file and tests it against the WAF
- **Content-Type**: multipart/form-data

#### Parameters

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| file | Yes | - | The dataset file to test |
| datasetFormat | No | AUTO | The format of the dataset (AUTO, TXT, CSV, JSON, XML, TSV) |
| attackTypeTag | No | - | Optional tag for the attack type (e.g., SQLi, XSS) |
| samplingSize | No | All | Number of attacks to test (All, Random 100, Random 1,000, etc.) |

#### Response

The API returns a JSON object with the following fields:

```json
{
  "testRun": {
    "id": 123,
    "startTime": "2025-07-25T17:56:00",
    "endTime": "2025-07-25T17:57:00",
    "totalPassed": 30,
    "totalBlocked": 70,
    "datasetFileName": "attacks.csv",
    "datasetFormat": "CSV",
    "attackTypeTag": "SQLi",
    "samplingSize": "Random 100",
    "totalMaliciousRequests": 100,
    "totalMaliciousBlocked": 70,
    "blockCounts": [
      {
        "ruleName": "SQL Injection Rule",
        "count": 65
      },
      {
        "ruleName": "Generic Attack Signatures",
        "count": 5
      }
    ],
    "passedPayloads": [
      {
        "payload": "' OR 1=1 --"
      },
      {
        "payload": "1' OR '1'='1"
      },
      {
        "payload": "admin' --"
      }
    ]
  },
  "detectedFormat": "CSV",
  "message": "Test completed successfully using CSV format"
}
```

### Examples

#### Using Auto-Detection (Recommended)

```bash
curl -X POST http://localhost:8080/api/tests/start-dataset-test \
  -F "file=@/path/to/your/dataset.csv" \
  -F "attackTypeTag=SQLi" \
  -F "samplingSize=Random 100"
```

This command will:
1. Upload the file `dataset.csv`
2. Automatically detect the file format
3. Tag the attacks as "SQLi"
4. Test a random sample of 100 attacks

#### Specifying a Format Explicitly

```bash
curl -X POST http://localhost:8080/api/tests/start-dataset-test \
  -F "file=@/path/to/your/dataset.csv" \
  -F "datasetFormat=CSV" \
  -F "attackTypeTag=SQLi" \
  -F "samplingSize=Random 100"
```

This command explicitly specifies the format as CSV.

### Error Handling

If there's an error processing the dataset, the API will return a JSON object with error details:

```json
{
  "error": "Error message",
  "errorType": "IllegalArgumentException",
  "suggestion": "Try specifying the format explicitly or check if the file content is valid"
}
```

Common errors include:
- File format detection failures
- Parsing errors for specific formats
- Empty files or missing payload data

### Tips for Using the API

1. **Always use auto-detection** when possible by omitting the datasetFormat parameter or setting it to "AUTO"
2. If auto-detection fails, try specifying the format explicitly
3. For large datasets, use sampling to test a subset of the attacks
4. Check the response for the detected format and any error messages
5. Ensure your file has the correct structure for the format you're using

For more detailed information about supported file formats and usage examples, please refer to the [Dataset Testing Guide](DATASET_GUIDE.md).