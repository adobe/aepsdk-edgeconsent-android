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

import com.adobe.marketing.mobile.ExtensionApi;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ConsentTestUtil {

    public static String SAMPLE_METADATA_TIMESTAMP = "2019-09-23T18:15:45Z";

    /**
     * A fully prepared valid consent JSON looks like :
     * {
     *   "consents": {
     *     "adId": {
     *       "val": "n"
     *     },
     *     "collect": {
     *       "val": "y"
     *     },
     *     "metadata": {
     *       "time": "2019-09-23T18:15:45Z"
     *     }
     *   }
     * }
     */

    public static String CreateConsentDataJSONString(final String collectConsentString, final String adIDConsentString) {
        return CreateConsentDataJSONString(collectConsentString, adIDConsentString, null);
    }

    public static String CreateConsentDataJSONString(final String collectConsentString, final String adIDConsentString, final String time) {
        Map<String, Object> consentDataMap = CreateConsentDataMap(collectConsentString, adIDConsentString, time);
        JSONObject jsonObject = new JSONObject(consentDataMap);
        return jsonObject.toString();
    }

    public static Map<String, Object> CreateConsentDataMap(final String collectConsentString, final String adIDConsentString) {
        return CreateConsentDataMap(collectConsentString, adIDConsentString, null);
    }

    public static Map<String, Object> CreateConsentDataMap(final String collectConsentString, final String adIDConsentString, final String time) {
        Map<String, Object> consentData = new HashMap<String, Object>();
        Map<String, Object> consents = new HashMap<String, Object>();
        if (collectConsentString != null) {
            consents.put(ConsentConstants.EventDataKey.COLLECT, new HashMap<String, String>() {
                {
                    put(ConsentConstants.EventDataKey.VALUE, collectConsentString);
                }
            });
        }

        if (adIDConsentString != null) {
            consents.put(ConsentConstants.EventDataKey.ADID, new HashMap<String, String>() {
                {
                    put(ConsentConstants.EventDataKey.VALUE, adIDConsentString);
                }
            });
        }

        if (time != null) {
            Map<String, String> metaDataMap = new HashMap<String, String>();
            metaDataMap.put(ConsentConstants.EventDataKey.TIME, time);
            consents.put(ConsentConstants.EventDataKey.MEATADATA, metaDataMap);
        }

        consentData.put(ConsentConstants.EventDataKey.CONSENTS, consents);
        return consentData;
    }
}
