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
import static org.junit.Assert.assertNull;

public class ConsentDefaultsTests {

	@Rule
	public RuleChain rule = RuleChain.outerRule(new TestHelper.SetupCoreRule())
							.around(new TestHelper.RegisterMonitorExtensionRule());

	@Test
	public void test_ConsentExtension_UsesDefaultConsent() throws Exception {
		// test summary
		//-----------------------------------------
		// Type         collect   AdID    Metadata
		//-----------------------------------------
		// Default        yes      -        -
		//-------------------------------------------
		// Final          yes      -       -
		//-------------------------------------------
		// verify in (ConsentResponse and XDMSharedState)

		// setup
		initWithDefaultConsent(CreateConsentXDMMap("y"));
		waitForThreads(1000);

		// verify consent response event dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(ConsentConstants.EventType.CONSENT,
											ConsentConstants.EventSource.RESPONSE_CONTENT);
		assertEquals(1, consentResponseEvents.size());
		Map<String, String> consentResponseData = flattenMap(consentResponseEvents.get(0).getEventData());
		assertEquals(1, consentResponseData.size()); // verify that only collect consent is updated
		assertEquals("y", consentResponseData.get("consents.collect.val"));

		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
		assertEquals(1, xdmSharedState.size()); // verify that only collect consent is set
		assertEquals("y", xdmSharedState.get("consents.collect.val"));

		// verify Public API Call
		Map<String, Object> getConsentResponse = getConsentsSync();
		Map<String, String> responseMap = flattenMap((Map) getConsentResponse.get(ConsentTestConstants.GetConsentHelper.VALUE));
		assertEquals("y", responseMap.get("consents.collect.val"));
	}

	@Test
	public void test_DefaultConsent_GetsOverridden() throws Exception {
		// test summary
		//-----------------------------------------
		// Type         collect   AdID    Metadata
		//-----------------------------------------
		// Default        yes      -        -
		// Update         no       -        -
		//-------------------------------------------
		// Final          no       -        available
		//-------------------------------------------
		// verify in (XDMSharedState)

		// setup
		initWithDefaultConsent(CreateConsentXDMMap("y", "n")); // Initiate with collectConsent = y and adID = n
		Consent.update(CreateConsentXDMMap("n")); // // Initiate with collectConsent = n
		waitForThreads(1000);

		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // verify that only collect consent is set
		assertEquals("n", xdmSharedState.get("consents.collect.val"));
		assertEquals("n", xdmSharedState.get("consents.adID.val"));
		assertNotNull(xdmSharedState.get("consents.metadata.time"));
	}

	@Test
	public void test_Reset_DefaultConsent() throws Exception {
		// test summary
		//-----------------------------------------
		// Type         collect   AdID    Metadata
		//-----------------------------------------
		// Default        yes      no        -
		// Default         no       -        -
		//-------------------------------------------
		// Final           no       -        -
		//-------------------------------------------
		// verify in (XDMSharedState)
		// setup
		initWithDefaultConsent(CreateConsentXDMMap("y", "n")); // Initiate with collectConsent = y and adID = n
		HashMap<String, Object> config = new HashMap<String, Object>() {
			{
				put(ConsentConstants.ConfigurationKey.DEFAULT_CONSENT, CreateConsentXDMMap("n")); // Reset collectConsent = y
			}
		};
		MobileCore.updateConfiguration(config);
		waitForThreads(1000);

		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
		assertEquals(1, xdmSharedState.size()); // verify that only collect consent is set
		assertEquals("n", xdmSharedState.get("consents.collect.val"));
	}

	@Test
	public void test_DefaultConsent_DoesNotGetForwardedToEdge() throws Exception {
		// setup
		initWithDefaultConsent(CreateConsentXDMMap("y", "n")); // Initiate default consents with collectConsent = y and adID = n
		Consent.update(CreateConsentXDMMap("n")); // then update collectConsent = n
		waitForThreads(1000);

		// verify edge event for only collectConsent data
		List<Event> edgeRequestEvents = getDispatchedEventsWith(ConsentConstants.EventType.EDGE,
										ConsentConstants.EventSource.UPDATE_CONSENT);
		assertEquals(1, edgeRequestEvents.size());
		Map<String, String> edgeRequestData = flattenMap(edgeRequestEvents.get(0).getEventData());
		assertEquals(2, edgeRequestData.size()); // verify that only collect consent and metadata are updated
		assertEquals("n", edgeRequestData.get("consents.collect.val"));
		assertNotNull(edgeRequestData.get("consents.metadata.time"));
	}


	@Test
	public void test_DefaultConsent_NotSavedInPersistence() throws Exception {
		// setup
		initWithDefaultConsent(CreateConsentXDMMap("y", "n"));
		waitForThreads(2000);

		// verify persisted Data
		final String persistedJson = TestPersistenceHelper.readPersistedData(ConsentConstants.DataStoreKey.DATASTORE_NAME,
									 ConsentConstants.DataStoreKey.CONSENT_PREFERENCES);
		assertNull(persistedJson);
	}

	// --------------------------------------------------------------------------------------------
	// private helper methods
	// --------------------------------------------------------------------------------------------

	private void initWithDefaultConsent(final Map<String, Object> defaultConsentMap) throws InterruptedException {
		HashMap<String, Object> config = new HashMap<String, Object>() {
			{
				put(ConsentConstants.ConfigurationKey.DEFAULT_CONSENT, defaultConsentMap);
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
}
