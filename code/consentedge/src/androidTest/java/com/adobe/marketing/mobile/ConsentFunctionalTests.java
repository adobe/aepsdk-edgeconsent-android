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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import static com.adobe.marketing.mobile.FunctionalTestHelper.resetTestExpectations;

@RunWith(AndroidJUnit4.class)
public class ConsentFunctionalTests {

    @Rule
    public RuleChain rule = RuleChain.outerRule(new FunctionalTestHelper.LogOnErrorRule())
            .around(new FunctionalTestHelper.SetupCoreRule())
            .around(new FunctionalTestHelper.RegisterMonitorExtensionRule());

    // --------------------------------------------------------------------------------------------
    // Setup
    // --------------------------------------------------------------------------------------------

    @Before
    public void setup() throws Exception {
        HashMap<String, Object> config = new HashMap<String, Object>() {
            {
                put("global.privacy", "optedin");
                put("experienceCloud.org", "testOrg@AdobeOrg");
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


    // --------------------------------------------------------------------------------------------
    // Tests for ConsentUpdateEvent
    // --------------------------------------------------------------------------------------------
    @Test
    public void testConsentUpdateHappy() throws InterruptedException {
        // test
        MobileCore.dispatchEvent(buildConsentUpdateEvent(), null);

        // verify
        // coming soon
    }

    // --------------------------------------------------------------------------------------------
    // private methods
    // --------------------------------------------------------------------------------------------

    private Event buildConsentUpdateEvent() {
        Event event =  new Event.Builder("Consent Response",EventType.CONSENT, EventSource.UPDATE_CONSENT).setEventData(new HashMap<String, Object>() {
            {
                put("consents", new HashMap<String, Object>() {
                    {
                        put("adId", new HashMap<String, String>() {
                            {
                                put("val", "y");
                            }
                        });
                        put("collect", new HashMap<String, String>() {
                            {
                                put("val", "y");
                            }
                        });
                    }
                });
            }
        }).build();
        return event;
    }


}
