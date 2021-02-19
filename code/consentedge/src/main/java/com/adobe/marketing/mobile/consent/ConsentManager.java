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

class ConsentManager {

    private Consents currentConsents;

    ConsentManager() {
        loadFromPreference();
    }

    /**
     * Call this method to merge the {@link #currentConsents} maintained by the {@link ConsentManager} provided Consent.
     * <p>
     *
     * @return {}
     */
    Consents mergeAndPersist(final Consents newConsents) {
        // if new consents is null or empty
        if (newConsents == null || newConsents.isEmpty()) {
            return currentConsents;
        }

        // merge and persist
        if (currentConsents == null) {
            currentConsents = new Consents(newConsents);
        } else {
            currentConsents.merge(newConsents);
        }
        saveToPreference();
        return currentConsents;
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

    /**
     * Call this method to save the current consent values to sharedPreferences.
     * <p>
     * The value of {@link #currentConsents} is converted to jsonString and stored in the SharedPreference.
     * Saving to preferences fails if {@link SharedPreferences} or {@link SharedPreferences.Editor} is null.
     */
    private void saveToPreference() {
        if (currentConsents.isEmpty()) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Consents is empty. Ignoring to save to persistence");
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreference();
        if (sharedPreferences == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Shared Preference value is null. Unable to read/write consent data from Shared Preference.");
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (editor == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Shared Preference Editor is null. Unable to read/write consent data from Shared Preference.");
            return;
        }

        final JSONObject jsonObject = new JSONObject(currentConsents.asMap());
        final String jsonString = jsonObject.toString();
        editor.putString(ConsentConstants.DataStoreKey.CONSENT, jsonString);
        editor.apply();
    }

    /**
     * Loads the consent details from sharedPreferences.
     * <p>
     * The jsonString from the sharedPreference is serialized and loaded into {@link #currentConsents}.
     * Loading from preferences fails if {@link SharedPreferences} or {@link SharedPreferences.Editor} is null.
     */
    private void loadFromPreference() {
        final SharedPreferences sharedPreferences = getSharedPreference();
        if (sharedPreferences == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Shared Preference value is null. Unable to read/write consent data from Shared Preference.");
            return;
        }

        final String jsonString = sharedPreferences.getString(ConsentConstants.DataStoreKey.CONSENT, null);

        if (jsonString == null) {
            MobileCore.log(LoggingMode.VERBOSE, ConsentConstants.LOG_TAG, "No previous consents were store in preference. Current consent is null");
            return;
        }

        try {
            final JSONObject jsonObject = new JSONObject(jsonString);
            final Map<String, Object> consentMap = ConsentJSONUtility.serializeJSONObject(jsonObject);
            final Consents loadedConsents = new Consents(consentMap);
            if (!loadedConsents.isEmpty()) {
                currentConsents = loadedConsents;
            }

        } catch (JSONException exception) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Serialization error while reading consent data from Shared preference. Unable to load current shared preference value.");
        }
    }

    /**
     * Getter for the applications {@link SharedPreferences}
     * <p>
     * Returns null if the app or app context is not available
     *
     * @return a {@code SharedPreferences} instance
     */
    private SharedPreferences getSharedPreference() {
        final Application application = MobileCore.getApplication();
        if (application == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Application value is null. Unable to read/write consent data from Shared Preference.");
            return null;
        }

        final Context context = application.getApplicationContext();
        if (context == null) {
            MobileCore.log(LoggingMode.DEBUG, ConsentConstants.LOG_TAG, "Context value is null. Unable to read/write consent data from Shared Preference.");
            return null;
        }

        return context.getSharedPreferences(ConsentConstants.DataStoreKey.DATASTORE_NAME, Context.MODE_PRIVATE);
    }


}
