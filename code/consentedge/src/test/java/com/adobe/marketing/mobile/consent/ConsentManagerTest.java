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

import com.adobe.marketing.mobile.MobileCore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


import java.util.HashMap;

import static com.adobe.marketing.mobile.consent.ConsentTestUtil.CreateConsentXDMMap;
import static com.adobe.marketing.mobile.consent.ConsentTestUtil.CreateConsentsXDMJSONString;
import static com.adobe.marketing.mobile.consent.ConsentTestUtil.SAMPLE_METADATA_TIMESTAMP;
import static com.adobe.marketing.mobile.consent.ConsentTestUtil.SAMPLE_METADATA_TIMESTAMP_OTHER;
import static com.adobe.marketing.mobile.consent.ConsentTestUtil.readAdIdConsent;
import static com.adobe.marketing.mobile.consent.ConsentTestUtil.readPersonalizeConsent;
import static com.adobe.marketing.mobile.consent.ConsentTestUtil.readCollectConsent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class})
public class ConsentManagerTest {

    @Mock
    Context mockContext;

    @Mock
    SharedPreferences mockSharedPreference;

    @Mock
    SharedPreferences.Editor mockSharedPreferenceEditor;

    @Mock
    Application mockApplication;

    private ConsentManager consentManager;

    // ========================================================================================
    // Test Scenario    : consentManager load consents from persistence on boot
    // Test method      : constructor, loadFromPreference , getCurrentConsents
    // ========================================================================================

    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(MobileCore.class);

        Mockito.when(MobileCore.getApplication()).thenReturn(mockApplication);
        Mockito.when(mockApplication.getApplicationContext()).thenReturn(mockContext);
        Mockito.when(mockContext.getSharedPreferences(ConsentConstants.DataStoreKey.DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);
    }

