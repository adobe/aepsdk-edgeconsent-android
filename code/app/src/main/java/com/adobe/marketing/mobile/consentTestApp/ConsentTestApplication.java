/*
  Copyright 2021 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.consentTestApp;

import android.app.Application;
import android.util.Log;
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.Edge;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.edge.consent.Consent;

public class ConsentTestApplication extends Application {

	private static final String LOG_TAG = "ConsentTestApplication";

	// TODO: fill in your Launch environment ID here
	private final String LAUNCH_ENVIRONMENT_ID = "";

	@Override
	public void onCreate() {
		super.onCreate();
		MobileCore.setApplication(this);

		MobileCore.setLogLevel(LoggingMode.VERBOSE);

		/* Launch generates a unique environment ID that the SDK uses to retrieve your
		configuration. This ID is generated when an app configuration is created and published to
		a given environment. It is strongly recommended to configure the SDK with the Launch
		environment ID.
		*/
		MobileCore.configureWithAppID(LAUNCH_ENVIRONMENT_ID);

		// register AEP Mobile extensions
		Consent.registerExtension();
		Edge.registerExtension();
		Assurance.registerExtension();

		// once all the extensions are registered, call MobileCore.start(...) to start processing the events
		MobileCore.start(
			new AdobeCallback() {
				@Override
				public void call(final Object o) {
					Log.d(LOG_TAG, "Mobile SDK was initialized");
				}
			}
		);
	}
}
