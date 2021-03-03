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
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
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

    /**
     * Updates the consent for the user with the provided value.
     * <p>
     * The provided consents map must be in XDMFormat.
     * If the consent is already contained in the extension, the old consent is replaced by the newly specified consent.
     * Any new consents provided will be appended to the existing consents list.
     *
     * @param xdmFormattedConsents An {@link Map} of consents in predefined XDMformat
     */
    public static void update(final Map<String,Object> xdmFormattedConsents) {
        if (xdmFormattedConsents == null || xdmFormattedConsents.isEmpty()) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Null/Empty consents passed to Consent.update() Public API. Ignoring the API call.");
        }

        // create and dispatch an consent fragments update event
        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, String.format("Consents.update() API. Failed to dispatch %s event: Error : %s.", ConsentConstants.EventNames.CONSENT_UPDATE_REQUEST,
                        extensionError.getErrorName()));
            }
        };
        final Event event = new Event.Builder(ConsentConstants.EventNames.CONSENT_UPDATE_REQUEST, ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.UPDATE_CONSENT).setEventData(xdmFormattedConsents).build();
        MobileCore.dispatchEvent(event, errorCallback);
    }

    /**
     * Retrieves the current consents for the user.
     * <p>
     * Callback is invoked with null value if no consents were assigned to this user.
     *
     * @param callback a {@link AdobeCallback} of {@link Map} invoked with current consents of the extension
     *                  If an {@link AdobeCallbackWithError} is provided, an {@link AdobeError} can be returned in the
     *  				eventuality of any error that occurred while getting the user consents.
     */
    public static void getConsents(final AdobeCallback<Map<String,Object>> callback) {
        if (callback == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Unexpected null callback, provide a callback to retrieve current consents.");
            return;
        }

        // create and dispatch an consent fragments update event
        final ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                returnError(callback, extensionError);
                MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, String.format("Consents.getConsents() API. Failed to dispatch %s event: Error : %s.", ConsentConstants.EventNames.CONSENT_UPDATE_REQUEST,
                        extensionError.getErrorName()));
            }
        };


        final Event event = new Event.Builder(ConsentConstants.EventNames.GET_CONSENTS_REQUEST, ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.REQUEST_CONTENT).build();
        MobileCore.dispatchEventWithResponseCallback(event, new AdobeCallback<Event>() {
            @Override
            public void call(Event event) {
                if (event == null || event.getEventData() == null) {
                    returnError(callback, AdobeError.UNEXPECTED_ERROR);
                    return;
                }

                callback.call(event.getEventData());
            }
        }, errorCallback);
    }


    /**
     * When an {@link AdobeCallbackWithError} is provided, the fail method will be called with provided {@link AdobeError}.
     * @param callback should not be null, should be instance of {@code AdobeCallbackWithError}
     * @param error the {@code AdobeError} returned back in the callback
     */
    private static void returnError (final AdobeCallback<Map<String,Object>> callback, final AdobeError error) {
        if (callback == null) {
            return;
        }

        final AdobeCallbackWithError<Map<String,Object>> adobeCallbackWithError = callback instanceof AdobeCallbackWithError ?
                (AdobeCallbackWithError<Map<String,Object>>) callback : null;

        if (adobeCallbackWithError != null) {
            adobeCallbackWithError.fail(error);
        }
    }

}
