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

class ConsentListenerEdgeConsentPreference extends ExtensionListener {

    /**
     * Constructor.
     *
     * @param extensionApi an instance of {@link ExtensionApi}
     * @param type         the {@link String} eventType this listener is registered to handle
     * @param source       the {@link String} eventSource this listener is registered to handle
     */
    ConsentListenerEdgeConsentPreference(final ExtensionApi extensionApi, final String type, final String source) {
        super(extensionApi, type, source);
    }

    /**
     * Method that gets called when event with event type {@link EventType#EDGE}
     * and with event source {@link ConsentConstants.EventSource#CONSENT_PREFERENCE}  is dispatched through eventHub.
     *
     * @param event the edge request {@link Event} to be processed
     */
    @Override
    public void hear(final Event event) {
        if (event == null || event.getEventData() == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Event or Event data is null. Ignoring the event listened by ConsentListenerEdgeConsentPreference");
            return;
        }

        final ConsentExtension parentExtension = (ConsentExtension) getParentExtension();

        if (parentExtension == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG,
                    "The parent extension, associated with the ConsentListenerEdgeConsentPreference is null, ignoring the consent update event.");
            return;
        }

        parentExtension.handleEdgeConsentPreference(event);
    }
}
