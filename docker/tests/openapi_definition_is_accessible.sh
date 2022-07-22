#!/usr/bin/env bash

response=$(curl --request GET \
  --url http://localhost:8090/v3/api-docs \
  --location \
  --include)

wasSuccessful=$(
    echo "$response" \
    | grep 200 \
    | wc -l
)

if [ "$wasSuccessful" -eq 0 ]
then
    echo "FAILED: Expected HTTP 200 when accessing http://localhost:8090/v3/api-docs. Response:"
    echo "$response"
    exit 1  # failure
else
    echo "SUCCESS: OpenAPI definition is accessible"
    exit 0  # success
fi
