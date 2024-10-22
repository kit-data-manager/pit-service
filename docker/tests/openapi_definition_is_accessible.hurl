# Get the api definition
GET http://localhost:8090/v3/api-docs
[Options]
location: true             # follow redirection for this request
max-redirs: 10             # maximum number of redirections

# expect them to exist
HTTP 200
