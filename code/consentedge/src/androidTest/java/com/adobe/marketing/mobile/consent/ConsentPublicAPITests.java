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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.FunctionalTestConstants;
import com.adobe.marketing.mobile.FunctionalTestHelper;
import com.adobe.marketing.mobile.MobileCore;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.adobe.marketing.mobile.FunctionalTestHelper.getDispatchedEventsWith;
import static com.adobe.marketing.mobile.FunctionalTestHelper.getXDMSharedStateFor;
import static com.adobe.marketing.mobile.FunctionalTestHelper.resetConsentPersistence;
import static com.adobe.marketing.mobile.FunctionalTestHelper.resetTestExpectations;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class ConsentPublicAPITests {

    @Rule
    public RuleChain rule = RuleChain.outerRule(new FunctionalTestHelper.SetupCoreRule())
            .around(new FunctionalTestHelper.RegisterMonitorExtensionRule());

    // --------------------------------------------------------------------------------------------
    // Setup
    // --------------------------------------------------------------------------------------------

    @Before
    public void setup() throws Exception {
        HashMap<String, Object> config = new HashMap<String, Object>() {
            {
                put("consents", "optedin");
            }
        };
        MobileCore.updateConfiguration(config);
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

    @After
    public void tearDown() {
        resetConsentPersistence();
    }


    // --------------------------------------------------------------------------------------------
    // Tests for ConsentUpdateEvent
    // --------------------------------------------------------------------------------------------
    @Test
    public void testConsentUpdateHappy() throws InterruptedException {
        // test
        Map<String, Object> collectConsent = new HashMap<String, Object>();
        collectConsent.put("collect", new HashMap<String, String>() {
            {
                put(ConsentConstants.EventDataKey.VALUE, "y");
            }
        });

        Map<String, Object> consents = new HashMap<String, Object>();
        consents.put(ConsentConstants.EventDataKey.CONSENTS, collectConsent);
        Consent.update(consents);

        // verify
        List<Event> dispatchEvents = getDispatchedEventsWith(ConsentConstants.EventType.EDGE,
                ConsentConstants.EventSource.UPDATE_CONSENT);
        assertEquals(1, dispatchEvents.size());

        // verify shared state

        Map<String,Object> sharedState = getXDMSharedStateFor(FunctionalTestConstants.CONSENT_EXTENSION_NAME, 2000);
        assertNotNull(sharedState);

    }


}