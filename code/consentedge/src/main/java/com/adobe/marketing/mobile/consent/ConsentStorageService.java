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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.adobe.marketing.mobile.LoggingMode;
import com.adobe.marketing.mobile.MobileCore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

class ConsentStorageService {

    /**
     * Loads the consents from persistence.
     * <p>
     * The jsonString from persistence is serialized into {@link Consents} object and returned.
     * <p>
     * Returns null, if loading from persistence fails because {@link SharedPreferences} or {@link SharedPreferences.Editor} is null.
     * Returns null, if there was any {@link JSONException} while serializing JSONString to {@code Consents} object.
     *
     * @return {@link Consent} the previously persisted consents
     */
    static Consents loadConsentsFromPersistence() {
        final SharedPreferences sharedPreferences = getSharedPreference();
        if (sharedPreferences == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Shared Preference value is null. Unable to load saved consents from persistence.");
            return null;
        }


        final String jsonString = sharedPreferences.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null);

        if (jsonString == null) {
            MobileCore.log(LoggingMode.VERBOSE, ConsentConstants.LOG_TAG, "No previous consents were stored in persistence. Current consent is null");
            return null;
        }

        try {
            final JSONObject jsonObject = new JSONObject(jsonString);
            final Map<String, Object> consentMap = Utility.toMap(jsonObject);
            final Consents loadedConsents = new Consents(consentMap);
            return loadedConsents;

        } catch (JSONException exception) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Serialization error while reading consent jsonString from persistence. Unable to load saved consents from persistence.");
            return null;
        }
    }


    /**
     * Call this method to save the consents to persistence.
     * <p>
     * The consents are converted to jsonString and stored in the persistence.
     * Saving to persistence fails if {@link SharedPreferences} or {@link SharedPreferences.Editor} is null.
     *
     * @param consents the consents that needs to be persisted under key {@link ConsentConstants.DataStoreKey#CONSENT_PREFERENCES}
     */
    static void saveConsentsToPersistence(final Consents consents) {
        SharedPreferences sharedPreferences = getSharedPreference();
        if (sharedPreferences == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Shared Preference value is null. Unable to write consents to persistence.");
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (editor == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Shared Preference Editor is null. Unable to write consents to persistence.");
            return;
        }

        if (consents.isEmpty()) {
            editor.remove(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES);
            editor.apply();
            return;
        }

        final JSONObject jsonObject = new JSONObject(consents.asXDMMap());
        final String jsonString = jsonObject.toString();
        editor.putString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, jsonString);
        editor.apply();
    }

    /**
     * Getter for the applications {@link SharedPreferences}
     * <p>
     * Returns null if the app or app context is not available
     *
     * @return a {@code SharedPreferences} instance
     */
    private static SharedPreferences getSharedPreference() {
        final Application application = MobileCore.getApplication();
        if (application == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Application value is null. Unable to read/write consent data from persistence.");
            return null;
        }

        final Context context = application.getApplicationContext();
        if (context == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Context value is null. Unable to read/write consent data from persistence.");
            return null;
        }

        return context.getSharedPreferences(ConsentConstants.DataStoreKey.DATASTORE_NAME, Context.MODE_PRIVATE);
    }
}
