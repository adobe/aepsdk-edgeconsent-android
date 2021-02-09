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

class ConsentExtension extends Extension {

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
     *
     * Thread : Background thread created by MobileCore
     *
     * @param extensionApi  {@link ExtensionApi} instance
     */
    protected ConsentExtension(final ExtensionApi extensionApi) {
        super(extensionApi);

        ExtensionErrorCallback<ExtensionError> listenerErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                Log.error(ConsentConstants.LOG_TAG, String.format("Failed to register listener, error: %s",
                        extensionError.getErrorName()));
            }
        };
        extensionApi.registerEventListener(ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.UPDATE_CONSENT, ConsentListenerConsentUpdateConsent.class, listenerErrorCallback);
        extensionApi.registerEventListener(ConsentConstants.EventType.EDGE, ConsentConstants.EventSource.CONSENT_PREFERENCE, ConsentListenerEdgeConsentPreference.class, listenerErrorCallback);
    }

    /**
     * Required override. Each extension must have a unique name within the application.
     * @return unique name of this extension
     */
    @Override
    protected String getName() {
        return ConsentConstants.EXTENSION_NAME;
    }

    /**
     * Optional override.
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
