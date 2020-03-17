# Notification Service

![Build Status](https://img.shields.io/travis/kit-data-manager/notification-service.svg)
![Code Coverage](https://img.shields.io/coveralls/github/kit-data-manager/notification-service.svg)
![License](https://img.shields.io/github/license/kit-data-manager/notification-service.svg)

This project provides a general purpose notification service for informing users on system events to distribute general information to them. The service is based
on messages sent via RabbitMQ by any service capable of emitting such notifications. The messages are then stored in a database and can be queried by the user 
by utilizing the RESTful interface or they are automatically distributed by so called subscriptions. One currently supported subscription type is via email. Therefor,
the user has to subscribe with his/her email address and notifications are delivered frequently to the configured mail account.

## How to build

In order to build the Notification Service you'll need:

* Java SE Development Kit 8 or higher

After obtaining the sources change to the folder where the sources are located perform the following steps:

```
user@localhost:/home/user/notification-service$ ./gradlew -Pclean-release build
> Configure project :
Using release profile for building notification-service
<-------------> 0% EXECUTING [0s]
[...]
user@localhost:/home/user/notification-service$
```

The Gradle wrapper will now take care of downloading the configured version of Gradle, checking out all required libraries, build these
libraries and finally build the notification-service microservice itself. As a result, a fat jar containing the entire service is created at 'build/jars/notification-service.jar'.

## How to start

### Prerequisites

* PostgreSQL 9.1 or higher

### Setup
Before you are able to start the microservice, you have to modify the file 'application.properties' according to your local setup. 
Therefor, copy the file 'conf/application.properties' to your project folder and customize it. For the Collection API you just have to adapt the properties of 
spring.datasource and you may change the server.port property. All other properties can be ignored for the time being.

As soon as you finished modifying 'application.properties', you may start the notification service by executing the following command inside the project folder, 
e.g. where the service has been built before:

```
user@localhost:/home/user/notification-service$ ./build/libs/notification-service.jar

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

http://localhost:8070/swagger-ui.html

in order to see available RESTful endpoints and their documentation. You may have to adapt the port according to your local settings.
Furthermore, you can use this Web interface to test single API calls in order to get familiar with the service. 

## License

The KIT Data Manager is licensed under the Apache License, Version 2.0.
