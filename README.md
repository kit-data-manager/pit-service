# Typed PID Maker

![License](https://img.shields.io/github/license/kit-data-manager/pit-service.svg) [![Java CI with Gradle](https://github.com/kit-data-manager/pit-service/actions/workflows/gradle.yml/badge.svg)](https://github.com/kit-data-manager/pit-service/actions/workflows/gradle.yml)

This project provides a service for dealing with PID Information Types and Kernel Information Profiles according to the recommendations of the Research Data Alliance. In the RDA context, this kind of service is called a "PIT service".
Depending on the configuration it allows minting PIDs from different services, to lookup PIDs with a specific record entry and to validate PIDs according to profile information
contained in the PID record.

## How to build

In order to build the Typed PID Maker, you'll need:

* Java SE Development Kit 11 (or openjdk 11) or higher

After obtaining the sources change to the folder where the sources are located perform the following steps:

```
user@localhost:/home/user/typed-pid-maker$ ./gradlew build
> Configure project :
Using release profile for building notification-service
<-------------> 0% EXECUTING [0s]
[...]
user@localhost:/home/user/typed-pid-maker$
```

The Gradle wrapper will now take care of downloading the configured version of Gradle and finally build the Typed PID Maker microservice. As a result, a jar file containing the entire service is created at `build/libs/TypedPIDMaker-$(version).jar`.

## How to start

Before you are able to start the microservice, you have to modify the file 'application.properties' according to your local setup. 
Therefor, copy the file `conf/application.properties` to your project folder and customize it. For the Collection API you just have to adapt the properties of 
`spring.datasource` and you may change the `server.port` property. All other properties can be ignored for the time being.

As soon as you finished modifying 'application.properties', you may start the service by executing the following command inside the project folder, 
e.g. where the service has been built before:

```
user@localhost:/home/user/typed-pid-maker$ ./build/libs/TypedPIDMaker-$(version).jar

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.0.5.RELEASE)
[...]
1970-01-01 00:00:00.000  INFO 56918 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8070 (http) with context path ''

```

As soon as the microservice is started, you can browse to 

http://localhost:8090/swagger-ui.html

in order to see available RESTful endpoints and their documentation. You may have to adapt the port according to your local settings.
Furthermore, you can use this Web interface to test single API calls in order to get familiar with the service. 

## License

The KIT Data Manager is licensed under the Apache License, Version 2.0.
