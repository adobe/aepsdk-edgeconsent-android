# Getting started with the test app

## Data Collection mobile property prerequisites

The test app needs to be configured with the following extensions before it can be used:

* [Mobile Core](https://github.com/adobe/aepsdk-core-android) (installed by default)
* [Assurance](https://github.com/adobe/aepsdk-assurance-android)
* Consent

## Run test application

1. In the test app, set your `ENVIRONMENT_FILE_ID` in `ConsentTestApplication.java`.
2. Select the `app` runnable with the desired emulator and run the program.

## Validation with Assurance

Configure a new Assurance session by setting the Base URL to `testapp://main` and launch Assurance in the test app by running the following command in your terminal:

```bash
$ adb shell am start -W -a  android.intent.action.VIEW -d "testapp://main?adb_validation_sessionid=ADD_YOUR_SESSION_ID_HERE" com.adobe.marketing.mobile.consenttestapp
```

>> **Note**
>> Replace ADD_YOUR_SESSION_ID_HERE with your Assurance session identifier.

Once the connection is established and the events list starts getting populated, you can filter the Consent extension events by typing `Consent` in the `Search Events` search box.
