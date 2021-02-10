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

class ConsentListenerEdgeConsentPreference extends ExtensionListener {

    /**
     * Constructor.
     *
     * @param extensionApi an instance of {@link ExtensionApi}
     * @param type         the {@link EventType} this listener is registered to handle
     * @param source       the {@link EventSource} this listener is registered to handle
     */
    ConsentListenerEdgeConsentPreference(final ExtensionApi extensionApi, final String type, final String source) {
        super(extensionApi, type, source);
    }

    /**
     * Method that gets called when event with event type {@link ConsentConstants.EventType#EDGE}
     * and with event source {@link ConsentConstants.EventSource#CONSENT_PREFERENCE}  is dispatched through eventHub.
     *
     * @param event the edge request {@link Event} to be processed
     */
    @Override
    public void hear(final Event event) {
        if (event == null || event.getEventData() == null) {
            Log.warning(ConsentConstants.LOG_TAG, "Event or Event data is null. Ignoring the event listened by consent preference listener");
            return;
        }

        final ConsentExtension parentExtension = (ConsentExtension) getParentExtension();

        if (parentExtension == null) {
            Log.warning(ConsentConstants.LOG_TAG,
                    "The parent extension, associated with the ConsentListenerEdgeConsentPreference is null, ignoring the consent update event.");
            return;
        }

        parentExtension.handleEdgeConsentPreference(event);
    }
}
