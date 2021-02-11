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

class ConsentExtension extends Extension {

    /**
     * Constructor.
     *
     * <p>
     * Called during the Consent extension's registration.
     * The following listeners are registered during this extension's registration.
     * <ul>
     *     <li> Listener {@link ConsentListenerConsentUpdateConsent} to listen for event with eventType {@link EventType#CONSENT}
     *     and EventSource {@link EventSource#UPDATE_CONSENT}</li>
     *     <li> Listener {@link ConsentListenerEdgeConsentPreference} to listen for event with eventType {@link EventType#EDGE}
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
        extensionApi.registerEventListener(EventType.CONSENT.getName(), EventSource.UPDATE_CONSENT.getName(), ConsentListenerConsentUpdateConsent.class, listenerErrorCallback);
        extensionApi.registerEventListener(EventType.EDGE.getName(), ConsentConstants.EventSource.CONSENT_PREFERENCE, ConsentListenerEdgeConsentPreference.class, listenerErrorCallback);
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

    void handleConsentUpdate(final Event event) {

    }

    void handleEdgeConsentPreference(final Event event) {
        // TODO: Upcoming in PR's
    }

}
