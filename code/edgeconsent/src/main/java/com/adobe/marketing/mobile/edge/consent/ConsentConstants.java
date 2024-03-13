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

package com.adobe.marketing.mobile.edge.consent;

final class ConsentConstants {

	static final String LOG_TAG = "Consent";
	static final String EXTENSION_VERSION = "3.0.0";
	static final String EXTENSION_NAME = "com.adobe.edge.consent";
	static final String FRIENDLY_NAME = "Consent";

	private ConsentConstants() {}

	static final class EventDataKey {

		static final String CONSENTS = "consents";
		static final String METADATA = "metadata";
		static final String PAYLOAD = "payload";

		static final String TIME = "time";

		private EventDataKey() {}
	}

	static final class DataStoreKey {

		static final String DATASTORE_NAME = EXTENSION_NAME;
		static final String CONSENT_PREFERENCES = "consent:preferences";

		private DataStoreKey() {}
	}

	static final class EventNames {

		static final String EDGE_CONSENT_UPDATE = "Edge Consent Update Request";
		static final String CONSENT_UPDATE_REQUEST = "Consent Update Request";
		static final String GET_CONSENTS_REQUEST = "Get Consents Request";
		static final String GET_CONSENTS_RESPONSE = "Get Consents Response";
		static final String CONSENT_PREFERENCES_UPDATED = "Consent Preferences Updated";

		private EventNames() {}
	}

	static final class ConfigurationKey {

		static final String DEFAULT_CONSENT = "consent.default";

		private ConfigurationKey() {}
	}
}
