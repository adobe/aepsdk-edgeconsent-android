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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.Map;

class ConsentExtension extends Extension {

    ConsentManager consentManager;

    /**
     * Constructor.
     *
     * <p>
     * Called during the Consent extension's registration.
     * The following listeners are registered during this extension's registration.
     * <ul>
     *     <li> Listener {@link ConsentListenerConsentUpdateConsent} to listen for event with eventType {@link ConsentConstants.EventType#CONSENT}
     *     and EventSource {@link ConsentConstants.EventSource#UPDATE_CONSENT}</li>
     *     <li> Listener {@link ConsentListenerEdgeConsentPreference} to listen for event with eventType {@link ConsentConstants.EventType#EDGE}
     *     and EventSource {@link ConsentConstants.EventSource#CONSENT_PREFERENCE}</li>
     * </ul>
     * <p>
     * Thread : Background thread created by MobileCore
     *
     * @param extensionApi {@link ExtensionApi} instance
     */
    protected ConsentExtension(final ExtensionApi extensionApi) {
        super(extensionApi);

        ExtensionErrorCallback<ExtensionError> listenerErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.ERROR, ConsentConstants.LOG_TAG, String.format("Failed to register listener, error: %s",
                        extensionError.getErrorName()));
            }
        };
        extensionApi.registerEventListener(ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.UPDATE_CONSENT, ConsentListenerConsentUpdateConsent.class, listenerErrorCallback);
        extensionApi.registerEventListener(ConsentConstants.EventType.EDGE, ConsentConstants.EventSource.CONSENT_PREFERENCE, ConsentListenerEdgeConsentPreference.class, listenerErrorCallback);
        consentManager = new ConsentManager();
    }

    /**
     * Required override. Each extension must have a unique name within the application.
     *
     * @return unique name of this extension
     */
    @Override
    protected String getName() {
        return ConsentConstants.EXTENSION_NAME;
    }

    /**
     * Optional override.
     *
     * @return the version of this extension
     */
    @Override
    protected String getVersion() {
        return ConsentConstants.EXTENSION_VERSION;
    }


    /**
     * Use this method to process the event with eventType {@link ConsentConstants.EventType#CONSENT}
     * and EventSource {@link ConsentConstants.EventSource#UPDATE_CONSENT}.
     * <p>
     * 1. Read the event data and extract new available consents.
     * 2. Merge with the existing consents.
     * 3. Dispatch the merged consent to edge for processing.
     *
     * @param event the {@link Event} to be processed
     */
    void handleConsentUpdate(final Event event) {
        // bail out if event data is empty
        Map<String, Object> consentData = event.getEventData();
        if (consentData == null || consentData.isEmpty()) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Consent data not found in consent update event. Dropping event.");
            return;
        }

        // bail out if no valid consents are found in eventData
        Consents newConsents = new Consents(consentData);
        if (newConsents.isEmpty()) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Unable to find valid data from consent update event. Dropping event.");
            return;
        }


        // dispatch obtained consent if current consent is null/empty
        Consents currentConsents = consentManager.getCurrentConsents();
        if (currentConsents == null || currentConsents.isEmpty()) {
            dispatchEdgeConsentUpdateEvent(newConsents);
            return;
        }

        // If current consent exists, create a copy and merge with newConsent
        Consents copyConsent = new Consents(currentConsents);
        copyConsent.merge(newConsents);
        dispatchEdgeConsentUpdateEvent(copyConsent);
    }

    void handleEdgeConsentPreference(final Event event) {
        // TODO: In Upcoming PR's
    }

    /**
     * Dispatches a consent update event with the latest consent represented as event data.
     * <p>
     * Does not dispatch the event if the current consent data is null/empty.
     *
     * @param consents {@link Consents} object representing the updated consents of AEP SDK
     */
    private void dispatchEdgeConsentUpdateEvent(final Consents consents) {
        // do not send an event if the consent data is empty
        if (consents == null || consents.isEmpty()) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Current consent data is null/empty, not dispatching consent update event.");
            return;
        }

        // create and dispatch an consent update event
        ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, String.format("Failed to dispatch %s event: Error : %s.", ConsentConstants.EventNames.EDGE_CONSENT_UPDATE,
                        extensionError.getErrorName()));
            }
        };
        Event event = new Event.Builder(ConsentConstants.EventNames.EDGE_CONSENT_UPDATE, ConsentConstants.EventType.EDGE, ConsentConstants.EventSource.UPDATE_CONSENT).setEventData(consents.asMap()).build();
        MobileCore.dispatchEvent(event, errorCallback);
    }

}
