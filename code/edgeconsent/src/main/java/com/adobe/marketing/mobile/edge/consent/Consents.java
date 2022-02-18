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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

final class Consents {

	private Map<String, Object> consentsMap = new HashMap<>();

	// Suppresses default constructor.
	private Consents() {}

	/**
	 * Copy Constructor.
	 *
	 * @param newConsents the consents values
	 */
	Consents(final Consents newConsents) {
		if (newConsents == null) {
			return;
		}

		this.consentsMap = Utility.deepCopy(newConsents.consentsMap);
	}

	/**
	 * Constructor.
	 *
	 * @param xdmMap a {@link Map} in consents XDMFormat
	 */
	Consents(final Map<String, Object> xdmMap) {
		if (xdmMap == null || xdmMap.isEmpty()) {
			return;
		}

		Object allConsents = xdmMap.get(ConsentConstants.EventDataKey.CONSENTS);

		try {
			consentsMap =
				(allConsents instanceof HashMap)
					? Utility.deepCopy((Map<String, Object>) allConsents)
					: new HashMap<String, Object>();
		} catch (final ClassCastException exp) {
			consentsMap = new HashMap<>();
		}
	}

	/**
	 * Retrieves the timestamp for this {@link Consents}.
	 *
	 * @return timestamp in ISO 8601 date-time string, null if consents does not have timestamp in its metadata
	 */
	String getTimestamp() {
		if (isEmpty()) {
			return null;
		}

		try {
			final Map<String, Object> metaDataContents = (Map<String, Object>) consentsMap.get(
				ConsentConstants.EventDataKey.METADATA
			);

			if (metaDataContents == null) {
				return null;
			}

			return (String) metaDataContents.get(ConsentConstants.EventDataKey.TIME);
		} catch (final ClassCastException exp) {
			return null;
		}
	}

	/**
	 * Use this method to set the metadata timestamp for the consents.
	 *
	 * @param timeStamp {@code long} timestamp in milliseconds indicating the time of last consents update
	 */
	void setTimestamp(final long timeStamp) {
		if (isEmpty()) {
			return;
		}

		Map<String, Object> metaDataContents;

		try {
			metaDataContents = (Map<String, Object>) consentsMap.get(ConsentConstants.EventDataKey.METADATA);

			if (metaDataContents == null || metaDataContents.isEmpty()) {
				metaDataContents = new HashMap<>();
			}
		} catch (final ClassCastException exception) {
			return;
		}

		metaDataContents.put(ConsentConstants.EventDataKey.TIME, DateUtility.dateToISO8601String(new Date(timeStamp)));
		consentsMap.put(ConsentConstants.EventDataKey.METADATA, metaDataContents);
	}

	/**
	 * Verifies if the consents associated with the current object is empty.
	 * Returns true if at least one consent is found, false otherwise.
	 *
	 * @return {@code true} if there are no consents
	 */
	boolean isEmpty() {
		return consentsMap == null || consentsMap.isEmpty();
	}

	/**
	 * Merges the provided {@link Consents} with the current object.
	 * The current object is undisturbed if the provided consent is null or empty.
	 *
	 * @param newConsents the consents that needs to be merged
	 */
	void merge(final Consents newConsents) {
		if (newConsents == null || newConsents.isEmpty()) {
			return;
		}

		if (isEmpty()) {
			consentsMap = newConsents.consentsMap;
			return;
		}

		consentsMap.putAll(newConsents.consentsMap);
	}

	/**
	 * XDMMap representation of the available consents associated with this {@link Consents} object.
	 * <p>
	 * Will make a deep copy of the available consents map before sharing.
	 * An empty XDMFormatted consent Map is returned if there are no consents present in this object.
	 *
	 * @return {@link Map} representing the Consents in XDM format
	 */
	Map<String, Object> asXDMMap() {
		final Map<String, Object> internalConsentMap = consentsMap != null
			? Utility.deepCopy(consentsMap)
			: new HashMap<String, Object>();

		final Map<String, Object> xdmFormattedMap = new HashMap<>();
		xdmFormattedMap.put(ConsentConstants.EventDataKey.CONSENTS, internalConsentMap);
		return xdmFormattedMap;
	}

	/**
	 * Compares the current consent instance the with the passed object
	 *
	 * @return true, if both the consents are equal
	 */
	@Override
	public boolean equals(final Object comparingConsentObject) {
		if (comparingConsentObject == null) {
			return false;
		}

		if (this == comparingConsentObject) {
			return true;
		}

		if (!(comparingConsentObject instanceof Consents)) {
			return false;
		}

		Consents comparingConsent = (Consents) comparingConsentObject;

		if (consentsMap == null) {
			return comparingConsent.consentsMap == null;
		}

		return this.consentsMap.equals(comparingConsent.consentsMap);
	}

	/**
	 * Compares the current consent instance the with the passed object ignoring the timestamp field in metadata
	 *
	 * @return true, if both the consents are equal ignoring timestamp
	 */
	boolean equalsIgnoreTimestamp(final Consents comparingConsent) {
		if (comparingConsent == null) {
			return false;
		}

		if (this == comparingConsent) {
			return true;
		}

		final Consents originalConsentCopy = new Consents(this);
		final Consents comparingConsentCopy = new Consents(comparingConsent);

		originalConsentCopy.removeTimeStamp();
		comparingConsentCopy.removeTimeStamp();

		return originalConsentCopy.consentsMap.equals(comparingConsentCopy.consentsMap);
	}

	/**
	 * Private helper method to remove metadata timestamp from this {@link Consents}
	 */
	private void removeTimeStamp() {
		Map<String, Object> metaDataContents;
		try {
			metaDataContents = (Map<String, Object>) consentsMap.get(ConsentConstants.EventDataKey.METADATA);
		} catch (final ClassCastException exp) {
			return;
		}

		if (metaDataContents == null || metaDataContents.isEmpty()) {
			return;
		}

		metaDataContents.remove(ConsentConstants.EventDataKey.TIME);

		if (metaDataContents.isEmpty()) {
			consentsMap.remove(ConsentConstants.EventDataKey.METADATA);
		} else {
			consentsMap.put(ConsentConstants.EventDataKey.METADATA, metaDataContents);
		}
	}
}
