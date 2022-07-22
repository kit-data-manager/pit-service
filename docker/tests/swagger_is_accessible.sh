#!/usr/bin/env bash

response=$(curl --request GET \
  --url http://localhost:8090/swagger-ui.html \
  --location \
  --include)

wasSuccessful=$(
    echo "$response" \
    | grep 200 \
    | wc -l
)

if [ "$wasSuccessful" -eq 0 ]
then
    echo "FAILED: Expected HTTP 200 when accessing http://localhost:8090/swagger-ui.html. Response:"
    echo "$response"
    exit 1  # failure
else
    echo "SUCCESS: Swagger is accessible"
    exit 0  # success
fi
