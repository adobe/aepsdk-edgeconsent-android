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

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.adobe.marketing.mobile.consent.ConsentConstants.LOG_TAG;

class Utility {

    /**
     * Method to serialize jsonObject to Map.
     *
     * @param jsonObject the {@link JSONObject} to be serialized
     * @return a {@link Map} representing the serialized JSONObject
     */

    static Map<String, Object> toMap(final JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        Map<String, Object> jsonAsMap = new HashMap<String, Object>();
        Iterator<String> keysIterator = jsonObject.keys();

        while (keysIterator.hasNext()) {
            String nextKey  = keysIterator.next();
            Object value = null;
            Object returnValue;

            try {
                value = jsonObject.get(nextKey);
            } catch (JSONException e) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
                        "toMap - Unable to convert jsonObject to Map for key " + nextKey + ", skipping.");
            }

            if (value == null) {
                continue;
            }

            if (value instanceof JSONObject) {
                returnValue = toMap((JSONObject)value);
            } else if (value instanceof JSONArray) {
                returnValue = toList((JSONArray) value);
            } else {
                returnValue = value;
            }

            jsonAsMap.put(nextKey, returnValue);
        }

        return jsonAsMap;
    }


    /**
     * Converts provided {@link JSONArray} into {@link List} for any number of levels which can be used as event data
     * This method is recursive.
     * The elements for which the conversion fails will be skipped.
     *
     * @param jsonArray to be converted
     * @return {@link List} containing the elements from the provided json, null if {@code jsonArray} is null
     */
    static List<Object> toList(final JSONArray jsonArray) {
        if (jsonArray == null) {
            return null;
        }

        List<Object> jsonArrayAsList = new ArrayList<Object>();
        int size = jsonArray.length();

        for (int i = 0; i < size; i++) {
            Object value = null;
            Object returnValue = null;

            try {
                value = jsonArray.get(i);
            } catch (JSONException e) {
                MobileCore.log(LoggingMode.DEBUG, LOG_TAG,
                        "toList - Unable to convert jsonObject to List for index " + i + ", skipping.");
            }

            if (value == null) {
                continue;
            }

            if (value instanceof JSONObject) {
                returnValue = toMap((JSONObject)value);
            } else if (value instanceof JSONArray) {
                returnValue = toList((JSONArray) value);
            } else {
                returnValue = value;
            }

            jsonArrayAsList.add(returnValue);
        }

        return jsonArrayAsList;
    }

    /**
     * Creates a deep copy of the provided {@link Map}.
     *
     * @param map to be copied
     * @return {@link Map} containing a deep copy of all the elements in {@code map}
     */
    static Map<String, Object> deepCopy(final Map<String, Object> map) {
        if (map == null) {
            return null;
        }

        try {
            return toMap(new JSONObject(map));
        } catch (NullPointerException e) {
            MobileCore.log(LoggingMode.DEBUG, LOG_TAG, "deepCopy - Unable to deep copy map, json string invalid.");
        }

        return null;
    }

    private Utility(){}

}
