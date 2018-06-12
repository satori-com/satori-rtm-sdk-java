v1.2 (2018-04-09)
--------------------
* Add support for return messages for RTM requests.
* Drop Java 1.6 Support (Supports 1.7+ only)

v1.1.1 (2017-10-11)
-------------------
* Add method to PduException to get common RTM reply in case of error
* Add optional position field to write request
* Leave only `onError` callback to handle transport exceptions from WebSocket library

v1.1.0 (2017-08-11)
-------------------
* Add proxy support
* Update javadocs

v1.0.3 (2017-05-09)
-------------------
* Upgrade 3rd party deps to the latest versions
* Rename package satori-sdk-java -> satori-rtm-sdk
* Remove superuser from credentials.json file

v1.0.2 (2017-04-02)
-------------------
* Fix issue in subscription mode flags
* Fix eclispe build
* Fix compilation error when SDK is used from sources
* Fix typos in README
* Don't show full stacktrace in log when negative PDU is received

v1.0.1 (2017-03-13)
-------------------
* Add assembly for Jackson2 library
* Set SO_TIMEOUT for WebSocket

v1.0.0 (2017-03-07)
-------------------
* Initial release
