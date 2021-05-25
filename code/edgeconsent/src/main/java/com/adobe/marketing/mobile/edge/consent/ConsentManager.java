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

import java.util.HashMap;

final class ConsentManager {

	private Consents userOptedConsents; // holds on to consents that are updated using PublicAPI or from Edge Consent Response
	private Consents defaultConsents; // holds on to default consents obtained from configuration response

	/**
	 * Constructor.
	 * <p>
	 * Initializes the {@link #userOptedConsents} from data in persistence.
	 */
	ConsentManager() {
		userOptedConsents = ConsentStorageService.loadConsentsFromPersistence();

		// Initiate update consent with empty consent object if nothing is loaded from persistence
		if (userOptedConsents == null) {
			userOptedConsents = new Consents(new HashMap<String, Object>());
		}
	}

	/**
	 * Merges the provided {@link Consents} with {@link #userOptedConsents} and persists them.
	 *
	 * @param newConsents the newly obtained consents that needs to be merged with existing consents
	 */
	void mergeAndPersist(final Consents newConsents) {
		// merge and persist
		userOptedConsents.merge(newConsents);
		ConsentStorageService.saveConsentsToPersistence(userOptedConsents);
	}

	/**
	 * Updates and replaces the existing default consents with the passed in default consents.
	 *
	 * @param newDefaultConsents the default consent obtained from configuration response event
	 * @return true if `currentConsents` has been updated as a result of updating the default consents
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
	 * <p>
	 * The current consents is computed by overriding the {@link #userOptedConsents} over the {@link #defaultConsents}
	 * The returned consent is never null. When there is no {@code #userOptedConsents} or {@code #defaultConsents}, still an empty consent object is returned.
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
}
