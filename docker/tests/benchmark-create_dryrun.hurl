# create/register a pid record
POST http://localhost:8090/api/v1/pit/pid/
Content-Type: application/vnd.datamanager.pid.simple+json
Accept: application/vnd.datamanager.pid.simple+json
[QueryStringParams]
dryrun: true
{
    "record": [
        {
            "key": "21.T11148/076759916209e5d62bd5",
            "value": "21.T11148/b9b76f887845e32d29f7"
        },
        {
            "key": "21.T11148/1c699a5d1b4ad3ba4956",
            "value": "21.T11148/ca9fd0b2414177b79ac2"
        },
        {
            "key": "21.T11148/a753134738da82809fc1",
            "value": "21.T11148/a753134738da82809fc1"
        },
        {
            "key": "21.T11148/b8457812905b83046284",
            "value": "https://hdl.handle.net/21.T11148/b8457812905b83046284"
        },
        {
            "key": "21.T11148/1a73af9e7ae00182733b",
            "value": "https://orcid.org/0000-0001-6575-1022"
        },
        {
            "key": "21.T11148/aafd5fb4c7222e2d950a",
            "value": "2020-10-21T00:00:00+02:00"
        },
        {
            "key": "21.T11969/a00985b98dac27bd32f8",
            "value": "Book"
        },
        {
            "key": "21.T11148/2f314c8fe5fb6a0063a8",
            "value": "{\"licenseURL\": \"https://www.gnu.org/licenses/agpl-3.0.en.html\"}"
        },
        {
            "key": "21.T11148/82e2503c49209e987740",
            "value": "{\"md5sum\": \"2289159614f3e3b06fc436423c0dc398\"}"
        },
        {
            "key": "21.T11148/7fdada5846281ef5d461",
            "value": "{\"locationPreview/Sample\": \"https://example.com/my/path/to/image.svg\"}"
        },
        {
            "key": "21.T11148/6ae999552a0d2dca14d6",
            "value": "this-is-a-string"
        },
        {
            "key": "21.T11148/f3f0cbaa39fa9966b279",
            "value": "{\"identifier\": \"this-is-a-string\"}"
        },
        {
            "key": "21.T11148/4fe7cde52629b61e3b82",
            "value": "sandboxed/some-random-pid"
        }
    ]
}

# on success, we get a 200 (no 201, because we're in dry-run mode)
HTTP 200
