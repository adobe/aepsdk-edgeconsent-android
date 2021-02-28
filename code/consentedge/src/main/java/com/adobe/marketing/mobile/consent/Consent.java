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

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.Map;

public class Consent {

    private Consent() { }

    /**
     * Returns the version of the {@code Consent} extension
     *
     * @return The version as {@code String}
     */
    public static String extensionVersion() {
        return ConsentConstants.EXTENSION_VERSION;
    }

    /**
     * Registers the extension with the Mobile SDK. This method should be called only once in your application class.
     */
    public static void registerExtension() {
        MobileCore.registerExtension(ConsentExtension.class, new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(ExtensionError extensionError) {
                MobileCore.log(LoggingMode.ERROR, ConsentConstants.LOG_TAG,
                        "There was an error registering the Consent extension: " + extensionError.getErrorName());
            }
        });
    }

    // TODO:<DOC_LINK_HERE> Provide a git book doc link, with an example usage of this API with correct XDM format.
    /**
     * Updates the consent for the user with the provided value.
     * <p>
     * The provided consents map must be in XDMFormat. DOC_LINK_HERE
     * If the consent is already contained in the extension, the old consent is replaced by the newly specified consent.
     * Any new consents provided will be appended to the existing consents list.
     *
     * On a successful consent update following happens.
     * 1. Edge servers are notified with the updated consents for the user.
     * 2. XDMSharedState is updated for {@link Consent} extension with the changed consents.
     * 3. An {@link ConsentConstants.EventNames#CONSENT_PREFERENCES_UPDATED} event is dispatched to eventHub to notify other concerned extensions about the Consent Changes.
     *
     * @param xdmFormattedConsents An {@link Map} of consents in predefined XDMformat
     */
    public static void update(final Map<String,Object> xdmFormattedConsents) {
        // create and dispatch an consent fragments update event
        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, String.format("Consents.update() API. Failed to dispatch %s event: Error : %s.", ConsentConstants.EventNames.CONSENT_FRAGMENTS_UPDATE_REQUEST,
                        extensionError.getErrorName()));
            }
        };
        final Event event = new Event.Builder(ConsentConstants.EventNames.CONSENT_FRAGMENTS_UPDATE_REQUEST, ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.UPDATE_CONSENT).setEventData(xdmFormattedConsents).build();
        MobileCore.dispatchEvent(event, errorCallback);
    }

    /**
     * Retrieves the current consents for the User.
     * <p>
     * Callback is invoked with null value if no consents were assigned to this user.
     * This can happen if there is no default consents provided for your application. Please check the launch property and verify if the consents extension is installed.
     *
     * @param callback a {@link AdobeCallback} of {@link Map} invoked with current consents of the extension
     */
    public static void getConsents(final AdobeCallback<Map<String,Object>> callback) {
        // create and dispatch an consent fragments update event
        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, String.format("Consents.getConsents() API. Failed to dispatch %s event: Error : %s.", ConsentConstants.EventNames.CONSENT_FRAGMENTS_UPDATE_REQUEST,
                        extensionError.getErrorName()));
            }
        };


        final Event event = new Event.Builder(ConsentConstants.EventNames.GET_CONSENTS_REQUEST, ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.REQUEST_CONTENT).build();
        MobileCore.dispatchEventWithResponseCallback(event, new AdobeCallback<Event>() {
            @Override
            public void call(Event event) {
                if (event == null) {
                    callback.call(null);
                    return;
                }

                callback.call(event.getEventData());
            }
        }, errorCallback);
    }

}
