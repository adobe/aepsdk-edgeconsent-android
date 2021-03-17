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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

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

public class ConsentEdgeResponseHandlingTests {

    @Rule
    public RuleChain rule = RuleChain.outerRule(new TestHelper.SetupCoreRule())
            .around(new TestHelper.RegisterMonitorExtensionRule());

    // --------------------------------------------------------------------------------------------
    // Setup
    // --------------------------------------------------------------------------------------------

    @Before
    public void setup() throws Exception {
        Consent.registerExtension();

        final CountDownLatch latch = new CountDownLatch(1);
        MobileCore.start(new AdobeCallback() {
            @Override
            public void call(Object o) {
                latch.countDown();
            }
        });

        latch.await();
        resetTestExpectations();
    }

    @Test
    public void test_EdgeResponse_MergesWithCurrentConsent() throws Exception {

        // test summary
        //-----------------------------------------
        // Type         collect   AdID    Metadata
        //-----------------------------------------
        // Default      pending     NO      null
        // Updated        YES
        // EdgeResponse    NO
        //-------------------------------------------
        // Final           NO      NO       available
        //-------------------------------------------
        // verify in (Persistence, ConsentResponse and XDMSharedState)

        // setup
        applyDefaultConsent(CreateConsentXDMMap("p", "n"));
        Consent.update(CreateConsentXDMMap("y"));
        waitForThreads(1000);
        resetTestExpectations();

        MobileCore.dispatchEvent(buildEdgeConsentPreferenceEventWithConsents(CreateConsentXDMMap("n")), null); // edge response sets the collect consent to no
        waitForThreads(1000);

        // verify consent response event dispatched
        List<Event> consentResponseEvents = getDispatchedEventsWith(ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.RESPONSE_CONTENT);
        assertEquals(1, consentResponseEvents.size());
        Map<String, String> consentResponseData = flattenMap(consentResponseEvents.get(0).getEventData());
        assertEquals(3, consentResponseData.size());
        assertEquals("n", consentResponseData.get("consents.collect.val"));
        assertEquals("n", consentResponseData.get("consents.adID.val"));
        assertNotNull(consentResponseData.get("consents.metadata.time"));

        // verify xdm shared state
        Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
        assertEquals(3, xdmSharedState.size());
        assertEquals("n", xdmSharedState.get("consents.collect.val"));
        assertEquals("n", xdmSharedState.get("consents.adID.val"));
        assertNotNull(xdmSharedState.get("consents.metadata.time"));

        // verify persisted data - default consents are not persisted
        final String persistedJson = TestPersistenceHelper.readPersistedData(ConsentConstants.DataStoreKey.DATASTORE_NAME, ConsentConstants.DataStoreKey.CONSENT_PREFERENCES);
        Map<String,Object> persistedMap = Utility.toMap(new JSONObject(persistedJson));
        Map<String, String> flattenPersistedMap = flattenMap(persistedMap);
        assertEquals(2, flattenPersistedMap.size());
        assertEquals("n", flattenPersistedMap.get("consents.collect.val"));
        assertNotNull(xdmSharedState.get("consents.metadata.time"));
    }

    @Test
    public void test_EdgeResponse_InvalidPayload() throws Exception {
        // test summary
        //-----------------------------------------
        // Type         collect   AdID    Metadata
        //-----------------------------------------
        // Default      pending
        // Updated        YES
        // EdgeResponse  invalid
        //-------------------------------------------
        // Final           YES            available
        //-------------------------------------------

        // setup
        applyDefaultConsent(CreateConsentXDMMap("p"));
        Consent.update(CreateConsentXDMMap("y"));
        waitForThreads(1000);
        resetTestExpectations();

        // test
        MobileCore.dispatchEvent(buildEdgeConsentPreferenceEvent("{\n" +
                "  \"payload\" : \"not what I expect\"\n" +
                "}"), null);
        waitForThreads(1000);

        // verify xdm shared state
        Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
        assertEquals(2, xdmSharedState.size());
        assertEquals("y", xdmSharedState.get("consents.collect.val"));
        assertNotNull(xdmSharedState.get("consents.metadata.time"));

        // verify persisted data
        final String persistedJson = TestPersistenceHelper.readPersistedData(ConsentConstants.DataStoreKey.DATASTORE_NAME, ConsentConstants.DataStoreKey.CONSENT_PREFERENCES);
        Map<String,Object> persistedMap = Utility.toMap(new JSONObject(persistedJson));
        Map<String, String> flattenPersistedMap = flattenMap(persistedMap);
        assertEquals(2, flattenPersistedMap.size());
        assertEquals("y", flattenPersistedMap.get("consents.collect.val"));
        assertNotNull(xdmSharedState.get("consents.metadata.time"));
    }

}
