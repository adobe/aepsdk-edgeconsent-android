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

package com.adobe.marketing.mobile;

class FunctionalTestConstants {


	class EventType {
		static final String EDGE = "com.adobe.eventType.edge";
		static final String MONITOR = "com.adobe.functional.eventType.monitor";

		private EventType() {}
	}

	class EventSource {
		static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";
		static final String RESPONSE_CONTENT = "com.adobe.eventSource.responseContent";
		static final String ERROR_RESPONSE_CONTENT = "com.adobe.eventSource.errorResponseContent";

		// Used by Monitor Extension
		static final String SHARED_STATE_REQUEST = "com.adobe.eventSource.sharedStateRequest";
		static final String SHARED_STATE_RESPONSE = "com.adobe.eventSource.sharedStateResponse";
		static final String UNREGISTER = "com.adobe.eventSource.unregister";

		private EventSource() {}
	}

	class Defaults {
		static final int WAIT_TIMEOUT_MS = 1000;
		static final int WAIT_EVENT_TIMEOUT_MS = 2000;
		static final int WAIT_SHARED_STATE_TIMEOUT_MS = 3000;


		private Defaults() {};
	}

	class EventDataKey {
		// Used by Monitor Extension
		static final String STATE_OWNER = "stateowner";

		private EventDataKey() {};
	}

	class DataStoreKeys {
		static final String STORE_PAYLOADS = "storePayloads";

		private DataStoreKeys() {};
	}

	class SharedState {
		static final String STATE_OWNER = "stateowner";
		static final String CONFIGURATION = "com.adobe.module.configuration";
		static final String ASSURANCE = "com.adobe.assurance";
		private SharedState() {}
	}

	private FunctionalTestConstants() {}

}
