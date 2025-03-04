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


## Purpose

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

The benchmark mode is available to see the impact of new developments on the validation time needed.

## Benchmark results

Let's collect some results of the benchmarks:

- Mac Studio with M1 Max, 32GB RAM, Sonoma 14.7.2
  - Commit: 8fe47fc6781ed08457cedc09d0c0efbccf7359c7
  - Executed files:    1000
  - Executed requests: 1001 (6.2/s)
  - Succeeded files:   1000 (100.0%)
  - Failed files:      0 (0.0%)
  - Duration:          160903 ms