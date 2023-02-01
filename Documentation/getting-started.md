# Adobe Experience Platform Consent for Edge Network

The Consent for Edge Network mobile extension enables consent preferences collection from your mobile app when using the Adobe Experience Platform Mobile SDK and the Edge Network extension.

## Configure the Consent extension in Data Collection UI

1. Log into [Adobe Experience Platform Data Collection](https://experience.adobe.com/data-collection).
2. From **Tags**, locate or search for your Tag mobile property.
3. In your mobile property, select **Extensions** tab.
4. On the **Catalog** tab, locate or search for the **Consent** extension, and select **Install**.
5. Set your desired default consent level.
6. Select **Save**.
7. Follow the publishing process to update SDK configuration.

> **Note**
> In order to ingest and use the data collected by this extension, follow the guide on [ingesting data using the Consents and Preferences data type](https://experienceleague.adobe.com/docs/experience-platform/xdm/data-types/consents.html#ingest).

> **Warning**
> The use of this extension is currently limited to the setting (and enforcement) of client-side, macro consent flags. While the Mobile SDK APIs allow for granular and global consent preference collection, flags are not consistently enforced with upstream applications and therefore will not accommodate use cases that rely on global/granular consent preferences.

## Add Consent to your app

The Consent for Edge Network extension depends on the following extensions:
* [Mobile Core](https://github.com/adobe/aepsdk-core-android)
* [Edge Network](https://github.com/adobe/aepsdk-edge-android) (required for handling requests to Adobe Edge Network, including consent preferences updates)
* [Identity for Edge Network](https://github.com/adobe/aepsdk-edgeidentity-android) (peer dependency for the Edge Network extension)

1. Add the Mobile Core and Edge extensions to your project using the app's Gradle file:

```gradle
implementation 'com.adobe.marketing.mobile:core:2.+'
implementation 'com.adobe.marketing.mobile:edge:2.+'
implementation 'com.adobe.marketing.mobile:edgeidentity:2.+'
implementation 'com.adobe.marketing.mobile:edgeconsent:2.+'
```

> **Warning**
> Using dynamic dependency versions is not recommended for production apps. Refer to this [page](https://github.com/adobe/aepsdk-core-android/blob/main/Documentation/MobileCore/gradle-dependencies.md) for managing Gradle dependencies.

2. Import the libraries:

#### Java
```java
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.edge.identity.Identity;
import com.adobe.marketing.mobile.edge.consent.Consent;
```

#### Kotlin

```kotlin
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.edge.consent.Consent
```

## Register Edge extensions with Mobile Core

```java
public class MainApp extends Application {

  private final String ENVIRONMENT_FILE_ID = "YOUR_APP_ENVIRONMENT_ID";

	@Override
	public void onCreate() {
		super.onCreate();

		MobileCore.setApplication(this);
		MobileCore.configureWithAppID(ENVIRONMENT_FILE_ID);

		MobileCore.registerExtensions(
			Arrays.asList(Consent.EXTENSION, Identity.EXTENSION, Edge.EXTENSION),
			o -> Log.d("MainApp", "Adobe Experience Platform Mobile SDK was initialized")
		);
	}
}
```

#### Kotlin

```kotlin
class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()

        MobileCore.setApplication(this)
        MobileCore.configureWithAppID("YOUR_APP_ENVIRONMENT_ID")

        val extensions = listOf(Consent.EXTENSION, Identity.EXTENSION, Edge.EXTENSION)
        MobileCore.registerExtensions(extensions) {
            Log.d("MainApp", "Adobe Experience Platform Mobile SDK was initialized")
        }
    }

}
```
