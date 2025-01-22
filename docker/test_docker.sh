#!/usr/bin/env bash

# hurl is required
if ! command -v hurl &>/dev/null; then
  echo "> hurl is required but it's not installed. Aborting." >&2
  exit 1
fi

if [ "$1" == "benchmark" ]; then
  benchmark_mode=0 # 0 (true) for benchmarks
  echo "> benchmark mode"
else
  benchmark_mode=1 # 1 (false) for tests
  echo "> test mode"
fi

# docker parameters
tag=typed-pid-maker-test
container=typid-test

# meta information for this script
this=${BASH_SOURCE[0]}
echo "> this script is at $this"
docker_dir=$(dirname "$this")

echo "> trigger local build"
"$docker_dir"/../gradlew build -x test || exit 1

echo "> build docker image, reusing local build"
sleep .2
docker build --file "$docker_dir/Dockerfile-reuse-local-build" \
  --tag "$tag" "$docker_dir/.." || exit 1

echo -n "> run container: "
docker run \
  --env pit.typeregistry.cache.maxEntries=0 \
  -p 8090:8090 \
  --detach \
  --name $container $tag

echo "> Making sure the service is ready to accept requests"
# lets do something which will definitely not influence caches:
hurl --retry=20 --retry-interval=5000 \
  --test "$docker_dir"/tests/resolve-nonexisting.hurl ||
  exit 1
sleep 1
echo "> Service is ready. Starting with actual tests."

#####################################
### run tests / benchmarks ################
#####################################
echo "> benchmark mode: $benchmark_mode"
if [ "$benchmark_mode" -eq 0 ]; then
  echo "> running benchmark"
  hurl --retry=20 --retry-interval=5000 \
    --test --jobs 1 --repeat 1000 \
    "$docker_dir"/tests/benchmark-create_dryrun.hurl
  failure=$?
else
  echo "> running tests"
  hurl --retry=20 --retry-interval=5000 \
    --test "$docker_dir"/tests/*.hurl
  failure=$?
fi
#####################################

echo -n "> stopping container ... "
docker container stop $container
echo -n "> removing container ... "
docker container rm $container

if [ $failure -eq 0 ]; then
  echo "> ALL TESTS SUCCESSFUL."
  exit 0
else
  echo "> TESTS FAILED (see output for details)"
  exit 1
fi
