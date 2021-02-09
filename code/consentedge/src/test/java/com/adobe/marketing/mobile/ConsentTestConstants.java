/* ******************************************************************************
 * ADOBE CONFIDENTIAL
 *  ___________________
 *
 *  Copyright 2021 Adobe
 *  All Rights Reserved.
 *
 *  NOTICE: All information contained herein is, and remains
 *  the property of Adobe and its suppliers, if any. The intellectual
 *  and technical concepts contained herein are proprietary to Adobe
 *  and its suppliers and are protected by all applicable intellectual
 *  property laws, including trade secret and copyright laws.
 *  Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained
 *  from Adobe.
 ******************************************************************************/

package com.adobe.marketing.mobile;

public class ConsentTestConstants {
    static final String LOG_TAG = "Consent";
    static final String EXTENSION_VERSION = "1.0.0-alpha-1";
    static final String EXTENSION_NAME = "com.adobe.consent";

    final class EventSource {
        static final String UPDATE_CONSENT = "com.adobe.eventSource.updateConsent"; //todo move to core
        static final String CONSENT_PREFERENCE = "consent:preferences";

        private EventSource() { }
    }

    final class EventType {
        static final String CONSENT = "com.adobe.eventType.consent"; //todo move to core
        static final String EDGE = "com.adobe.eventType.edge"; //todo move to core

        private EventType() { }
    }

    private ConsentTestConstants() { }
}
