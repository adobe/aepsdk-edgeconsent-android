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
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import java.util.HashMap;

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
     * Registers the extension with the Mobile SDK. This method should be called only once in your application class.
     */
    public static void update(final HashMap<String,Object> xdmFormattedConsents) {
        // create and dispatch an consent fragments update event
        ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, String.format("Failed to dispatch %s event: Error : %s.", ConsentConstants.EventNames.CONSENT_FRAGMENTS_UPDATE_REQUEST,
                        extensionError.getErrorName()));
            }
        };
        final Event event = new Event.Builder(ConsentConstants.EventNames.EDGE_CONSENT_UPDATE, ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.UPDATE_CONSENT).setEventData(xdmFormattedConsents).build();
        MobileCore.dispatchEvent(event, errorCallback);
    }

}
