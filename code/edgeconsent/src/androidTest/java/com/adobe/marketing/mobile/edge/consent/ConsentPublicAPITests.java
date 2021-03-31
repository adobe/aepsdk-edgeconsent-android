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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.TestHelper;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.TestPersistenceHelper;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;


import static com.adobe.marketing.mobile.TestHelper.getDispatchedEventsWith;
import static com.adobe.marketing.mobile.TestHelper.getSharedStateFor;
import static com.adobe.marketing.mobile.TestHelper.getXDMSharedStateFor;
import static com.adobe.marketing.mobile.TestHelper.resetTestExpectations;
import static com.adobe.marketing.mobile.TestHelper.waitForThreads;
import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ConsentPublicAPITests {

	@Rule
	public RuleChain rule = RuleChain.outerRule(new TestHelper.SetupCoreRule())
							.around(new TestHelper.RegisterMonitorExtensionRule());

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

	// --------------------------------------------------------------------------------------------
	// Tests for GetExtensionVersion API
	// --------------------------------------------------------------------------------------------
	@Test
	public void testGetExtensionVersionAPI() {
		assertEquals(ConsentConstants.EXTENSION_VERSION, Consent.extensionVersion());
	}


	@Test
	public void testRegisterExtensionAPI() throws InterruptedException {
		// test
		// Consent.registerExtension() is called in the setup method

		// verify that the extension is registered with the correct version details
		Map<String, String> sharedStateMap = flattenMap(getSharedStateFor(ConsentTestConstants.SharedStateName.EVENT_HUB,
											 1000));
		assertEquals(ConsentConstants.EXTENSION_VERSION, sharedStateMap.get("extensions.com.adobe.edge.consent.version"));
	}

	// --------------------------------------------------------------------------------------------
	// Tests for Consent.update() API
	// --------------------------------------------------------------------------------------------
	@Test
	public void testUpdateAPI() throws Exception {

		// test summary
		//-----------------------------------------
		// Type         collect   AdID    Metadata
		//-----------------------------------------
		// Default
		// Updated        YES
		//-------------------------------------------
		// Final          YES      -       available
		//-------------------------------------------
		// verify in (Persistence, ConsentResponse and XDMSharedState)

		// test
		Consent.update(ConsentTestUtil.CreateConsentXDMMap("y"));

		// verify edge event dispatched
		List<Event> edgeRequestEvents = getDispatchedEventsWith(ConsentConstants.EventType.EDGE,
										ConsentConstants.EventSource.UPDATE_CONSENT);
		assertEquals(1, edgeRequestEvents.size());
		Map<String, String> edgeRequestData = flattenMap(edgeRequestEvents.get(0).getEventData());
		assertEquals(2, edgeRequestData.size()); // verify that only collect consent and metadata are updated
		assertEquals("y", edgeRequestData.get("consents.collect.val"));
		assertNotNull(edgeRequestData.get("consents.metadata.time"));

		// verify consent response event dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(ConsentConstants.EventType.CONSENT,
											ConsentConstants.EventSource.RESPONSE_CONTENT);
		assertEquals(1, consentResponseEvents.size());
		Map<String, String> consentResponseData = flattenMap(consentResponseEvents.get(0).getEventData());
		assertEquals(2, consentResponseData.size()); // verify that only collect consent and metadata are updated
		assertEquals("y", consentResponseData.get("consents.collect.val"));
		assertNotNull(consentResponseData.get("consents.metadata.time"));

		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
		assertEquals(2, xdmSharedState.size()); // verify that only collect consent and metadata are updated
		assertEquals("y", xdmSharedState.get("consents.collect.val"));
		assertNotNull(xdmSharedState.get("consents.metadata.time"));

		//verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(ConsentConstants.DataStoreKey.DATASTORE_NAME,
									 ConsentConstants.DataStoreKey.CONSENT_PREFERENCES);
		Map<String, Object> persistedMap = Utility.toMap(new JSONObject(persistedJson));
		Map<String, String> flattenPersistedMap = flattenMap(persistedMap);
		assertEquals(2, flattenPersistedMap.size());
		assertEquals("y", flattenPersistedMap.get("consents.collect.val"));
		assertNotNull(flattenPersistedMap.get("consents.metadata.time"));
	}

	@Test
	public void testUpdateAPI_NullData() throws InterruptedException {
		// test
		Consent.update(null);

		// verify no consent update event dispatched
		List<Event> dispatchedEvents = getDispatchedEventsWith(ConsentConstants.EventType.CONSENT,
									   ConsentConstants.EventSource.UPDATE_CONSENT);
		assertEquals(0, dispatchedEvents.size());


		// verify xdm shared state is not disturbed
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000);
		assertNull(xdmSharedState);
	}

	@Test
	public void testUpdateAPI_NonXDMComplaintData() throws InterruptedException {
		// test
		Consent.update(new HashMap<String, Object>() {
			{
				put("non-XDMKey", 344);
			}
		});

		// verify no consent response, edge request event dispatched
		List<Event> edgeEventDispatched = getDispatchedEventsWith(ConsentConstants.EventType.EDGE,
										  ConsentConstants.EventSource.UPDATE_CONSENT);
		List<Event> consentResponseDispatched = getDispatchedEventsWith(ConsentConstants.EventType.CONSENT,
												ConsentConstants.EventSource.RESPONSE_CONTENT);
		assertEquals(0, edgeEventDispatched.size());
		assertEquals(0, consentResponseDispatched.size());

		// verify xdm shared state is not disturbed
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000);
		assertNull(xdmSharedState);
	}

	@Test
	public void testUpdateAPI_MergesWithExistingConsents() throws InterruptedException {

		// test summary
		//-----------------------------------------
		// Type         collect   AdID    Metadata
		//-----------------------------------------
		// Default
		// Updated       YES
		// Updated        NO      YES
		//-------------------------------------------
		// Final          NO      YES       available
		//-------------------------------------------
		// verify in (Persistence, ConsentResponse and XDMSharedState)

		// test
		Consent.update(ConsentTestUtil.CreateConsentXDMMap("y"));
		waitForThreads(2000);
		resetTestExpectations();
		Consent.update(ConsentTestUtil.CreateConsentXDMMap("n", "y"));

		// verify edge event dispatched
		List<Event> edgeRequestEvents = getDispatchedEventsWith(ConsentConstants.EventType.EDGE,
										ConsentConstants.EventSource.UPDATE_CONSENT);
		Map<String, String> edgeRequestData = flattenMap(edgeRequestEvents.get(0).getEventData());
		assertEquals(3, edgeRequestData.size()); // verify that collect, adID consent and metadata are updated
		assertEquals("n", edgeRequestData.get("consents.collect.val"));
		assertEquals("y", edgeRequestData.get("consents.adID.val"));
		assertNotNull(edgeRequestData.get("consents.metadata.time"));


		// verify consent response event dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(ConsentConstants.EventType.CONSENT,
											ConsentConstants.EventSource.RESPONSE_CONTENT);
		Map<String, String> consentResponseData = flattenMap(consentResponseEvents.get(0).getEventData());
		assertEquals(3, consentResponseData.size()); // verify that collect, adID consent and metadata are updated
		assertEquals("n", consentResponseData.get("consents.collect.val"));
		assertEquals("y", consentResponseData.get("consents.adID.val"));
		assertNotNull(consentResponseData.get("consents.metadata.time"));


		// verify xdm shared state
		Map<String, String> xdmSharedState = flattenMap(getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000));
		assertEquals(3, xdmSharedState.size()); // verify that collect, adID consent and metadata are updated
		assertEquals("n", xdmSharedState.get("consents.collect.val"));
		assertEquals("y", xdmSharedState.get("consents.adID.val"));
		assertNotNull(xdmSharedState.get("consents.metadata.time"));
	}


	// --------------------------------------------------------------------------------------------
	// Tests for Consent.getConsents() API
	// --------------------------------------------------------------------------------------------
	@Test
	public void testGetConsentsAPI() {
		// setup
		Consent.update(ConsentTestUtil.CreateConsentXDMMap("y"));

		// test
		Map<String, Object> getConsentResponse = getConsentsSync();

		Map<String, String> responseMap = flattenMap((Map) getConsentResponse.get(ConsentTestConstants.GetConsentHelper.VALUE));
		assertEquals("y", responseMap.get("consents.collect.val"));
		assertNotNull(responseMap.get("consents.metadata.time"));
	}

	@Test
	public void testGetConsentsAPI_WhenNoConsent() {
		// test
		Map<String, Object> getConsentResponse = getConsentsSync();

		// returns an xdmFormatted empty consent map
		Map<String, Object> consentResponse = (Map) getConsentResponse.get(ConsentTestConstants.GetConsentHelper.VALUE);
		Map<String, Object> consents = (Map) consentResponse.get(ConsentConstants.EventDataKey.CONSENTS);
		assertTrue(consents.isEmpty());
	}

	@Test
	public void testGetConsentsAPI_NoCallback() throws InterruptedException {
		// setup
		Consent.update(ConsentTestUtil.CreateConsentXDMMap("y"));

		// test
		Consent.getConsents(null);

		// verify shared state set
		Map<String, Object> sharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 2000);
		assertNotNull(sharedState);
	}

}
