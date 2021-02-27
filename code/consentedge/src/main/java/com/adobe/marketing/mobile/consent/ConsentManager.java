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

class ConsentManager {
    private Consents currentConsents;

    /**
     * Constructor.
     * <p>
     * Initializes the {@link #currentConsents} from data in persistence.
     */
    ConsentManager() {
        currentConsents = ConsentStorageService.loadConsentsFromPersistence();
    }

    /**
     * Merges the provided {@link Consents} with {@link #currentConsents} and persists them.
     *
     * @param newConsents the newly obtained consents that needs to be merged with existing consents
     * @return {@link Consents} representing the current Consents after the merge
     */
    void mergeAndPersist(final Consents newConsents) {
        // merge and persist
        if (currentConsents == null) {
            currentConsents = new Consents(newConsents);
        } else {
            currentConsents.merge(newConsents);
        }
        ConsentStorageService.saveConsentsToPersistence(currentConsents);
        // make a copy of the merged consents and return
        return;
    }

    /**
     * Getter method to retrieve the {@link #currentConsents} of the Consent Extension.
     * <p>
     * The {@link #currentConsents} could be null if no consents were set.
     *
     * @return the {@link #currentConsents}
     */
    Consents getCurrentConsents() {
        return currentConsents;
    }

}
