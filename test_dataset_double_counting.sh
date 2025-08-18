#!/bin/bash

# Test script to verify the fix for double-counting in dataset test results
# This script uploads a small test dataset and checks the results

echo "Starting test for dataset double-counting fix..."

# Define the API endpoint
API_URL="http://localhost:8080/api/test/dataset"

# Create a small test file with 3 requests
echo "Creating test dataset with 3 requests..."
cat > test_dataset.csv << EOF
GET,/test?param1=value1,normal
POST,/test?param2=value2,attack
GET,/test?param3=value3,normal
EOF

# Upload the test file
echo "Uploading test dataset..."
RESPONSE=$(curl -s -X POST \
  -F "file=@test_dataset.csv" \
  -F "datasetFormat=CSV" \
  -F "attackTypeTag=test" \
  -F "samplingSize=ALL" \
  ${API_URL})

echo "Response from server:"
echo $RESPONSE

# Extract the test run ID from the response
TEST_RUN_ID=$(echo $RESPONSE | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -z "$TEST_RUN_ID" ]; then
  echo "Failed to get test run ID from response"
  exit 1
fi

echo "Test run ID: $TEST_RUN_ID"

# Get the test run details
echo "Getting test run details..."
TEST_RUN_DETAILS=$(curl -s "http://localhost:8080/api/test/runs/$TEST_RUN_ID")

echo "Test run details:"
echo $TEST_RUN_DETAILS

# Extract the total count of requests
TOTAL_REQUESTS=$(echo $TEST_RUN_DETAILS | grep -o '"totalPassed":[0-9]*' | cut -d':' -f2)
TOTAL_BLOCKED=$(echo $TEST_RUN_DETAILS | grep -o '"totalBlocked":[0-9]*' | cut -d':' -f2)
TOTAL_COUNT=$((TOTAL_REQUESTS + TOTAL_BLOCKED))

echo "Total requests processed: $TOTAL_COUNT (Passed: $TOTAL_REQUESTS, Blocked: $TOTAL_BLOCKED)"

# Check if the total count matches the expected count (3)
if [ "$TOTAL_COUNT" -eq 3 ]; then
  echo "TEST PASSED: The total count ($TOTAL_COUNT) matches the expected count (3)"
else
  echo "TEST FAILED: The total count ($TOTAL_COUNT) does not match the expected count (3)"
  if [ "$TOTAL_COUNT" -eq 6 ]; then
    echo "Double-counting issue is still present!"
  fi
fi

# Clean up
rm test_dataset.csv
echo "Test completed."