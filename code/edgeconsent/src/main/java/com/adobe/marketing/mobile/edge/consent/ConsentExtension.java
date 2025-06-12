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

package com.adobe.marketing.mobile.edge.consent;

import static com.adobe.marketing.mobile.edge.consent.ConsentConstants.LOG_TAG;

import androidx.annotation.NonNull;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.services.ServiceProvider;
import com.adobe.marketing.mobile.util.DataReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ConsentExtension extends Extension {

	private static final String LOG_SOURCE = "ConsentExtension";

	private final ConsentManager consentManager;

	// The last time a consent update was processed from public API.
	private long lastConsentUpdateTime = 0;

	/**
	 * Constructor. It is called by the Mobile SDK when registering the extension and it initializes
	 * the extension and registers event listeners.
	 *
	 * @param extensionApi {@link ExtensionApi} instance
	 */
	protected ConsentExtension(final ExtensionApi extensionApi) {
		this(
			extensionApi,
			ServiceProvider
				.getInstance()
				.getDataStoreService()
				.getNamedCollection(ConsentConstants.DataStoreKey.DATASTORE_NAME)
		);
	}

	/**
	 * Convenience constructor that instantiates a new {@link ConsentManager} using the passed
	 * {@link NamedCollection}.
	 *
	 * <p>Can be used for testing using mocked {@link ExtensionApi} and/or {@link NamedCollection}
	 * instances.
	 *
	 * @param extensionApi {@link ExtensionApi} instance
	 * @param namedCollection {@link NamedCollection} instance from {@link ServiceProvider}
	 */
	protected ConsentExtension(final ExtensionApi extensionApi, final NamedCollection namedCollection) {
		this(extensionApi, new ConsentManager(namedCollection));
	}

	/**
	 * Primary constructor that instantiates the {@link ConsentExtension}.
	 *
	 * @param extensionApi {@link ExtensionApi} instance
	 * @param consentManager {@link ConsentManager} instance from {@link ServiceProvider}
	 * @see ConsentExtension(ExtensionApi)
	 */
	protected ConsentExtension(final ExtensionApi extensionApi, final ConsentManager consentManager) {
		super(extensionApi);
		this.consentManager = consentManager;
	}

	/**
	 * Required override. Each extension must have a unique name within the application.
	 *
	 * @return unique name of this extension
	 */
	@NonNull @Override
	protected String getName() {
		return ConsentConstants.EXTENSION_NAME;
	}

	/**
	 * Sets the friendly name in the EventHub shared state
	 *
	 * @return unique friendly name of this extension
	 */
	@NonNull @Override
	protected String getFriendlyName() {
		return ConsentConstants.FRIENDLY_NAME;
	}

	/**
	 * Optional override.
	 *
	 * @return the version of this extension
	 */
	@NonNull @Override
	protected String getVersion() {
		return ConsentConstants.EXTENSION_VERSION;
	}

	/**
	 * Called during the Consent extension's registration. The ConsentExtension listens for the
	 * following {@link Event}s:
	 *
	 * <ul>
	 *   <li>{@code EventType#EDGE} and EventSource {@Code EventSource#CONSENT_PREFERENCE}
	 *   <li>{@code EventType#CONSENT} and EventSource {@Code EventSource#UPDATE_CONSENT}
	 *   <li>{@Code EventType#CONSENT} and EventSource {@Code EventSource#REQUEST_CONTENT}
	 *   <li>{@Code EventType#CONFIGURATION} and EventSource {{@Code EventSource#RESPONSE_CONTENT}
	 * </ul>
	 *
	 * <p>
	 */
	@Override
	protected void onRegistered() {
		getApi()
			.registerEventListener(
				EventType.EDGE,
				EventSource.CONSENT_PREFERENCE,
				this::handleEdgeConsentPreferenceHandle
			);
		getApi().registerEventListener(EventType.CONSENT, EventSource.UPDATE_CONSENT, this::handleConsentUpdate);
		getApi().registerEventListener(EventType.CONSENT, EventSource.REQUEST_CONTENT, this::handleRequestContent);
		getApi()
			.registerEventListener(
				EventType.CONFIGURATION,
				EventSource.RESPONSE_CONTENT,
				this::handleConfigurationResponse
			);

		handleInitialization();
	}

	/** Share the initial consents loaded from persistence to XDM shared state. */
	void handleInitialization() {
		// share the initial XDMSharedState onRegistered
		final Consents currentConsents = consentManager.getCurrentConsents();

		if (!currentConsents.isEmpty()) {
			shareCurrentConsents(null);
		}
	}

	/**
	 * Use this method to process the event with eventType {@link EventType#CONSENT} and EventSource
	 * {@link EventSource#UPDATE_CONSENT}.
	 *
	 * <p>1. Reads the event data and extract new available consents in XDM Format. 2. Merge with
	 * the existing consents. 3. Dispatch the merged consent to edge for processing.
	 *
	 * @param event the {@link Event} to be processed
	 */
	void handleConsentUpdate(@NonNull final Event event) {
		// bail out if event data is empty
		final Map<String, Object> consentData = event.getEventData();

		if (consentData == null || consentData.isEmpty()) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Consent data not found in consent update event. Dropping event.");
			return;
		}

		// bail out if no valid consents are found in eventData
		final Consents newConsents = new Consents(consentData);

		if (newConsents.isEmpty()) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Unable to find valid data from consent update event. Dropping event.");
			return;
		}

		final boolean outsideTimeout =
			event.getTimestamp() > lastConsentUpdateTime + ConsentConstants.Defaults.IGNORE_CONSENT_UPDATE_INTERVAL_MS;
		// set the timestamp and merge with existing consents
		newConsents.setTimestamp(event.getTimestamp());
		if (consentManager.mergeAndPersist(newConsents) || outsideTimeout) {
			// share and dispatch the updated consents
			shareCurrentConsents(event);
			dispatchEdgeConsentUpdateEvent(newConsents); // dispatches only the newly updated consents
			lastConsentUpdateTime = event.getTimestamp();
		} else {
			// If the consent preferences have not changed and arrived too soon to the previously synced preferences, ignore event
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Consent update request did not change preferences and is within %d ms of the previous update request, dropping event.",
				ConsentConstants.Defaults.IGNORE_CONSENT_UPDATE_INTERVAL_MS
			);
		}
	}

	/**
	 * Handles the event with eventType {@link EventType#EDGE} and EventSource {@link
	 * EventSource#CONSENT_PREFERENCE}.
	 *
	 * <p>1. Reads the event data and extracts new consents from the edge response in XDM Format. 2.
	 * Merges with the existing consents. 3. Creates XDMSharedState and dispatches a Consent
	 * response event for other modules to notify the consent change.
	 *
	 * @param event the Edge consent preferences response {@link Event} to be processed
	 */
	void handleEdgeConsentPreferenceHandle(@NonNull final Event event) {
		// bail out if event data is empty
		final Map<String, Object> eventData = event.getEventData();

		// bail out if you don't find payload in edge consent preference response event
		final List<Map<String, Object>> payload = DataReader.optTypedListOfMap(
			Object.class,
			eventData,
			ConsentConstants.EventDataKey.PAYLOAD,
			null
		);

		if (payload == null || payload.isEmpty()) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Ignoring the consent:preferences handle event from Edge Network, empty/missing" + " payload."
			);
			return;
		}

		// bail out if no valid consents are found in eventData
		final Consents newConsents = new Consents(prepareConsentXDMMapWithPayload(payload.get(0)));

		if (newConsents.isEmpty()) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Ignoring the consent:preferences handle event from Edge Network, no valid" + " consent data found."
			);
			return;
		}

		// If the consentPreferences handle has
		// 1. same consent as current and without timestamp
		// or
		// 2. same consent as current and with same timestamp
		// then ignore this event and do not update the sharedState unnecessarily
		final Consents currentConsent = consentManager.getCurrentConsents();

		if (newConsents.getTimestamp() == null || newConsents.getTimestamp().equals(currentConsent.getTimestamp())) {
			// compare the consents ignoring the timestamp
			if (newConsents.equalsIgnoreTimestamp(currentConsent)) {
				Log.debug(
					LOG_TAG,
					LOG_SOURCE,
					"Ignoring the consent:preferences handle event from Edge Network. There is" +
					" no modification from existing consent data"
				);
				return;
			}
		}

		// update the timestamp and share the updatedConsents as XDMSharedState and dispatch the
		// consent response event
		newConsents.setTimestamp(event.getTimestamp());
		consentManager.mergeAndPersist(newConsents);
		shareCurrentConsents(event);
	}

	/**
	 * Handles the get consents request event and dispatches a response event of EventType {@link
	 * EventType#CONSENT} and EventSource {@link EventSource#RESPONSE_CONTENT} with the current
	 * consent details.
	 *
	 * <p>Dispatched event will contain empty XDMConsentMap if currentConsents are null/empty.
	 *
	 * @param event the {@link Event} requesting consents
	 */
	void handleRequestContent(@NonNull final Event event) {
		final Event responseEvent = new Event.Builder(
			ConsentConstants.EventNames.GET_CONSENTS_RESPONSE,
			EventType.CONSENT,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(consentManager.getCurrentConsents().asXDMMap())
			.inResponseToEvent(event)
			.build();

		getApi().dispatch(responseEvent);
	}

	/**
	 * Handles the configuration response to read the default consents.
	 *
	 * @param event an {@link Event} representing configuration response event
	 */
	void handleConfigurationResponse(@NonNull final Event event) {
		final Map<String, Object> configData = event.getEventData();

		if (configData == null || configData.isEmpty()) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Event data configuration response event is empty, unable to read configuration" +
				" consent.default. Dropping event."
			);
			return;
		}

		final Map<String, Object> defaultConsentMap = DataReader.optTypedMap(
			Object.class,
			configData,
			ConsentConstants.ConfigurationKey.DEFAULT_CONSENT,
			null
		);

		if (defaultConsentMap == null || defaultConsentMap.isEmpty()) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"consent.default not found in configuration. Make sure Consent extension is" +
				" installed in your mobile property"
			);
			// do not return here, even with empty default consent go ahead and update the
			// defaultConsent in ConsentManager
			// This handles the case where if ConsentExtension was installed and then removed from
			// launch property. Then the defaults should be updated.
		}

		if (consentManager.updateDefaultConsents(new Consents(defaultConsentMap))) {
			shareCurrentConsents(event);
		}
	}

	/**
	 * Creates an XDM Shared state with the consents provided and then dispatches {@link
	 * ConsentConstants.EventNames#CONSENT_PREFERENCES_UPDATED} event to eventHub to notify other
	 * concerned extensions about the Consent changes.
	 *
	 * <p>Will not share the XDMSharedEventState or dispatch event if consents is null.
	 *
	 * @param event the {@link Event} that triggered the consents update. The event can be null on
	 *     the first call when extension initializes.
	 */
	private void shareCurrentConsents(final Event event) {
		final Map<String, Object> xdmConsents = consentManager.getCurrentConsents().asXDMMap();

		// set the shared state
		getApi().createXDMSharedState(xdmConsents, event);

		// create and dispatch an consent response event
		Event responseEvent = new Event.Builder(
			ConsentConstants.EventNames.CONSENT_PREFERENCES_UPDATED,
			EventType.CONSENT,
			EventSource.RESPONSE_CONTENT
		)
			.setEventData(xdmConsents)
			.build();

		getApi().dispatch(responseEvent);
	}

	/**
	 * Dispatches an {@link ConsentConstants.EventNames#EDGE_CONSENT_UPDATE} event with the latest
	 * consents in the event data.
	 *
	 * <p>Does not dispatch the event if the latest consents are null/empty.
	 *
	 * @param consents {@link Consents} object representing the updated consents of AEP SDK
	 */
	private void dispatchEdgeConsentUpdateEvent(final Consents consents) {
		// do not send an event if the consent data is empty
		if (consents == null || consents.isEmpty()) {
			Log.debug(LOG_TAG, LOG_SOURCE, "Consent data is null/empty, not dispatching Edge Consent Update event.");
			return;
		}

		// create and dispatch an edge consent update event
		final Event edgeConsentUpdateEvent = new Event.Builder(
			ConsentConstants.EventNames.EDGE_CONSENT_UPDATE,
			EventType.EDGE,
			EventSource.UPDATE_CONSENT
		)
			.setEventData(consents.asXDMMap())
			.build();
		getApi().dispatch(edgeConsentUpdateEvent);
	}

	/**
	 * Helper methods that take the payload from the edge consent preferences response and builds a
	 * XDM formatted consentMap.
	 *
	 * @param payload a {@link Map} representing a payload from edge consent response
	 * @return consentMap in XDM formatted Map
	 */
	private Map<String, Object> prepareConsentXDMMapWithPayload(final Map<String, Object> payload) {
		final Map<String, Object> consentMap = new HashMap<>();
		consentMap.put(ConsentConstants.EventDataKey.CONSENTS, payload);
		return consentMap;
	}
}
