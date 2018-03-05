# Java SDK for Satori RTM
![Maven Central](https://img.shields.io/maven-central/v/com.satori/satori-rtm-sdk.svg)

Use the Java SDK for the Satori RTM to create server-based or mobile Android
 applications that use the RTM to publish and subscribe.

# Installing the Java SDK

## Maven

To install the Java SDK from the Central Maven repository using Maven, add the following lines to `pom.xml`:

```
<dependency>
    <groupId>com.satori</groupId>
    <artifactId>satori-rtm-sdk</artifactId>
    <version>[1.1.9,)</version>
</dependency>
```

## Gradle

To install the Java SDK from the Central Maven repository using Gradle, add the following lines to `build.gradle`:

```
dependencies {
    compile group: 'com.satori', name: 'satori-rtm-sdk', version:'1.1.9+'
}
```

# Documentation

The various documentation is available:

* The [Java SDK page on Satori Website](https://www.satori.com/docs/client-libraries/java).
* The [JavaDoc](https://satori-com.github.io/satori-rtm-sdk-java/).
* The [RTM API](https://www.satori.com/docs/references/rtm-api) specification.

# Logging

The Java SDK uses slf4j library as an abstraction for various logging frameworks. We have not provided any back-end implementation.
Choose an implementation that applies to your project.

For more information, see the [slf4j](https://www.slf4j.org/) documentation.

# JSON Library

By default, Java SDK uses the [google-gson](https://github.com/google/gson) library for JSON serialization.

Satori Java SDK has adapter to use [Jackson2](http://wiki.fasterxml.com/JacksonHome) library. To use it use assembly with Jackson2 support.

```
dependencies {
    compile group: 'com.satori', name: 'satori-rtm-sdk-jackson2', version:'1.1.1'
}
```

You can also specify your own serialization module in `ClientBuilder` to use a own JSON library instead of gson and jackson2.

# Using HTTPS proxy

The SDK supports working through an HTTPS proxy.

The following is an example how to set a proxy server:

```Java
RtmClient client = new RtmClientBuilder("YOUR_ENDPOINT", "YOUR_APPKEY")
    .setHttpsProxy(URI.create("http://127.0.0.1:3128"))
    .build();
```

# Android integration

## ProGuard settings

Add the following lines to your ProGuard config (usually `proguard-rules.pro`) to make SDK works when build minification is enabled:

```
# if you are using assembly with gson json library
-keep class sun.misc.Unsafe { *; }
-keepattributes Signature, *Annotation*, EnclosingMethod

# if you are using assembly with jackson2 json library
-dontwarn com.fasterxml.jackson.databind.**
-keep class org.codehaus.**
-keepnames class com.fasterxml.jackson.** { *; }

# guava
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn java.lang.ClassValue
-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn sun.misc.Unsafe
-keep class com.google.j2objc.annotations.** { *; }
-keep class java.lang.ClassValue { *; }
-keep class org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement { *; }
-keep class com.google.common.util.concurrent.AbstractFuture** { <fields>; }

# slf4j
-dontwarn org.slf4j.**

# satori-rtm-sdk
-keep class com.satori.rtm.connection.StaticJsonBinder { *; }
-keep class com.satori.rtm.model.** { *; }
-keep class com.satori.rtm.auth.** { *; }
```

## "Duplicate files copied in APK META-INF/LICENCE" error when using Jackson2 assembly

If you see the following build exception:

```
FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':app:transformResourcesWithMergeJavaResForRelease'.
> com.android.build.api.transform.TransformException: com.android.builder.packaging.DuplicateFileException: Duplicate files copied in APK META-INF/LICENSE
        File1: /Users/user/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.core/jackson-annotations/2.8.8/1ed81c0e4eb2d261d1da0a3a45bd6b199fb5cf9a/jackson-annotations-2.8.8.jar
        File2: /Users/user/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.core/jackson-databind/2.8.8/bf88c7b27e95cbadce4e7c316a56c3efffda8026/jackson-databind-2.8.8.jar
        File3: /Users/user/.gradle/caches/modules-2/files-2.1/com.fasterxml.jackson.core/jackson-core/2.8.8/d478fb6de45a7c3d2cad07c8ad70c7f0a797a020/jackson-core-2.8.8.jar
```

Add below lines in your application level gradle config:
```
packagingOptions {
    exclude 'META-INF/LICENSE'
}
```

# Running Tests

Tests require an active RTM to be available. The tests require `credentials.json` to be
populated with the RTM properties.

The `credentials.json` file must include the following key-value pairs:

```
{
  "endpoint": "wss://<SATORI_HOST>/",
  "appkey": "<APP_KEY>",
  "auth_role_name": "<ROLE_NAME>",
  "auth_role_secret_key": "<ROLE_SECRET_KEY>",
  "auth_restricted_channel": "<CHANNEL_NAME>"
}
```

* `endpoint` is your customer-specific DNS name for RTM access.
* `appkey` is your application key.
* `auth_role_name` is a role name that permits to publish / subscribe to `auth_restricted_channel`. Must be not `default`.
* `auth_role_secret_key` is a secret key for `auth_role_name`.
* `auth_restricted_channel` is a channel with subscribe and publish access for `auth_role_name` role only.

You must use [DevPortal](https://developer.satori.com/) to create role and set channel permissions.

After setting up `credentials.json`, run SDK tests with the following commands:

```
./gradlew test -DRTM_CONFIG=./credentials.json
```

If the `RTM_CONFIG` option is not specified, the SDK uses `<SDK ROOT>/credentials.json`.
