#!/bin/bash
# Test script for uploading a large CSV dataset to the SafeGate WAF

# Configuration
API_URL="http://localhost:8080/api/tests/start-dataset-test"
CSV_FILE="output_http_csic_2010_weka_with_duplications_RAW-RFC2616_escd_v02_full.csv"
ATTACK_TYPE="HTTP"
SAMPLING_SIZE="Random 1000"

# Check if the CSV file exists
if [ ! -f "$CSV_FILE" ]; then
    echo "Error: CSV file '$CSV_FILE' not found."
    echo "Please make sure the file exists in the current directory."
    exit 1
fi

# Get file size
FILE_SIZE=$(du -h "$CSV_FILE" | cut -f1)
echo "File size: $FILE_SIZE"

# Test with curl
echo "Uploading dataset to $API_URL..."
echo "File: $CSV_FILE"
echo "Attack Type: $ATTACK_TYPE"
echo "Sampling Size: $SAMPLING_SIZE"
echo ""

# Use curl to upload the file
curl -X POST "$API_URL" \
  -F "file=@$CSV_FILE" \
  -F "datasetFormat=AUTO" \
  -F "attackTypeTag=$ATTACK_TYPE" \
  -F "samplingSize=$SAMPLING_SIZE" \
  -v

echo ""
echo "Test completed."