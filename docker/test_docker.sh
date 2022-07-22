#!/usr/bin/env bash

# docker parameters
tag=typed-pid-maker-test
container=typid-test
# meta information for this script
this=${BASH_SOURCE}
echo "this script is at $this"
docker_dir=$(dirname $this)

echo "build docker image"
sleep .2
# use "standalone" or "release" to build in the docker container
if [ $1 ]
then
    echo "  > compiling in container: "
    docker build --file $docker_dir/Dockerfile-build-in-image --tag $tag $docker_dir/..
else
    echo "  > reusing local build: "
    docker build --file $docker_dir/Dockerfile-reuse-local-build --tag $tag $docker_dir/..
fi

echo -n "run container: "
docker run -p 8090:8090 --detach --name $container $tag
# give the application and container some time:
sleep 20 # seconds

#####################################
### tests ###########################
#####################################
failure=0
echo "running tests"

for test in $(ls $docker_dir/tests/*.sh)
do
    echo ""
    echo "> running test $test:"
    bash $test
    failure=$(expr $failure + $?)
    echo "> finished $test"
done

echo ""
echo "finished tests"
#####################################

echo -n "stopping container ... "
docker container stop $container
echo -n "removing container ... "
docker container rm $container

if [ $failure -eq 0 ]
then
    echo "ALL TESTS SUCCESSFUL."
    exit 0
else
    echo "TESTS FAILED: $failure (see output for details)"
    exit 1
fi
