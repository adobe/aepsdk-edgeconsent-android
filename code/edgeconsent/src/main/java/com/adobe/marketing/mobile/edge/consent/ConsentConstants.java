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
	static final String EXTENSION_VERSION = "3.0.2";
	static final String EXTENSION_NAME = "com.adobe.edge.consent";
	static final String FRIENDLY_NAME = "Consent";

	private ConsentConstants() {}

	static final class Defaults {

		static final long IGNORE_CONSENT_UPDATE_INTERVAL_MS = 1000; // 1 second
	}

	static final class EventDataKey {

		static final String CONSENTS = "consents";
		static final String METADATA = "metadata";
		static final String PAYLOAD = "payload";

		static final String TIME = "time";

		static final String COLLECT = "collect";
		static final String VAL = "val";
		static final String YES = "y";
		static final String NO = "n";
		static final String PENDING = "p";

		/**
		 * Top-level boolean field included in {@link EventNames#CONSENT_PREFERENCES_UPDATED}
		 * events when the effective {@code consents.collect.val} has just transitioned to
		 * {@code "y"} from a non-{@code "y"} value (including absent / null). Absent when
		 * no such transition occurred. Listeners that have data gated by collect consent
		 * should re-sync when this flag is {@code true}.
		 */
		static final String COLLECT_CONSENT_RESYNC_REQUIRED = "collectConsentResyncRequired";

		private EventDataKey() {}
	}

	static final class DataStoreKey {

		static final String DATASTORE_NAME = EXTENSION_NAME;
		static final String CONSENT_PREFERENCES = "consent:preferences";

		/**
		 * Persisted "last definitive collect.val" used for cross-session transition
		 * detection. Records the most recent {@code "y"}, {@code "n"}, or null observation
		 * — {@code "p"} (pending) events do NOT advance it. See
		 * {@link ConsentManager#evaluateCollectConsentTransition()}.
		 */
		static final String LAST_DEFINITIVE_COLLECT_CONSENT = "consent.lastDefinitiveCollect";

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
