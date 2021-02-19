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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


import static com.adobe.marketing.mobile.consent.ConsentTestUtil.CreateConsentDataJSONString;
import static com.adobe.marketing.mobile.consent.ConsentTestUtil.SAMPLE_METADATA_TIMESTAMP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
        final String sharedPreferenceJSON = CreateConsentDataJSONString("y", "n", SAMPLE_METADATA_TIMESTAMP);
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT, null)).thenReturn(sharedPreferenceJSON);

        // test
        consentManager = new ConsentManager();

        // verify
        Consents currentConsents = consentManager.getCurrentConsents();
        Assert.assertEquals(ConsentValue.YES, currentConsents.getCollectConsent());
        Assert.assertEquals(ConsentValue.NO, currentConsents.getAdIdConsent());
        Assert.assertEquals(SAMPLE_METADATA_TIMESTAMP, currentConsents.getMetadata().getTime());
    }

    @Test
    public void test_LoadFromSharedPreference_whenNull() {
        // setup
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT, null)).thenReturn(null);

        // test
        consentManager = new ConsentManager();

        // verify
        Consents currentConsents = consentManager.getCurrentConsents();
        assertNull(currentConsents);
    }

    @Test
    public void test_LoadFromSharedPreference_whenInvalidJSON() {
        // setup
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT, null)).thenReturn("{InvalidJSON}[]$62&23Fsd^%");

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
        final String sharedPreferenceJSON = CreateConsentDataJSONString("y", "n", SAMPLE_METADATA_TIMESTAMP);
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT, null)).thenReturn(sharedPreferenceJSON);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data

        // test
        Consents newConsent = new Consents();
        newConsent.setAdIdConsent(ConsentValue.YES);
        Consents mergedConsent = consentManager.mergeAndPersist(newConsent);

        // verify
        Assert.assertEquals(ConsentValue.YES, mergedConsent.getCollectConsent()); // assert CollectConsent value has not changed on merge
        Assert.assertEquals(ConsentValue.YES, mergedConsent.getAdIdConsent()); // assert adIdConsent value has changed on merge
        Assert.assertEquals(SAMPLE_METADATA_TIMESTAMP, mergedConsent.getMetadata().getTime()); // assert time has not changed on merge

        // verify if correct data is written in shared preference
        verify(mockSharedPreferenceEditor, times(1)).putString(ConsentConstants.DataStoreKey.CONSENT, CreateConsentDataJSONString("y","y", SAMPLE_METADATA_TIMESTAMP));
    }

    @Test
    public void test_MergeAndPersist_nullConsent() {
        // setup currentConsent
        final String sharedPreferenceJSON = CreateConsentDataJSONString("y", "n", SAMPLE_METADATA_TIMESTAMP);
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT, null)).thenReturn(sharedPreferenceJSON);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data

        // test
        Consents mergedConsent = consentManager.mergeAndPersist(null);

        // verify that no value has changed
        Assert.assertEquals(ConsentValue.YES, mergedConsent.getCollectConsent());
        Assert.assertEquals(ConsentValue.NO, mergedConsent.getAdIdConsent());
        Assert.assertEquals(SAMPLE_METADATA_TIMESTAMP, mergedConsent.getMetadata().getTime());

        // verify shared preference is not disturbed
        verify(mockSharedPreferenceEditor, times(0)).putString(anyString(), anyString());
    }

    @Test
    public void test_MergeAndPersist_emptyConsent() {
        // setup currentConsent
        final String sharedPreferenceJSON = CreateConsentDataJSONString("y", "n", SAMPLE_METADATA_TIMESTAMP);
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT, null)).thenReturn(sharedPreferenceJSON);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data

        // test
        Consents mergedConsent = consentManager.mergeAndPersist(new Consents());

        // verify that no value has changed
        Assert.assertEquals(ConsentValue.YES, mergedConsent.getCollectConsent());
        Assert.assertEquals(ConsentValue.NO, mergedConsent.getAdIdConsent());
        Assert.assertEquals(SAMPLE_METADATA_TIMESTAMP, mergedConsent.getMetadata().getTime());

        // verify shared preference is not disturbed
        verify(mockSharedPreferenceEditor, times(0)).putString(anyString(), anyString());
    }


    @Test
    public void test_MergeAndPersist_whenCurrentConsentIsEmpty() {
        // setup currentConsent
        final String sharedPreferenceJSON = CreateConsentDataJSONString("y", "n", SAMPLE_METADATA_TIMESTAMP);
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT, null)).thenReturn(sharedPreferenceJSON);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data

        // test
        Consents mergedConsent = consentManager.mergeAndPersist(new Consents());

        // verify that no value has changed
        Assert.assertEquals(ConsentValue.YES, mergedConsent.getCollectConsent());
        Assert.assertEquals(ConsentValue.NO, mergedConsent.getAdIdConsent());
        Assert.assertEquals(SAMPLE_METADATA_TIMESTAMP, mergedConsent.getMetadata().getTime());

        // verify shared preference is not disturbed
        verify(mockSharedPreferenceEditor, times(0)).putString(anyString(), anyString());
    }

    @Test
    public void test_MergeAndPersist_whenSharePreferenceNull() {
        // setup currentConsent
        final String sharedPreferenceJSON = CreateConsentDataJSONString("y", "n", SAMPLE_METADATA_TIMESTAMP);
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT, null)).thenReturn(sharedPreferenceJSON);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data
        Mockito.when(mockContext.getSharedPreferences(ConsentConstants.DataStoreKey.DATASTORE_NAME, 0)).thenReturn(null);

        // test
        Consents newConsent = new Consents();
        newConsent.setAdIdConsent(ConsentValue.YES);
        Consents mergedConsent = consentManager.mergeAndPersist(newConsent);

        // verify that in-memory variable are still correct
        Assert.assertEquals(ConsentValue.YES, mergedConsent.getCollectConsent()); // assert CollectConsent value has not changed on merge
        Assert.assertEquals(ConsentValue.YES, mergedConsent.getAdIdConsent()); // assert adIdConsent value has changed on merge
        Assert.assertEquals(SAMPLE_METADATA_TIMESTAMP, mergedConsent.getMetadata().getTime()); // assert time has not changed on merge

        // verify shared preference is not disturbed
        verify(mockSharedPreferenceEditor, times(0)).putString(anyString(), anyString());
    }

    @Test
    public void test_MergeAndPersist_whenSharePreferenceEditorNull() {
        // setup currentConsent
        final String sharedPreferenceJSON = CreateConsentDataJSONString("y", "n", SAMPLE_METADATA_TIMESTAMP);
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT, null)).thenReturn(sharedPreferenceJSON);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data
        Mockito.when(mockSharedPreference.edit()).thenReturn(null);

        // test
        Consents newConsent = new Consents();
        newConsent.setAdIdConsent(ConsentValue.YES);
        Consents mergedConsent = consentManager.mergeAndPersist(newConsent);

        // verify that in-memory variable are still correct
        Assert.assertEquals(ConsentValue.YES, mergedConsent.getCollectConsent()); // assert CollectConsent value has not changed on merge
        Assert.assertEquals(ConsentValue.YES, mergedConsent.getAdIdConsent()); // assert adIdConsent value has changed on merge
        Assert.assertEquals(SAMPLE_METADATA_TIMESTAMP, mergedConsent.getMetadata().getTime()); // assert time has not changed on merge
    }

    @Test
    public void test_MergeAndPersist_whenExistingAndNewConsentEmpty() {
        // setup currentConsent to be null
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT, null)).thenReturn(null);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data

        // test
        Consents newConsent = new Consents();
        Consents mergedConsent = consentManager.mergeAndPersist(newConsent);

        // verify that
        assertNull(mergedConsent);
        assertNull(consentManager.getCurrentConsents());
    }

    @Test
    public void test_MergeAndPersist_whenExistingConsentsNull_AndNewConsentValid() {
        // setup currentConsent to be null
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT, null)).thenReturn(null);
        consentManager = new ConsentManager(); // consentManager now loads the persisted data

        // test
        Consents newConsent = new Consents();
        newConsent.setCollectConsent(ConsentValue.YES);
        Consents mergedConsent = consentManager.mergeAndPersist(newConsent);

        // verify
        assertEquals(mergedConsent, consentManager.getCurrentConsents());
        assertEquals(ConsentValue.YES,mergedConsent.getCollectConsent());
        assertNull(mergedConsent.getAdIdConsent());
        assertNull(mergedConsent.getMetadata());
    }

}
