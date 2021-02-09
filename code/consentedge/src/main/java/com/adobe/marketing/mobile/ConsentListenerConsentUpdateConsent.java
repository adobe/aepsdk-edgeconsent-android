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

class ConsentListenerConsentUpdateConsent extends ExtensionListener {

    /**
     * Constructor.
     *
     * @param extensionApi an instance of {@link ExtensionApi}
     * @param type         the {@link EventType} this listener is registered to handle
     * @param source       the {@link EventSource} this listener is registered to handle
     */
    ConsentListenerConsentUpdateConsent(final ExtensionApi extensionApi, final String type, final String source) {
        super(extensionApi, type, source);
    }

    /**
     * Method that gets called when event with event type {@link ConsentConstants.EventType#CONSENT}
     * and with event source {@link ConsentConstants.EventSource#UPDATE_CONSENT}  is dispatched through eventHub.
     * <p>
     *
     * @param event the consent update {@link Event} to be processed
     */
    @Override
    public void hear(final Event event) {
        if (event == null || event.getEventData() == null) {
            Log.warning(ConsentConstants.LOG_TAG, "Event or Event data is null. Ignoring the event listened by consentUpdate listener");
            return;
        }

        final ConsentExtension parentExtension = (ConsentExtension) getParentExtension();

        if (parentExtension == null) {
            Log.warning(ConsentConstants.LOG_TAG,
                    "The parent extension, associated with the ConsentListenerConsentUpdateConsent is null, ignoring the consent update event.");
            return;
        }

        parentExtension.handleConsentUpdate(event);
    }

}
