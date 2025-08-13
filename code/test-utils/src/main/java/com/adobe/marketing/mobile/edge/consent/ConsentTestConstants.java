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

/** Class to maintain test constants. */
public class ConsentTestConstants {

	public static final String LOG_TAG = "Consent";

	public static class EventType {

		static final String MONITOR = "com.adobe.functional.eventType.monitor";

		private EventType() {}
	}

	static final class DataStoreKey {

		public static final String CONFIG_DATASTORE = "AdobeMobile_ConfigState";
		public static final String CONSENT_DATASTORE = "com.adobe.edge.consent";
		public static final String CONSENT_PREFERENCES = "consent:preferences";

		private DataStoreKey() {}
	}

	public final class SharedStateName {

		public static final String CONFIG = "com.adobe.module.configuration";
		public static final String EVENT_HUB = "com.adobe.module.eventhub";

		private SharedStateName() {}
	}

	public final class GetConsentHelper {

		public static final String VALUE = "getConsentValue";
		public static final String ERROR = "getConsentError";

		private GetConsentHelper() {}
	}

	public static class EventSource {

		// Used by Monitor Extension
		static final String XDM_SHARED_STATE_REQUEST = "com.adobe.eventSource.xdmSharedStateRequest";
		static final String XDM_SHARED_STATE_RESPONSE = "com.adobe.eventSource.xdmSharedStateResponse";
		static final String SHARED_STATE_REQUEST = "com.adobe.eventSource.sharedStateRequest";
		static final String SHARED_STATE_RESPONSE = "com.adobe.eventSource.sharedStateResponse";
		static final String UNREGISTER = "com.adobe.eventSource.unregister";

		private EventSource() {}
	}

	public static class EventDataKey {

		static final String STATE_OWNER = "stateowner";
		public static final String CONSENTS = "consents";
		static final String METADATA = "metadata";
		static final String TIME = "time";

		private EventDataKey() {}
	}

	public static final class ConfigurationKey {

		public static final String DEFAULT_CONSENT = "consent.default";

		private ConfigurationKey() {}
	}
}
