# Adobe Experience Platform Consent Collection Mobile Extension
[![Maven Central](https://img.shields.io/maven-metadata/v.svg?label=edgeconsent&logo=android&logoColor=white&metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fcom%2Fadobe%2Fmarketing%2Fmobile%2Fedgeconsent%2Fmaven-metadata.xml)](https://mvnrepository.com/artifact/com.adobe.marketing.mobile/edgeconsent)

## About this project

The Adobe Experience Platform Edge Consent Collection mobile extension enables consent preferences collection from your mobile app when using the [Adobe Experience Platform Mobile SDK](https://developer.adobe.com/client-sdks) and the Edge Network extension.

### Installation

Integrate the Consent mobile extension into your app by following the [getting started guide](Documentation/getting-started.md).

### Development

#### Open the project

To open and run the project, open the `code/settings.gradle` file in Android Studio.

#### Run the test application

To configure and run the test app for this project, follow the [getting started guide for the test app](Documentation/getting-started-test-app.md).

#### Code format

This project uses the code formatting tools [Spotless](https://github.com/diffplug/spotless/tree/main/plugin-gradle) with [Prettier](https://prettier.io/). Formatting is applied when the project is built from Gradle and is checked when changes are submitted to the CI build system.

Prettier requires [Node version](https://nodejs.org/en/download/releases/) 10+

To enable the Git pre-commit hook to apply code formatting on each commit, run the following to update the project's git config `core.hooksPath`:
```
make init
```

## Related Projects

| Project                                                      | Description                                                  |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| [Core extensions](https://github.com/adobe/aepsdk-core-android)                      | The Mobile Core represents the foundation of the Adobe Experience Platform Mobile SDK.               |
| [Edge Network](https://github.com/adobe/aepsdk-edge-android)                      | This extension allows you to send data to Edge Network from a mobile application.               |
| [Identity for Edge Network](https://github.com/adobe/aepsdk-edgeidentity-android) | The Identity for Edge Network extension enables identity management from a mobile app when using the Edge Network extension. |
| [Assurance extension](https://github.com/adobe/aepsdk-assurance-android)                      | The Assurance extension enables validation workflows for your Mobile SDK implementation.              |
| [Adobe Experience Platform Mobile SDK Android sample app](https://github.com/adobe/aepsdk-sample-app-android) | Contains Android sample app for the Mobile SDKs.                 |

## Documentation

Additional documentation for usage and Mobile SDK architecture can be found under the [Documentation](Documentation) directory.

## Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
