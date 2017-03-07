# Java SDK for Satori platform

Use the Java SDK for the Satori platform to create server-based or mobile Android
 applications that use the RTM to publish and subscribe.

# Installing the Java SDK

## Maven

To install the Java SDK from the private Maven repository, add the following lines to `build.gradle`:

    dependencies {
        compile group: 'com.satori', name: 'satori-sdk-java', version:'1.0.0'
    }

## Download from Releases

If you want to download the JAR file manually, you can download it
[here](https://github.com/satori-com/satori-sdk-java/releases).

Add the location of the JAR file to your `CLASSPATH` or add it to `build.gradle`:

    dependencies {
        compile fileTree(dir: 'libs', include: ['*.jar'])
    }

# Documentation

You can view the latest Java SDK documentation [here](https://www.satori.com/docs).

## Logging

The Java SDK uses slf4j library for logging. We have not provided any back-end implementation.
Choose an implementation that applies to your project.

For more information, see the slf4j documentation.

## Serialization

By default, Java SDK uses the `google-gson` library for JSON serialization. You can specify your own serialization
module in `ClientBuilder` to use a different JSON library.

### Using FasterXML Jackson JSON Library Example

Use [JacksonTest.java](./src/test/java/com/satori/rtm/real/JacksonTest.java) for an example
using the Jackson JSON library.

## Running Tests

Tests require an active RTM to be available. The tests require `credentials.json` to be
populated with the RTM properties.

The `credentials.json` file must include the following key-value pairs:

```
{
  "endpoint": "ws://<SATORI_HOST>/",
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
