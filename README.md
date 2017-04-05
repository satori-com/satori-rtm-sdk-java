# Java SDK for Satori platform
![Maven Central](https://img.shields.io/maven-central/v/com.satori/satori-sdk-java.svg)

Use the Java SDK for the Satori platform to create server-based or mobile Android
 applications that use the RTM to publish and subscribe.

# Installing the Java SDK

## Maven

To install the Java SDK from the Central Maven repository using Maven, add the following lines to `pom.xml`:

```
<dependency>
    <groupId>com.satori</groupId>
    <artifactId>satori-sdk-java</artifactId>
    <version>1.0.2</version>
</dependency>
```

## Gradle

To install the Java SDK from the Central Maven repository using Gradle, add the following lines to `build.gradle`:

```
dependencies {
    compile group: 'com.satori', name: 'satori-sdk-java', version:'1.0.2'
}
```

# Documentation

You can view the latest Java SDK documentation [here](https://www.satori.com/docs/client-libraries/java).

# Logging

The Java SDK uses slf4j library as an abstraction for various logging frameworks. We have not provided any back-end implementation.
Choose an implementation that applies to your project.

For more information, see the [slf4j](https://www.slf4j.org/) documentation.

# JSON Library

Satori Java SDK could work with different JSON libraries. By default, Java SDK uses the [google-gson](https://github.com/google/gson) library for JSON serialization.

You can specify your own serialization module in `ClientBuilder` to use a different JSON library.

Satori Java SDK has adapter to use [Jackson2](http://wiki.fasterxml.com/JacksonHome) library. To use it use assembly with Jackson2 support.

```
dependencies {
    compile group: 'com.satori', name: 'satori-sdk-java-jackson2', version:'1.0.2'
}
```

# Running Tests

Tests require an active RTM to be available. The tests require `credentials.json` to be
populated with the RTM properties.

The `credentials.json` file must include the following key-value pairs:

```
{
  "endpoint": "wss://<SATORI_HOST>/",
  "appkey": "<APP KEY>",
  "superuser_role_secret": "<ROLE SECRET KEY>"
}
```

* `endpoint` is your customer-specific DNS name for RTM access.
* `appkey` is your application key.
* `superuser_role_secret` is a role secret key for a role named `superuser`.

After setting up `credentials.json`, run SDK tests with the following commands:

```
./gradlew test -DRTM_CONFIG=./credentials.json
```

If the `RTM_CONFIG` option is not specified, the SDK uses `<SDK ROOT>/credentials.json`.
