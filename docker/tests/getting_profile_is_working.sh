#!/usr/bin/env bash

response=$(curl --request GET \
  --url http://localhost:8090/api/v1/pit/profile/21.T11148/b9b76f887845e32d29f7 \
  --location \
  --include)

wasSuccessful=$(
    echo "$response" \
    | grep 200 \
    | wc -l
)

if [ "$wasSuccessful" -eq 0 ]
then
    echo "FAILED: Expected HTTP 200 when retrieving Profile with PID 21.T11148/b9b76f887845e32d29f7. Response:"
    echo "$response"
    exit 1  # failure
else
    echo "SUCCESS: Retrieved Profile"
    exit 0  # success
fi
