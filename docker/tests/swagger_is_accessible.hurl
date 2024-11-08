# Get the swagger page
GET http://localhost:8090/swagger-ui.html
[Options]
location: true             # follow redirection for this request
max-redirs: 10             # maximum number of redirections

# and expect it to exist
HTTP 200
