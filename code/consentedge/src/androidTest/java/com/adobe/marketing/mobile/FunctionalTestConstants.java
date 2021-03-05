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

public class FunctionalTestConstants {

    public static final String CONSENT_EXTENSION_NAME = "com.adobe.consent";

    public class EventType {
        static final String MONITOR = "com.adobe.functional.eventType.monitor";
        private EventType() {}
    }

    public class EventSource {
        // Used by Monitor Extension
        static final String SHARED_STATE_REQUEST = "com.adobe.eventSource.sharedStateRequest";
        static final String SHARED_STATE_RESPONSE = "com.adobe.eventSource.sharedStateResponse";
        static final String UNREGISTER = "com.adobe.eventSource.unregister";
        private EventSource() {}
    }

    public class EventDataKey {
        static final String STATE_OWNER = "stateowner";
        private EventDataKey() {};
    }

    final class DataStoreKey {
        static final String DATASTORE_NAME = "com.adobe.consent";
        static final String CONSENT_PREFERENCES = "consent:preferences";
        private DataStoreKey() { }
    }


}
