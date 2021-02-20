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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class ConsentJSONUtility {

    /**
     * Method to serialize jsonObject to Map.
     *
     * @param jsonObject the {@link JSONObject} to be serialized
     * @return a {@link Map} representing the serialized JSONObject
     */
    static Map<String, Object> serializeJSONObject(final JSONObject jsonObject) throws JSONException {
        if (jsonObject == null) {
            return null;
        }

        final Map<String, Object> serializedMap = new HashMap<>();
        final Iterator<String> keysIterator = jsonObject.keys();

        while (keysIterator.hasNext()) {
            final String key = keysIterator.next();

            if (key == null) {
                continue; // skip null keys
            }

            Object entryValue;
            entryValue = jsonObject.get(key);

            final Object serializedEntryValue = serializeObject(entryValue);
            serializedMap.put(key, serializedEntryValue);
        }

        return serializedMap;
    }

    private static Object serializeObject(final Object jsonValueAsObject) throws JSONException {

        if (jsonValueAsObject == null) {
            return null;
        } else if (jsonValueAsObject instanceof JSONObject) {
            final JSONObject jsonValueAsJsonObject = (JSONObject) jsonValueAsObject;
            return serializeJSONObject(jsonValueAsJsonObject);
        } else if (jsonValueAsObject instanceof JSONArray) {
            final JSONArray jsonValueAsJsonArray = (JSONArray) jsonValueAsObject;
            return serializeArray(jsonValueAsJsonArray);
        } else {
            return jsonValueAsObject;
        }
    }

    private static Object serializeArray(final JSONArray jsonValueAsJsonArray) throws JSONException {
        if (jsonValueAsJsonArray == null) {
            return null;
        }

        final List<Object> serializedArray = new ArrayList<>();

        for (int i = 0, c = jsonValueAsJsonArray.length(); i < c; ++i) {
            Object element;
            element = jsonValueAsJsonArray.get(i);

            final Object serializedElement = serializeObject(element);
            serializedArray.add(serializedElement);
        }

        return serializedArray;
    }

    private ConsentJSONUtility(){}

}
