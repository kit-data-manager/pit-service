# Typed PID Maker

![License](https://img.shields.io/github/license/kit-data-manager/pit-service.svg) [![Java CI with Gradle](https://github.com/kit-data-manager/pit-service/actions/workflows/gradle.yml/badge.svg)](https://github.com/kit-data-manager/pit-service/actions/workflows/gradle.yml)

The Typed PID Maker enables the creation, maintenance, and validation of PIDs. It ensures the PID contains typed, machine-actionable information using validation. This is especially helpful in the context of FAIR Digital Objects (FAIR DOs / FDOs). To make this work, our validation strategy requires a reference to a registered Kernel Information Profile within the PID record, as defined by the [recommendations of the Research Data Alliance (RDA)](https://doi.org/10.15497/rda00031). [In the RDA context, this kind of service is called a "PIT service"](https://doi.org/10.15497/FDAA09D5-5ED0-403D-B97A-2675E1EBE786). We use Handle PIDs, which can be created using a Handle Prefix (not included). For testing or other local purposes, we support sandboxed PIDs, which require no external service.

**See also: [Documentation](https://kit-data-manager.github.io/webpage/typed-pid-maker/index.html) | [Configuration details](https://github.com/kit-data-manager/pit-service/blob/master/config/application.properties)**

## Features

- ✅ Create PIDs containing typed key-value-pairs for easy, fast, and automated decision-making.
- ✅ Maintain the information within these PIDs.
- ✅ Validate PIDs.
- ✅ Resolve PIDs.
- ✅ Store the created PIDs in your database.
  - ✅ Pagination support
  - ✅ Tabulator.js support
- ✅ Authentication via [JWT](https://jwt.io/introduction) or [KeyCloak](https://www.keycloak.org/)

## How to build

In order to build the Typed PID Maker, you'll need:

* Java SE Development Kit 11 (or openjdk 11) or higher

After obtaining the sources change to the folder where the sources are located perform the following steps:

```
user@localhost:/home/user/typed-pid-maker$ ./gradlew build
> Configure project :
Using release profile for building notification-service
<-------------> 42% EXECUTING [0s]
[...]
user@localhost:/home/user/typed-pid-maker$
```

The Gradle wrapper will now take care of downloading the configured version of Gradle and finally build the Typed PID Maker microservice. As a result, a jar file containing the entire service is created at `build/libs/TypedPIDMaker-$(version).jar`.

## How to start

For development purposes, the easiest way to run the service with your configuration file is:

```bash
./gradlew run --args="--spring.config.location=config/application.properties"
```

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

Details on the version being used and other build information can be found on http://localhost:8090/actuator/info.

## License

The KIT Data Manager is licensed under the Apache License, Version 2.0.
