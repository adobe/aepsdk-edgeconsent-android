# Adobe Experience Platform Consent for Edge Network extension API reference

## Prerequisites

Refer to the [Getting started guide](getting-started.md).

## API reference

- [extensionVersion](#extensionversion)
- [getConsents](#getConsents)
- [updateConsents](#updateConsents)
------

### extensionVersion

The extensionVersion() API returns the version of the client-side Consent extension.

#### Java

##### Syntax
```java
public static String extensionVersion();
```

##### Example
```java
String extensionVersion = Consent.extensionVersion();
```

#### Kotlin

##### Example
```kotlin
val extensionVersion = Consent.extensionVersion()
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

#### Kotlin

##### Example
```kotlin
Consent.getConsents { currentConsents ->
    // handle currentConsents
}
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
```

```java
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

#### Kotlin

##### Example
```kotlin
// example 1, updating users collect consent to 'yes'
val collectConsents = mutableMapOf<String, Any>()
collectConsents["collect"] = mutableMapOf("val" to "y")

val consents = mutableMapOf<String, Any>()
consents["consents"] = collectConsents

Consent.update(consents)
```

```kotlin
// example 2, updating users collect consent to 'no'
val collectConsents = mutableMapOf<String, Any>()
collectConsents["collect"] = mutableMapOf("val" to "n")

val consents = mutableMapOf<String, Any>()
consents["consents"] = collectConsents

Consent.update(consents)
```