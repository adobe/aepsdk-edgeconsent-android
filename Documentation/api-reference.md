# Adobe Experience Platform Consent for Edge Network Extension - Android

## Prerequisites

Refer to the [Getting Started Guide](getting-started.md)

## API reference

- [extensionVersion](#extensionversion)
- [getConsents](#getConsents)
- [registerExtension](#registerextension)
- [updateConsents](#updateConsents)
------

### extensionVersion

The extensionVersion() API returns the version of the client-side Consent extension.

#### Java

##### Syntax
```java
public static String extensionVersion()
```

##### Example
```java
String extensionVersion = Consent.extensionVersion();
```

------

### getConsents

Retrieves the current consent preferences stored in the Consent extension.

#### Java

##### Syntax
```java
public static void getConsents(final AdobeCallback<Map<String, Object>> callback);
```
* callback - callback invoked with the current consents of the extension. If an AdobeCallbackWithError is provided, an AdobeError, can be returned in the eventuality of any error that occurred while getting the user consents. The callback may be invoked on a different thread.

##### Example
```java
Consent.getConsents(new AdobeCallback<Map<String, Object>>() {
    @Override
    public void call(Map<String, Object> currentConsents) {
        // handle currentConsents
    }
});
```

------

### registerExtension

Registers the Edge Consent extension with the Mobile Core SDK.

> **Warning**
> Deprecated as of 2.0.0. Use [MobileCore.registerExtensions API](https://github.com/adobe/aepsdk-core-android/blob/main/docs/Usage/MobileCore.md#registering-extensions-and-starting-the-sdk) instead.

#### Java

##### Syntax
```java
public static void registerExtension()
```

##### Example
```java
import com.adobe.marketing.mobile.Consent

...
Consent.registerExtension();
```

------


### updateConsents

Merges the existing consents with the given consents. Duplicate keys will take the value of those passed in the API.
The Consent extension supports "collect" consents values of 'y' and 'n'.

#### Java

##### Syntax
```java
public static void update(final Map<String, Object> consents);
```
consents - A Map of consents defined based on [Privacy/Personalization/Marketing Preferences (Consents) XDM Schema](https://github.com/adobe/xdm/blob/master/docs/reference/mixins/profile/profile-consents.schema.md).

##### Example
```java
// example 1, updating users collect consent to 'yes'
final Map<String, Object> collectConsents = new HashMap<>();
collectConsents.put("collect", new HashMap<String, String>() {
    {
        put("val", "y");
    }
});

final Map<String, Object> consents = new HashMap<>();
consents.put("consents", collectConsents);

Consent.update(consents);

// example 2, updating users collect consent to 'no'
final Map<String, Object> collectConsents = new HashMap<>();
collectConsents.put("collect", new HashMap<String, String>() {
    {
        put("val", "n");
    }
});

final Map<String, Object> consents = new HashMap<>();
consents.put("consents", collectConsents);

Consent.update(consents);
```