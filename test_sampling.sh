#!/bin/bash

# Test script to check the sampling functionality
# This script will run a test with "Random 1000" sampling size

echo "Running test with 'Random 1000' sampling size..."

# Assuming there's a CSV dataset file available
DATASET_FILE="output_http_csic_2010_weka_with_duplications_RAW-RFC2616_escd_v02_full.csv"

# Check if the dataset file exists
if [ ! -f "$DATASET_FILE" ]; then
    echo "Dataset file not found: $DATASET_FILE"
    exit 1
fi

# Use curl to send a request to the test endpoint
# Adjust the URL and parameters as needed based on your application's API
curl -X POST \
  -F "file=@$DATASET_FILE" \
  -F "datasetFormat=CSV" \
  -F "attackTypeTag=test" \
  -F "samplingSize=Random 1000" \
  http://localhost:8080/api/test/start-dataset-test

echo "Test completed. Check the logs for details."