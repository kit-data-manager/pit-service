#!/usr/bin/env bash

echo "create pid"

response=$(curl --request POST \
  --url http://localhost:8090/api/v1/pit/pid/ \
  --header 'Content-Type: application/json' \
  --include \
  --data '{
    "entries": {
        "21.T11148/076759916209e5d62bd5": [
            {
                "key": "21.T11148/076759916209e5d62bd5",
                "name": "kernelInformationProfile",
                "value": "21.T11148/301c6f04763a16f0f72a"
            }
        ],
        "21.T11148/397d831aa3a9d18eb52c": [
            {
                "key": "21.T11148/397d831aa3a9d18eb52c",
                "name": "dateModified",
                "value": "2022-01-18T00:00:00+00:00"
            }
        ],
        "21.T11148/8074aed799118ac263ad": [
            {
                "key": "21.T11148/8074aed799118ac263ad",
                "name": "digitalObjectPolicy",
                "value": "21.T11148/37d0f4689c6ea3301787"
            }
        ],
        "21.T11148/92e200311a56800b3e47": [
            {
                "key": "21.T11148/92e200311a56800b3e47",
                "name": "etag",
                "value": "{ \"md5sum\": \"md5 d5cbefb19bd6e44bd266f4d2fb3a76b2\" }"
            }
        ],
        "21.T11148/aafd5fb4c7222e2d950a": [
            {
                "key": "21.T11148/aafd5fb4c7222e2d950a",
                "name": "dateCreated",
                "value": "2022-01-18T00:00:00+00:00"
            }
        ],
        "21.T11148/b8457812905b83046284": [
            {
                "key": "21.T11148/b8457812905b83046284",
                "name": "digitalObjectLocation",
                "value": "https://zenodo.org/record/5872645"
            }
        ],
        "21.T11148/c692273deb2772da307f": [
            {
                "key": "21.T11148/c692273deb2772da307f",
                "name": "version",
                "value": "1.0.0"
            }
        ],
        "21.T11148/c83481d4bf467110e7c9": [
            {
                "key": "21.T11148/c83481d4bf467110e7c9",
                "name": "digitalObjectType",
                "value": "21.T11148/Publication"
            }
        ]
    }
}')


wasSuccessful=$(
    echo "$response" \
    | grep 201 \
    | wc -l
)
if [ "$wasSuccessful" -eq 0 ]
then
    echo "FAILED: Expected 201 when creating a PID. Response:"
    echo "$response"
    exit 1  # failure
fi

pid=$(
    echo "$response" \
    | grep -o '"pid":"[^"]*' \
    | cut -d'"' -f4
)
echo got PID "$pid"


echo "update the pid which was created"

response=$(curl --request PUT \
  --url "http://localhost:8090/api/v1/pit/pid/$pid" \
  --header 'Content-Type: application/json' \
  --data '{
	"entries": {
		"21.T11148/076759916209e5d62bd5": [
			{
				"key": "21.T11148/076759916209e5d62bd5",
				"name": "kernelInformationProfile",
				"value": "21.T11148/301c6f04763a16f0f72a"
			}
		],
		"21.T11148/397d831aa3a9d18eb52c": [
			{
				"key": "21.T11148/397d831aa3a9d18eb52c",
				"name": "dateModified",
				"value": "2023-05-17T00:00:00+00:00"
			}
		],
		"21.T11148/8074aed799118ac263ad": [
			{
				"key": "21.T11148/8074aed799118ac263ad",
				"name": "digitalObjectPolicy",
				"value": "21.T11148/37d0f4689c6ea3301787"
			}
		],
		"21.T11148/92e200311a56800b3e47": [
			{
				"key": "21.T11148/92e200311a56800b3e47",
				"name": "etag",
				"value": "{ \"md5sum\": \"md5 d5cbefb19bd6e44bd266f4d2fb3a76b2\" }"
			}
		],
		"21.T11148/aafd5fb4c7222e2d950a": [
			{
				"key": "21.T11148/aafd5fb4c7222e2d950a",
				"name": "dateCreated",
				"value": "2022-01-18T00:00:00+00:00"
			}
		],
		"21.T11148/b8457812905b83046284": [
			{
				"key": "21.T11148/b8457812905b83046284",
				"name": "digitalObjectLocation",
				"value": "https://zenodo.org/record/5872645"
			}
		],
		"21.T11148/c692273deb2772da307f": [
			{
				"key": "21.T11148/c692273deb2772da307f",
				"name": "version",
				"value": "1.0.5"
			}
		],
		"21.T11148/c83481d4bf467110e7c9": [
			{
				"key": "21.T11148/c83481d4bf467110e7c9",
				"name": "digitalObjectType",
				"value": "21.T11148/Publication"
			}
		]
	}
}')

wasSuccessful=$(
    echo "$response" \
    | grep 200 \
    | wc -l
)

if [ "$wasSuccessful" -eq 0 ]
then
    echo "FAILED: Expected 200 when updating a PID. Response:"
    echo "$response"
    exit 1  # failure
fi


exit 0  # success
