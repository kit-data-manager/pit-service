# create/register a pid record
POST http://localhost:8090/api/v1/pit/pid/
Content-Type: application/vnd.datamanager.pid.simple+json
Accept: application/vnd.datamanager.pid.simple+json
{
    "record": [
        { "key": "21.T11148/076759916209e5d62bd5", "value": "21.T11148/301c6f04763a16f0f72a" },
        { "key": "21.T11148/397d831aa3a9d18eb52c", "value": "2021-12-21T17:36:09.541+00:00" },
        { "key": "21.T11148/8074aed799118ac263ad", "value": "21.T11148/37d0f4689c6ea3301787" },
        { "key": "21.T11148/92e200311a56800b3e47", "value": "{ \"sha256sum\": \"sha256 c50624fd5ddd2b9652b72e2d2eabcb31a54b777718ab6fb7e44b582c20239a7c\" }" },
        { "key": "21.T11148/aafd5fb4c7222e2d950a", "value": "2021-12-21T17:36:09.541+00:00" },
        { "key": "21.T11148/b8457812905b83046284", "value": "https://test.repo/file001" },
        { "key": "21.T11148/c692273deb2772da307f", "value": "1.0.0" },
        { "key": "21.T11148/c83481d4bf467110e7c9", "value": "21.T11148/ManuscriptPage" }
    ]
}

# on success, we get a 201
HTTP 201
# in the response body, you'll get the record and the pid
[Captures]
pid: jsonpath "$.pid"

# The PID should exist now, let's ask if this is true:
HEAD http://localhost:8090/api/v1/pit/pid/{{pid}}

# 200 means yes
HTTP 200

# if we resolve the pid...
GET http://localhost:8090/api/v1/pit/pid/{{pid}}

# we get the PID and its record
HTTP 200
[Asserts]
jsonpath "$.pid" == {{pid}}
