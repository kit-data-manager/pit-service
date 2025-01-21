# Docker images and container tests

There are two images:

- `Dockerfile-build-in-image`
   - Will build the image in the container. The result is still a clean image as it uses multi-stage-builds to only include what it really needs to run.
   - Best for making a container release.
   - Used by CI to create an image for publication.
- `Dockerfile-reuse-local-build`
  - Will reuse the build on your local machine.
  - Good for local development and reusing CI artifacts.
  - Used in `test_docker.sh` and build/test CI.


## Run all tests

For the recommended usage instructions, see the main readme file. The test script `docker_tests.sh` will:

- build and run a container, named `typed-pid-maker-test`
- if the script gets `benchmark` as its first parameter:
  - execute benchmark tests repeatedly
- else (default):
  - execute all tests in the `tests` subfolder
- stop and delete the container

The idea behind the tests is to have basic tests from a very practical perspective (integration tests, component/service tests). The goal is to test the docker container, but also the standard configuration (application.properties) and the spring setup in general. Examples:

- test the creation of a PID: This makes sure that the request actually reaches the handler function. Such a test should exist for all endpoints with different security configurations.
- test if swagger page is reachable: This makes sure that the spring and openapi/swagger libraries work well together in the current version.
- test if openAPI definition is reachable: same as above.

The goal is **not** to achieve full test coverage here. For this, we have unit tests and integration tests in `src/test`.

The benchmark mode is available to see the impact of new developments on the validation.