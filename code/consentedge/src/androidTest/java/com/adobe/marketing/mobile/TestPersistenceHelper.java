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

package com.adobe.marketing.mobile;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.adobe.marketing.mobile.consent.ConsentTestConstants;

import java.util.ArrayList;

/**
 * Helper class to add and remove persisted data to extension concerned with testing Consents.
 */
public class TestPersistenceHelper {

    private static ArrayList<String> knownDatastoreName = new ArrayList<String>() {{
        add(ConsentTestConstants.DataStoreKey.CONSENT_DATASTORE);
        add(ConsentTestConstants.DataStoreKey.CONFIG_DATASTORE);
    }};

    public static void resetKnownPersistence() {
        final Application application = MobileCore.getApplication();
        if (application == null) {
            return;
        }

        final Context context = application.getApplicationContext();
        if (context == null) {
            return;
        }

        for (String eachDatastore : knownDatastoreName) {
            SharedPreferences sharedPreferences = context.getSharedPreferences(eachDatastore, Context.MODE_PRIVATE);;
            if (sharedPreferences == null) {
                return;
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (editor == null) {
                return;
            }
            editor.clear();
            editor.apply();
        }
    }

}
