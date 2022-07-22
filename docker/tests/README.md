# Docker images and container tests

There are two images:

- `Dockerfile-build-in-image` will build the image in the container. The result is still a clean image as it uses multi-stage-builds to only include what it really needs to run. This is good for a container release.
- `Dockerfile-reuse-local-build` will reuse the build on your local machine. This is good for local development and reusing CI artifacts.

The test script `docker_tests.sh` will:

- build and run a container, named `typed-pid-maker-test`
    - without parameters, it will use the `Dockerfile-reuse-local-build` definition.
    - given an arbitrary argument (we recommend "release" or "build-in-image" for readability), it will use the `Dockerfile-build-in-image` definition.
- execute all tests in the `tests` subfolder
- stop and delete the container

The idea behind these tests is to have tests from a very practical perspective (integration tests, component/service tests). The goal is to test the docker container, but also the standard configuration (application.properties) and the spring setup in general. Examples:

- test the creation of a PID: This makes sure that the request actually reaches the handler function. Such a test should exist for all endpoint with different security configurations.
- test if swagger page is reachable: This makes sure that the spring and openapi/swagger libraries work well together in the current version.
- test if openAPI definition is reachable: same as above.