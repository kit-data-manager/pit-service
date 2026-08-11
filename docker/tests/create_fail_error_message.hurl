# create/register a pid record with missing required fields
POST http://localhost:8090/api/v1/pit/pid/
Content-Type: application/vnd.datamanager.pid.simple+json
Accept: application/vnd.datamanager.pid.simple+json
{
    "record": [
        { "key": "21.T11148/076759916209e5d62bd5", "value": "21.T11148/301c6f04763a16f0f72a" }
    ]
}

# this will fail because the profile validation will fail
HTTP 400
# in the response body, you'll get the record and the pid
[Asserts]
jsonpath "$.detail" exists
jsonpath "$.detail" contains "Missing mandatory types"
jsonpath "$.pid-record" exists

# Lets ask if the record exists now:
HEAD http://localhost:8090/api/v1/pit/pid/{{pid}}

# it should not:
HTTP 404
