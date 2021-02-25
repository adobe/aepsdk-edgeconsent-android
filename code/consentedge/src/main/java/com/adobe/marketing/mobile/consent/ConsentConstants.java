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

package com.adobe.marketing.mobile.consent;

final class ConsentConstants {

    static final String LOG_TAG = "Consent";
    static final String EXTENSION_VERSION = "1.0.0-alpha-1";
    static final String EXTENSION_NAME = "com.adobe.consent";

    final class EventDataKey {
        static final String CONSENTS = "consents";
        static final String MEATADATA = "metadata";
        static final String TIME = "time";
        static final String VALUE = "val";
        private EventDataKey() { }
    }

    final class EventSource {
        static final String CONSENT_PREFERENCE = "consent:preferences";
        static final String UPDATE_CONSENT = "com.adobe.eventSource.updateConsent";
        static final String REQUEST_CONSENT = "com.adobe.eventSource.requestConsent";
        private EventSource() { }
    }

    final class EventType {
        static final String CONSENT = "com.adobe.eventType.consent";
        static final String EDGE = "com.adobe.eventType.edge";
        private EventType() { }
    }

    final class DataStoreKey {
        static final String DATASTORE_NAME = "com.adobe.consent";
        static final String CONSENT = "consent:preference";
        private DataStoreKey() { }
    }

    final class EventNames {
        static final String EDGE_CONSENT_UPDATE = "Edge Consent Update";
        static final String CONSENT_FRAGMENTS_UPDATE_REQUEST = "Consent Fragments Update Request";
        static final String GET_CONSENTS_REQUEST = "GetConsents Request";
        static final String GET_CONSENTS_RESPONSE = "GetConsents Response";
        static final String CONFIGURATION_UPDATE = "Consent Configuration Update";
        private EventNames() { }
    }

    private ConsentConstants() { }
}
