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

public class ConsentTestConstants {

    public static final String LOG_TAG = "Consent";
    public static final String EXTENSION_VERSION = "1.0.0-alpha-1";
    public static final String EXTENSION_NAME = "com.adobe.consent";

    public final class EventDataKey {
        public static final String CONSENTS = "consents";
        public static final String MEATADATA = "metadata";
        public static final String PAYLOAD = "payload";

        public static final String TIME = "time";
        public static final String VALUE = "val";
        private EventDataKey() { }
    }

    public final class EventSource {
        public static final String CONSENT_PREFERENCE = "consent:preferences";
        public static final String UPDATE_CONSENT = "com.adobe.eventSource.updateConsent";
        public static final String REQUEST_CONTENT = "com.adobe.eventSource.requestContent";
        public static final String RESPONSE_CONTENT = "com.adobe.eventSource.responseContent";
        private EventSource() { }
    }

    public final class EventType {
        public static final String CONSENT = "com.adobe.eventType.consent";
        public static final String EDGE = "com.adobe.eventType.edge";
        private EventType() { }
    }

    public final class DataStoreKey {
        public static final String CONFIG_DATASTORE = "AdobeMobile_ConfigState";
        public static final String CONSENT_DATASTORE = "com.adobe.consent";
        public static final String CONSENT_PREFERENCES = "consent:preferences";
        private DataStoreKey() { }
    }

    public final class EventNames {
        public static final String EDGE_CONSENT_UPDATE = "Edge Consent Update Request";
        public static final String CONSENT_UPDATE_REQUEST = "Consent Update Request";
        public static final String GET_CONSENTS_REQUEST = "Get Consents Request";
        public static final String GET_CONSENTS_RESPONSE = "Get Consents Response";
        public static final String CONSENT_PREFERENCES_UPDATED = "Consent Preferences Updated";
        private EventNames() { }
    }

    private ConsentTestConstants() { }
}

