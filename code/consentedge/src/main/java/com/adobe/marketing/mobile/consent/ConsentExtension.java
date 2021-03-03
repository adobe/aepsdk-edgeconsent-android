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

import java.util.HashMap;
import java.util.List;

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
     *     <li> Listener {@link ListenerEdgeConsentPreference} to listen for event with eventType {@link ConsentConstants.EventType#EDGE}
     *     and EventSource {@link ConsentConstants.EventSource#CONSENT_PREFERENCE}</li>
     *     <li> Listener {@link ListenerConsentUpdateConsent} to listen for event with eventType {@link ConsentConstants.EventType#CONSENT}
     *      and EventSource {@link ConsentConstants.EventSource#UPDATE_CONSENT}</li>
     *     <li> Listener {@link ListenerConsentRequestContent} to listen for event with eventType {@link ConsentConstants.EventType#CONSENT}
     *      and EventSource {@link ConsentConstants.EventSource#REQUEST_CONTENT}</li>
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
        extensionApi.registerEventListener(ConsentConstants.EventType.EDGE, ConsentConstants.EventSource.CONSENT_PREFERENCE, ListenerEdgeConsentPreference.class, listenerErrorCallback);
        extensionApi.registerEventListener(ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.UPDATE_CONSENT, ListenerConsentUpdateConsent.class, listenerErrorCallback);
        extensionApi.registerEventListener(ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.REQUEST_CONTENT, ListenerConsentRequestContent.class, listenerErrorCallback);
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
     * 1. Reads the event data and extract new available consents in XDM Format.
     * 2. Merge with the existing consents.
     * 3. Dispatch the merged consent to edge for processing.
     *
     * @param event the {@link Event} to be processed
     */
    void handleConsentUpdate(final Event event) {
        // bail out if event data is empty
        final Map<String, Object> consentData = event.getEventData();
        if (consentData == null || consentData.isEmpty()) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Consent data not found in consent update event. Dropping event.");
            return;
        }

        // bail out if no valid consents are found in eventData
        final Consents newConsents = new Consents(consentData);
        if (newConsents.isEmpty()) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Unable to find valid data from consent update event. Dropping event.");
            return;
        }

        // set the timestamp and merge with existing consents
        newConsents.setTimestamp(event.getTimestamp());
        consentManager.mergeAndPersist(newConsents);

        // share and dispatch the updated consents
        createXDMStateAndDispatchResponseEvent(consentManager.getCurrentConsents(), event);
        dispatchEdgeConsentUpdateEvent(consentManager.getCurrentConsents());
    }

    /**
     * Handles the event with eventType {@link ConsentConstants.EventType#EDGE} and EventSource {@link ConsentConstants.EventSource#CONSENT_PREFERENCE}.
     * <p>
     * 1. Reads the event data and extract newconsents from the edge response in XDM Format.
     * 2. Merge with the existing consents.
     * 3. Creates XDMSharedState and dispatches a Consent response event for other modules to notify the consent change.
     *
     * @param event the Edge consent preferences response {@link Event} to be processed
     */
    void handleEdgeConsentPreference(final Event event) {
        // bail out if event data is empty
        final Map<String, Object> eventData = event.getEventData();

        // bail out if you don't find payload in edge consent preference response event
        final List<Map<String, Object>> payload;
        try {
            payload = (List<Map<String, Object>>) eventData.get(ConsentConstants.EventDataKey.PAYLOAD);
        } catch (ClassCastException exp) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Invalid payload from edge server. Dropping event.");
            return;
        }

        if (payload == null || payload.isEmpty()) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "consent.preferences response event from edge is missing payload. Dropping event.");
            return;
        }


        // bail out if no valid consents are found in eventData
        final Consents newConsents = new Consents(prepareConsentXDMMapWithPayload(payload.get(0)));
        if (newConsents.isEmpty()) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Unable to find valid consent data from edge consent preference response event. Dropping event.");
            return;
        }

        // update the timestamp and share the updatedConsents as XDMSharedState and dispatch the consent response event
        // todo double check if need to explicity set time stamp on edge consent preference event. Wondering the timestamp would already be in the xdmFormatted edge response
        newConsents.setTimestamp(event.getTimestamp());
        consentManager.mergeAndPersist(newConsents);

        createXDMStateAndDispatchResponseEvent(consentManager.getCurrentConsents(), event);
    }

    /**
     * Handles the get consents request event and dispatches a response event of EventType {@link ConsentConstants.EventType#CONSENT} and EventSource
     * {@link ConsentConstants.EventSource#RESPONSE_CONTENT} with the current consent details.
     * <p>
     * Dispatched event will contain empty XDMConsentMap if currentConsents are null/empty.
     *
     * @param event the {@link Event} requesting consents
     */
    void handleRequestContent(final Event event) {
        Consents currentConsent = consentManager.getCurrentConsents();
        Map<String,Object> xdmMap = (currentConsent != null && !currentConsent.isEmpty()) ? currentConsent.asXDMMap() : emptyConsentXDMMap() ;

        ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, String.format("Failed to dispatch %s event: Error : %s.", ConsentConstants.EventNames.GET_CONSENTS_RESPONSE,
                        extensionError.getErrorName()));
            }
        };
        final Event responseEvent = new Event.Builder(ConsentConstants.EventNames.GET_CONSENTS_RESPONSE, ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.RESPONSE_CONTENT).setEventData(xdmMap).build();
        MobileCore.dispatchResponseEvent(responseEvent, event, errorCallback);
    }

    /**
     * Creates an XDM Shared state with the consents provided and then dispatches {@link ConsentConstants.EventNames#CONSENT_PREFERENCES_UPDATED}
     * event to eventHub to notify other concerned extensions about the Consent changes.
     * <p>
     * Will not share the XDMSharedEventState or dispatch event if consents is null.
     *
     * @param consents {@link Consents} object representing the latest consents
     */
    private void createXDMStateAndDispatchResponseEvent(final Consents consents, final Event event) {
        if (consents == null) {
            return;
        }

        // set the shared state
        ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, String.format("Failed create XDM shared state. Error : %s.", extensionError.getErrorName()));
            }
        };

        getApi().setXDMSharedEventState(consents.asXDMMap(), event, errorCallback);


        // create and dispatch an consent response event
        final Event responseEvent = new Event.Builder(ConsentConstants.EventNames.CONSENT_PREFERENCES_UPDATED, ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.RESPONSE_CONTENT).setEventData(consents.asXDMMap()).build();
        ExtensionErrorCallback<ExtensionError> dispatchErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, String.format("Failed to dispatch %s event: Error : %s.", responseEvent.getName(),
                        extensionError.getErrorName()));
            }
        };
        MobileCore.dispatchEvent(responseEvent, dispatchErrorCallback);
    }

    /**
     * Dispatches an {@link ConsentConstants.EventNames#EDGE_CONSENT_UPDATE} event with the latest consents in the event data.
     * <p>
     * Does not dispatch the event if the latests consents is null/empty.
     *
     * @param consents {@link Consents} object representing the updated consents of AEP SDK
     */
    private void dispatchEdgeConsentUpdateEvent(final Consents consents) {
        // do not send an event if the consent data is empty
        if (consents == null || consents.isEmpty()) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Consent data is null/empty, not dispatching Edge Consent Update event.");
            return;
        }

        // create and dispatch an edge consent update event
        final Event edgeConsentUpdateEvent = new Event.Builder(ConsentConstants.EventNames.EDGE_CONSENT_UPDATE, ConsentConstants.EventType.EDGE, ConsentConstants.EventSource.UPDATE_CONSENT).setEventData(consents.asXDMMap()).build();
        ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, String.format("Failed to dispatch %s event: Error : %s.", edgeConsentUpdateEvent.getName(),
                        extensionError.getErrorName()));
            }
        };
        MobileCore.dispatchEvent(edgeConsentUpdateEvent, errorCallback);
    }


    /**
     * Helper methods that take the payload from the edge consent preferences response and builds a XDM formatted consentMap.
     *
     * @param payload a {@link Map} representing a payload from edge consent response
     */
    private Map<String,Object> prepareConsentXDMMapWithPayload(final Map<String,Object> payload) {
        Map<String,Object> consentMap = new HashMap<>();
        consentMap.put(ConsentConstants.EventDataKey.CONSENTS, payload);
        return consentMap;
    }

    private Map<String, Object> emptyConsentXDMMap() {
        Map<String,Object> consentMap = new HashMap<>();
        consentMap.put(ConsentConstants.EventDataKey.CONSENTS, new HashMap<String,Object>());
        return consentMap;
    }

}