    @Test
    public void test_Constructor_LoadsFromSharedPreference() {
        // setup
        final String sharedPreferenceJSON = CreateConsentsXDMJSONString("y", "n", SAMPLE_METADATA_TIMESTAMP);
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null)).thenReturn(sharedPreferenceJSON);

        // test
        consentManager = new ConsentManager();

        // verify
        Consents currentConsents = consentManager.getCurrentConsents();
        assertEquals("y", readCollectConsent(currentConsents));
        assertEquals("n", readAdIdConsent(currentConsents));
        assertNull(ConsentTestUtil.readPersonalizeConsent(currentConsents));
        assertEquals(SAMPLE_METADATA_TIMESTAMP,ConsentTestUtil.readTimestamp(currentConsents));
    }

    @Test
    public void test_LoadFromSharedPreference_whenNull() {
        // setup
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null)).thenReturn(null);

        // test
        consentManager = new ConsentManager();

        // verify
        Consents currentConsents = consentManager.getCurrentConsents();
        assertNull(currentConsents);
    }

    @Test
    public void test_LoadFromSharedPreference_whenInvalidJSON() {
        // setup
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null)).thenReturn("{InvalidJSON}[]$62&23Fsd^%");

        // test
        consentManager = new ConsentManager();

        // verify
        Consents currentConsents = consentManager.getCurrentConsents();
        assertNull(currentConsents);
    }

    @Test
    public void test_LoadFromSharedPreference_whenSharedPreferenceNull() {
        // setup
        Mockito.when(mockContext.getSharedPreferences(ConsentConstants.DataStoreKey.DATASTORE_NAME, 0)).thenReturn(null);

        // test
        consentManager = new ConsentManager();

        // verify
        Consents currentConsents = consentManager.getCurrentConsents();
        assertNull(currentConsents);
    }

    @Test
    public void test_LoadFromSharedPreference_whenApplicationNull() {
        // setup
        Mockito.when(MobileCore.getApplication()).thenReturn(null);

        // test
        consentManager = new ConsentManager();

        // verify
        Consents currentConsents = consentManager.getCurrentConsents();
        assertNull(currentConsents);
    }

    @Test
    public void test_LoadFromSharedPreference_whenContextNull() {
        // setup
        Mockito.when(mockApplication.getApplicationContext()).thenReturn(null);

        // test
        consentManager = new ConsentManager();

        // verify
        Consents currentConsents = consentManager.getCurrentConsents();
        assertNull(currentConsents);
    }

    // ========================================================================================
    // Test Scenario    : consentManager ability to merge with current consent and persist
    // Test method      : mergeAndPersist, saveToPreference
    // ========================================================================================

    @Test
    public void test_MergeAndPersist() {
        // setup currentConsent
        final String sharedPreferenceJSON = CreateConsentsXDMJSONString("y", "n", "vi", SAMPLE_METADATA_TIMESTAMP);
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null)).thenReturn(sharedPreferenceJSON);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data

        // test
        Consents newConsent = new Consents(CreateConsentXDMMap("n", null, "pi", SAMPLE_METADATA_TIMESTAMP_OTHER));
        consentManager.mergeAndPersist(newConsent);
        Consents mergedConsent = consentManager.getCurrentConsents();

                // verify
        assertEquals("n", readCollectConsent(mergedConsent)); // assert CollectConsent value has changed on merge
        assertEquals("n", readAdIdConsent(mergedConsent)); // assert adIdConsent value has not changed on merge
        assertEquals("pi", readPersonalizeConsent(mergedConsent)); // assert PersonalizeConsent value has changed on merge
        assertEquals(SAMPLE_METADATA_TIMESTAMP_OTHER, ConsentTestUtil.readTimestamp(mergedConsent)); // assert time has changed on merge

        // verify if correct data is written in shared preference
        verify(mockSharedPreferenceEditor, times(1)).putString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, CreateConsentsXDMJSONString("n", "n", "pi", SAMPLE_METADATA_TIMESTAMP_OTHER));
        
    }

    @Test
    public void test_MergeAndPersist_nullConsent() {
        // setup currentConsent
        final String sharedPreferenceJSON = CreateConsentsXDMJSONString("y", "n", SAMPLE_METADATA_TIMESTAMP);
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null)).thenReturn(sharedPreferenceJSON);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data

        // test
        consentManager.mergeAndPersist(null);
        Consents mergedConsent = consentManager.getCurrentConsents();

        // verify that no value has changed
        assertEquals("y", readCollectConsent(mergedConsent)); // assert CollectConsent value has not changed on merge
        assertEquals("n", readAdIdConsent(mergedConsent)); // assert adIdConsent value has not changed on merge
        assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(mergedConsent)); // assert time has not changed on merge

        // verify shared preference is correct
        verify(mockSharedPreferenceEditor, times(1)).putString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, CreateConsentsXDMJSONString("y", "n", SAMPLE_METADATA_TIMESTAMP));
    }

    @Test
    public void test_MergeAndPersist_emptyConsent() {
        // setup currentConsent
        final String sharedPreferenceJSON = CreateConsentsXDMJSONString("y", "n", SAMPLE_METADATA_TIMESTAMP);
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null)).thenReturn(sharedPreferenceJSON);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data

        // test
        consentManager.mergeAndPersist(new Consents(new HashMap<String, Object>()));
        Consents mergedConsent = consentManager.getCurrentConsents();

                // verify that no value has changed
        assertEquals("y", readCollectConsent(mergedConsent)); // assert CollectConsent value has not changed on merge
        assertEquals("n", readAdIdConsent(mergedConsent)); // assert adIdConsent value has not changed on merge
        assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(mergedConsent)); // assert time has not changed on merge

        // verify shared preference is correct
        verify(mockSharedPreferenceEditor, times(1)).putString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, CreateConsentsXDMJSONString("y", "n", SAMPLE_METADATA_TIMESTAMP));
    }

    @Test
    public void test_MergeAndPersist_whenExistingConsentsNull_AndNewConsentValid() {
        // setup currentConsent
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null)).thenReturn(null);
        consentManager = new ConsentManager(); // consentManager now loads nothing from persisted data

        // test
        Consents newConsent = new Consents(CreateConsentXDMMap("n"));
        consentManager.mergeAndPersist(newConsent);
        Consents mergedConsent = consentManager.getCurrentConsents();

        // verify that no value has changed
        assertEquals("n", readCollectConsent(mergedConsent)); // assert CollectConsent value has not changed on merge
        assertNull(readAdIdConsent(mergedConsent)); // assert adID consent is null
        assertNull(ConsentTestUtil.readTimestamp(mergedConsent)); // assert timestamp is null

        // verify shared preference is not disturbed
        verify(mockSharedPreferenceEditor, times(1)).putString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, CreateConsentsXDMJSONString("n", null));
    }

    @Test
    public void test_MergeAndPersist_whenSharedPreferenceNull() {
        // setup currentConsent
        final String sharedPreferenceJSON = CreateConsentsXDMJSONString("y");
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null)).thenReturn(sharedPreferenceJSON);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data
        Mockito.when(mockContext.getSharedPreferences(ConsentConstants.DataStoreKey.DATASTORE_NAME, 0)).thenReturn(null);

        // test
        Consents newConsent = new Consents(CreateConsentXDMMap("n"));
        consentManager.mergeAndPersist(newConsent);
        Consents mergedConsent = consentManager.getCurrentConsents();


        // verify that in-memory variable are still correct
        assertEquals("n", readCollectConsent(mergedConsent)); // assert CollectConsent value is merged

        // verify shared preference is not disturbed
        verify(mockSharedPreferenceEditor, times(0)).putString(anyString(), anyString());
    }

    @Test
    public void test_MergeAndPersist_whenSharedPreferenceEditorNull() {
        // setup currentConsent
        final String sharedPreferenceJSON = CreateConsentsXDMJSONString("y");
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null)).thenReturn(sharedPreferenceJSON);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data
        Mockito.when(mockSharedPreference.edit()).thenReturn(null);

        // test
        Consents newConsent = new Consents(CreateConsentXDMMap("n"));
        consentManager.mergeAndPersist(newConsent);
        Consents mergedConsent = consentManager.getCurrentConsents();

        // verify that in-memory variable are still correct
        assertEquals("n", readCollectConsent(mergedConsent)); // assert CollectConsent value is merged

        // verify shared preference is not disturbed
        verify(mockSharedPreferenceEditor, times(0)).putString(anyString(), anyString());
    }

    @Test
    public void test_MergeAndPersist_whenExistingAndNewConsentEmpty() {
        // setup currentConsent to be null
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null)).thenReturn(null);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data

        // test
        Consents newConsent = new Consents(new HashMap<String, Object>());
        consentManager.mergeAndPersist(newConsent);
        Consents mergedConsent = consentManager.getCurrentConsents();

        // verify
        assertTrue(mergedConsent.isEmpty());
        assertTrue(consentManager.getCurrentConsents().isEmpty());

        // verify that consents is removed from shared preference
        verify(mockSharedPreferenceEditor, times(   1)).remove(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES);
    }

}
