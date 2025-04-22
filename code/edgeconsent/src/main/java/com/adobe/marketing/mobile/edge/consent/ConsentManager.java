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

import androidx.annotation.VisibleForTesting;
import com.adobe.marketing.mobile.services.Log;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

final class ConsentManager {

	private static final String LOG_SOURCE = "ConsentManager";
	private final NamedCollection namedCollection;
	private Consents userOptedConsents; // holds on to consents that are updated using PublicAPI or from Edge

	// Consent Response

	// Package private for testing
	@VisibleForTesting
	Consents defaultConsents; // holds on to default consents obtained from configuration response

	/**
	 * Constructor - initializes the {@link #userOptedConsents} from data in persistence.
	 *
	 * @param namedCollection used for reading/writing consent preferences to persistence
	 */
	ConsentManager(final NamedCollection namedCollection) {
		this.namedCollection = namedCollection;
		userOptedConsents = loadConsentsFromPersistence();

		// Initiate update consent with empty consent object if nothing is loaded from persistence
		if (userOptedConsents == null) {
			userOptedConsents = new Consents(new HashMap<>());
		}
	}

	/**
	 * Merges the provided {@link Consents} with {@link #userOptedConsents} and persists them.
	 *
	 * @param newConsents the newly obtained consents that needs to be merged with existing consents
	 * @return true if `currentConsents` has been updated as a result of merging; ignores differences in timestamp values.
	 */
	boolean mergeAndPersist(final Consents newConsents) {
		// hold temp copy of current consents for comparison
		final Consents currentConsents = getCurrentConsents();

		// merge and persist
		userOptedConsents.merge(newConsents);
		saveConsentsToPersistence(userOptedConsents);

		// return true if currentConsents has been updated as a result of merging
		return !currentConsents.equalsIgnoreTimestamp(getCurrentConsents());
	}

	/**
	 * Updates and replaces the existing default consents with the passed in default consents.
	 *
	 * @param newDefaultConsents the default consent obtained from configuration response event
	 * @return true if `currentConsents` has been updated as a result of updating the default
	 *     consents
	 */
	boolean updateDefaultConsents(final Consents newDefaultConsents) {
		// hold temp copy of current consents for comparison
		final Consents existingConsents = getCurrentConsents();

		// update the defaultConsents variable
		defaultConsents = newDefaultConsents;

		return !existingConsents.equals(getCurrentConsents());
	}

	/**
	 * Getter method to retrieve the current consents.
	 *
	 * <p>The current consents is computed by overriding the {@link #userOptedConsents} over the
	 * {@link #defaultConsents} The returned consent is never null. When there is no {@code
	 * #userOptedConsents} or {@code #defaultConsents}, still an empty consent object is returned.
	 *
	 * @return the sharable complete current consents of this user
	 */
	Consents getCurrentConsents() {
		// if defaults consents are not available, return userOptedConsents
		if (defaultConsents == null || defaultConsents.isEmpty()) {
			return new Consents(userOptedConsents);
		}

		// if default consents are available. Merge the userOpted consents on top of it
		final Consents currentConsents = new Consents(defaultConsents);
		currentConsents.merge(userOptedConsents);

		return currentConsents;
	}

	/**
	 * Loads the requested consents from persistence. The jsonString from persistence is serialized
	 * into {@link Consents} object and returned.
	 *
	 * @return {@link Consent} the previously persisted consents. Returns null if there was any
	 *     {@link JSONException} while serializing JSONString to {@code Consents} object.
	 */
	private Consents loadConsentsFromPersistence() {
		if (namedCollection == null) {
			Log.warning(
				LOG_TAG,
				LOG_SOURCE,
				"loadConsentsFromPersistence failed due to unexpected null namedCollection."
			);
			return null;
		}

		final String jsonString = namedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null);

		if (jsonString == null) {
			Log.trace(LOG_TAG, LOG_SOURCE, "No previous consents were stored in persistence. Current consent is null.");

			return null;
		}

		try {
			final JSONObject jsonObject = new JSONObject(jsonString);
			final Map<String, Object> consentMap = JSONUtils.toMap(jsonObject);
			return new Consents(consentMap);
		} catch (JSONException exception) {
			Log.debug(
				LOG_TAG,
				LOG_SOURCE,
				"Serialization error while reading consent jsonString from persistence. Unable" +
				" to load saved consents from persistence."
			);
			return null;
		}
	}

	/**
	 * Call this method to save the consents to persistence. The consents are converted to
	 * jsonString and stored into persistence.
	 *
	 * @param consents the consents that need to be persisted under key {@link
	 *     ConsentConstants.DataStoreKey#CONSENT_PREFERENCES}
	 */
	private void saveConsentsToPersistence(final Consents consents) {
		if (namedCollection == null) {
			Log.warning(
				LOG_TAG,
				LOG_SOURCE,
				"saveConsentsToPersistence failed due to unexpected null namedCollection."
			);
			return;
		}

		if (consents.isEmpty()) {
			namedCollection.remove(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES);
			return;
		}

		final JSONObject jsonObject = new JSONObject(consents.asXDMMap());
		final String jsonString = jsonObject.toString();
		namedCollection.setString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, jsonString);
	}
}
