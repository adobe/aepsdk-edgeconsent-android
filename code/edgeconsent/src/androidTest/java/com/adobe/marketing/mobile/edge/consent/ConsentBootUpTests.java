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

package com.adobe.marketing.mobile.edge.consent;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.TestHelper;
import com.adobe.marketing.mobile.TestPersistenceHelper;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.adobe.marketing.mobile.TestHelper.getDispatchedEventsWith;
import static com.adobe.marketing.mobile.TestHelper.getXDMSharedStateFor;
import static com.adobe.marketing.mobile.TestHelper.resetTestExpectations;
import static com.adobe.marketing.mobile.TestHelper.waitForThreads;
import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class ConsentBootUpTests {

    @Rule
    public RuleChain rule = RuleChain.outerRule(new TestHelper.SetupCoreRule())
            .around(new TestHelper.RegisterMonitorExtensionRule());

    @Test
    public void test_BootUp_loadsFromPersistence() throws Exception {
        // test summary
        //------------------------------------------------------------
        // Type         collect   AdID    personalize    metadata
        //------------------------------------------------------------
        // Persistence  pending    no        vi         available
        //
        //-------------------------------------------------------------
        // Final        pending    no        vi         available
        //-------------------------------------------------------------
        // verify in (ConsentResponse and XDMSharedState)

        // test
        initExtensionWithPersistedDataAndDefaults(CreateConsentXDMMap("p", "n", "vi", SAMPLE_METADATA_TIMESTAMP), null);
        waitForThreads(2000);

        // verify consent response event dispatched
        List<Event> consentResponseEvents = getDispatchedEventsWith(ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.RESPONSE_CONTENT);
        assertEquals(1, consentResponseEvents.size());
        Map<String, String> consentResponseData = flattenMap(consentResponseEvents.get(0).getEventData());
        assertEquals(4, consentResponseData.size());
        assertEquals("p", consentResponseData.get("consents.collect.val"));
        assertEquals("n", consentResponseData.get("consents.adID.val"));
        assertEquals("vi", consentResponseData.get("consents.personalize.content.val"));
        assertEquals(SAMPLE_METADATA_TIMESTAMP, consentResponseData.get("consents.metadata.time"));


        //  verify getConsent API
        Map<String, Object> getConsentResponse = getConsentsSync();
        Map<String, String> responseMap = flattenMap((Map) getConsentResponse.get(ConsentTestConstants.GetConsentHelper.VALUE));
        assertEquals("p", responseMap.get("consents.collect.val"));
        assertEquals("n", responseMap.get("consents.adID.val"));
        assertEquals("vi", responseMap.get("consents.personalize.content.val"));
        assertEquals(SAMPLE_METADATA_TIMESTAMP, responseMap.get("consents.metadata.time"));

        // verify xdm shared state
        Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
        assertEquals(4, xdmSharedState.size());
        assertEquals("p", xdmSharedState.get("consents.collect.val"));
        assertEquals("n", xdmSharedState.get("consents.adID.val"));
        assertEquals("vi", xdmSharedState.get("consents.personalize.content.val"));
        assertEquals(SAMPLE_METADATA_TIMESTAMP, xdmSharedState.get("consents.metadata.time"));
    }

    @Test
    public void test_BootUp_noPersistedData_withConfigDefault() throws Exception {
        // test summary
        //--------------------------------------------
        // Type         collect   AdID   metadata
        //--------------------------------------------
        // Persistence     -
        // Default         y
        //--------------------------------------------
        // Final           y       -       -
        //--------------------------------------------
        // verify in (ConsentResponse and XDMSharedState and GetConsent API)

        // test
        initExtensionWithPersistedDataAndDefaults(null, CreateConsentXDMMap("y"));
        waitForThreads(2000);

        // verify consent response event dispatched
        List<Event> consentResponseEvents = getDispatchedEventsWith(ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.RESPONSE_CONTENT);
        assertEquals(1, consentResponseEvents.size());
        Map<String, String> consentResponseData = flattenMap(consentResponseEvents.get(0).getEventData());
        assertEquals(1, consentResponseData.size());
        assertEquals("y", consentResponseData.get("consents.collect.val"));


        //  verify getConsent API
        Map<String, Object> getConsentResponse = getConsentsSync();
        Map<String, String> responseMap = flattenMap((Map) getConsentResponse.get(ConsentTestConstants.GetConsentHelper.VALUE));
        assertEquals("y", responseMap.get("consents.collect.val"));

        // verify xdm shared state //
        Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
        assertEquals(1, xdmSharedState.size());
        assertEquals("y", xdmSharedState.get("consents.collect.val"));
    }


    @Test
    public void test_BootUp_withPersistedData_withConfigDefault() throws Exception {
        // test summary
        //--------------------------------------------
        // Type         collect   AdID   metadata
        //--------------------------------------------
        // Persistence     n
        // Default         y
        //--------------------------------------------
        // Final           n       -       -
        //--------------------------------------------
        // verify in (ConsentResponse and XDMSharedState and GetConsent API)

        // setup and test
        initExtensionWithPersistedDataAndDefaults(CreateConsentXDMMap("n"), CreateConsentXDMMap("y"));
        waitForThreads(2000);

        // verify consent response event dispatched
        List<Event> consentResponseEvents = getDispatchedEventsWith(ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.RESPONSE_CONTENT);
        assertEquals(1, consentResponseEvents.size());
        Map<String, String> consentResponseData = flattenMap(consentResponseEvents.get(0).getEventData());
        assertEquals(1, consentResponseData.size());
        assertEquals("n", consentResponseData.get("consents.collect.val"));

        //  verify getConsent API
        Map<String, Object> getConsentResponse = getConsentsSync();
        Map<String, String> responseMap = flattenMap((Map) getConsentResponse.get(ConsentTestConstants.GetConsentHelper.VALUE));
        assertEquals("n", responseMap.get("consents.collect.val"));

        // verify xdm shared state //
        Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
        assertEquals(1, xdmSharedState.size());
        assertEquals("n", xdmSharedState.get("consents.collect.val"));
    }

    @Test
    public void test_BootUp_CompleteWorkflow() throws Exception {
        // test summary
        //----------------------------------------------------------
        // Type         collect   AdID   personalize    metadata
        //----------------------------------------------------------
        // Persistence     y
        // Default         y       y
        // Update          n
        // EdgeResponse    n                vi        available
        // ChangeDefault   y       n
        //----------------------------------------------------------
        // Final           n       n        vi        available
        //----------------------------------------------------------
        // verify in (ConsentResponse and XDMSharedState and GetConsent API and persistence)

        // setup and test
        initExtensionWithPersistedDataAndDefaults(CreateConsentXDMMap("y"), CreateConsentXDMMap("y", "y"));
        Consent.update(CreateConsentXDMMap("n"));
        MobileCore.dispatchEvent(buildEdgeConsentPreferenceEventWithConsents(CreateConsentXDMMap("n",null, "vi", SAMPLE_METADATA_TIMESTAMP)),null);
        HashMap<String, Object> config = new HashMap<String, Object>() {
            {
                put(ConsentConstants.ConfigurationKey.DEFAULT_CONSENT, CreateConsentXDMMap("y","n"));
            }
        };
        waitForThreads(2000);
        resetTestExpectations(); // reset here so we only assert on the last set of events
        MobileCore.updateConfiguration(config);
        waitForThreads(2000);

        // verify consent response event dispatched
        List<Event> consentResponseEvents = getDispatchedEventsWith(ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.RESPONSE_CONTENT);
        assertEquals(1, consentResponseEvents.size());
        Map<String, String> consentResponseData = flattenMap(consentResponseEvents.get(0).getEventData());
        assertEquals(4, consentResponseData.size());
        assertEquals("n", consentResponseData.get("consents.collect.val"));
        assertEquals("n", consentResponseData.get("consents.adID.val"));
        assertEquals("vi", consentResponseData.get("consents.personalize.content.val"));
        assertNotNull(consentResponseData.get("consents.metadata.time"));

        //  verify getConsent API
        Map<String, Object> getConsentResponse = getConsentsSync();
        Map<String, String> responseMap = flattenMap((Map) getConsentResponse.get(ConsentTestConstants.GetConsentHelper.VALUE));
        assertEquals("n", responseMap.get("consents.collect.val"));
        assertEquals("n", responseMap.get("consents.adID.val"));
        assertEquals("vi", responseMap.get("consents.personalize.content.val"));
        assertNotNull(consentResponseData.get("consents.metadata.time"));

        // verify xdm shared state
        Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
        assertEquals(4, xdmSharedState.size());
        assertEquals("n", xdmSharedState.get("consents.collect.val"));
        assertEquals("n", xdmSharedState.get("consents.adID.val"));
        assertEquals("vi", xdmSharedState.get("consents.personalize.content.val"));
        assertNotNull(xdmSharedState.get("consents.metadata.time"));
    }

    // --------------------------------------------------------------------------------------------
    // private helper methods
    // --------------------------------------------------------------------------------------------

    private void initExtensionWithPersistedDataAndDefaults(final Map<String, Object> persistedData, final Map<String, Object> defaultConsentMap) throws InterruptedException {
        if (persistedData != null) {
            final JSONObject persistedJSON = new JSONObject(persistedData);
            TestPersistenceHelper.updatePersistence(ConsentConstants.DataStoreKey.DATASTORE_NAME, ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, persistedJSON.toString());
        }

        if (defaultConsentMap != null) {
            HashMap<String, Object> config = new HashMap<String, Object>() {
                {
                    put(ConsentConstants.ConfigurationKey.DEFAULT_CONSENT, defaultConsentMap);
                }
            };
            MobileCore.updateConfiguration(config);
        }

        Consent.registerExtension();
        final CountDownLatch latch = new CountDownLatch(1);
        MobileCore.start(new AdobeCallback() {
            @Override
            public void call(Object o) {
                latch.countDown();
            }
        });

        latch.await();
    }

}
