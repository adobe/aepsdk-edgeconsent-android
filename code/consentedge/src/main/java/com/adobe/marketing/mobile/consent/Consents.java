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

import java.util.HashMap;
import java.util.Map;

public class Consents {
    private ConsentValue adIdConsent;
    private ConsentValue collectConsent;
    private ConsentMetadata metadata;

    public Consents() {
    }

    // copy Constructor
    Consents(final Consents newConsent) {
        this.adIdConsent = newConsent.adIdConsent;
        this.collectConsent = newConsent.collectConsent;
        this.metadata = newConsent.metadata;
    }

    Consents(final Map<String, Object> consentData) {
        this(consentData, null);
    }

    Consents(final Map<String, Object> consentData, final String iso8601timestamp) {
        initWithConsentData(consentData);
        if (iso8601timestamp != null && !iso8601timestamp.isEmpty()) {
            metadata = new ConsentMetadata(iso8601timestamp);
        }
    }

    // ========================================================================================
    // Public Methods : Setters
    // ========================================================================================
    public void setAdIdConsent(final ConsentValue adIdConsent) {
        this.adIdConsent = adIdConsent;
    }

    public void setCollectConsent(final ConsentValue collectConsent) {
        this.collectConsent = collectConsent;
    }

    // ========================================================================================
    // Public Methods : Getters
    // ========================================================================================

    public ConsentValue getAdIdConsent() {
        return adIdConsent;
    }

    public ConsentValue getCollectConsent() {
        return collectConsent;
    }

    public ConsentMetadata getMetadata() {
        return metadata;
    }

    boolean isEmpty() {
        return adIdConsent == null && collectConsent == null;
    }

    // ========================================================================================
    // protected methods
    // ========================================================================================

    /**
     * Merges the provided {@link Consents} with the current object.
     * The current object is undisturbed if the provided consent is null.
     *
     * @param newConsents the consents that needs to be merged
     */
    void merge(final Consents newConsents) {
        if (newConsents == null) {
            return;
        }
        collectConsent = (newConsents.collectConsent != null) ? newConsents.collectConsent : collectConsent;
        adIdConsent = (newConsents.adIdConsent != null) ? newConsents.adIdConsent : adIdConsent;
        metadata = (newConsents.metadata != null) ? newConsents.metadata : metadata;
    }

    /**
     * Dictionary representation of the available consent associated with this {@link Consents} object.
     *
     * @return {@link Map} representing the Consents in XDM format
     */
    Map<String, Object> asMap() {

        if (isEmpty()) {
            return null;
        }

        Map<String, Object> allConsents = new HashMap<String, Object>();

        // add collect consent if it exist
        Map<String, String> collectConsentMap = getConsentMapFromValue(collectConsent);
        if (collectConsentMap != null) {
            allConsents.put(ConsentConstants.EventDataKey.COLLECT, collectConsentMap);
        }

        // add adId consent if it exist
        Map<String, String> adIDConsentMap = getConsentMapFromValue(adIdConsent);
        if (collectConsentMap != null) {
            allConsents.put(ConsentConstants.EventDataKey.ADID, adIDConsentMap);
        }


        // add Metadata
        if (metadata != null) {
            // check for each metadata value and add them in the map
            Map<String, String> metaDataMap = new HashMap<String, String>();
            if (metadata.getTime() != null) {
                metaDataMap.put(ConsentConstants.EventDataKey.TIME, metadata.getTime());
            }
            allConsents.put(ConsentConstants.EventDataKey.MEATADATA, metaDataMap);
        }


        Map<String, Object> consentMap = new HashMap<String, Object>();
        consentMap.put(ConsentConstants.EventDataKey.CONSENTS, allConsents);
        return consentMap;
    }

    // ========================================================================================
    // private methods
    // ========================================================================================

    private void initWithConsentData(final Map<String, Object> consentMap) {
        Object allConsents = consentMap.get(ConsentConstants.EventDataKey.CONSENTS);
        final Map<String, Object> allConsentsMap = (allConsents instanceof HashMap) ? (Map<String, Object>) allConsents : null;

        if (allConsentsMap == null || allConsentsMap.isEmpty()) {
            return;
        }

        // check and update collect consent.
        final Map<String, String> collectConsentMap = (Map<String, String>) allConsentsMap.get(ConsentConstants.EventDataKey.COLLECT);
        if (collectConsentMap != null && !collectConsentMap.isEmpty()) {
            collectConsent = getConsentValueFromMap(collectConsentMap);
        }

        // check and update adId consent.
        final Map<String, String> adIdConsentMap = (Map<String, String>) allConsentsMap.get(ConsentConstants.EventDataKey.ADID);
        if (adIdConsentMap != null && !adIdConsentMap.isEmpty()) {
            adIdConsent = getConsentValueFromMap(adIdConsentMap);
        }

        // check and update time
        // TODO : double check on the assumption that metadata is always String:String
        final Map<String, String> metadataMap = (Map<String, String>) allConsentsMap.get(ConsentConstants.EventDataKey.MEATADATA);
        if (metadataMap != null && !metadataMap.isEmpty()) {
            String time = getTimeFromMetaDataMap(metadataMap);
            if (time != null) {
                metadata = new ConsentMetadata(time);
            }
        }
    }

    // ========================================================================================
    // Helper functions
    // ========================================================================================

    private Map<String, String> getConsentMapFromValue(final ConsentValue value) {
        if (value == null) {
            return null;
        }

        Map<String, String> consentMap = new HashMap<String, String>();
        consentMap.put(ConsentConstants.EventDataKey.VALUE, value.stringValue());
        return consentMap;
    }


    private ConsentValue getConsentValueFromMap(final Map<String, String> consentMap) {
        String consentValueString = consentMap.get(ConsentConstants.EventDataKey.VALUE);
        if (consentValueString == null || consentValueString.isEmpty()) {
            return null;
        }

        return ConsentValue.get(consentValueString);
    }

    private String getTimeFromMetaDataMap(final Map<String, String> metaDataMap) {
        String time = metaDataMap.get(ConsentConstants.EventDataKey.TIME);
        if (time == null || time.isEmpty()) {
            return null;
        }
        // TODO : check if we need validation of time string (ISO6801)
        return time;
    }

}

