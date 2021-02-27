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


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

class Consents {
    private Map<String, Object> consentsMap;

    // Suppresses default constructor.
    private Consents() {
    }

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
        consentsMap = (allConsents instanceof HashMap) ? Utility.deepCopy((Map<String, Object>) allConsents) : null;
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
        Map<String, Object> metaDataContents = (Map<String, Object>) consentsMap.get(ConsentConstants.EventDataKey.MEATADATA);
        if (metaDataContents == null || metaDataContents.isEmpty()) {
            metaDataContents = new HashMap<>();
        }

        metaDataContents.put(ConsentConstants.EventDataKey.TIME, DateUtility.dateToISO8601String(new Date(timeStamp)));
        consentsMap.put(ConsentConstants.EventDataKey.MEATADATA, metaDataContents);
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
     * Dictionary representation of the available consents associated with this {@link Consents} object.
     * <p>
     * Will make a deep copy of the avaialble consents map before sharing.
     *
     * @return {@link Map} representing the Consents in XDM format
     */
    Map<String, Object> asXDMMap() {
        if (isEmpty()) {
            return null;
        }

        Map<String, Object> xdmFormattedMap = new HashMap<>();
        xdmFormattedMap.put(ConsentConstants.EventDataKey.CONSENTS, Utility.deepCopy(consentsMap));
        return xdmFormattedMap;
    }

}

