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

import static com.adobe.marketing.mobile.edge.consent.util.ConsentFunctionalTestUtil.getConsentsSync;
import static com.adobe.marketing.mobile.util.JSONAsserts.assertExactMatch;
import static com.adobe.marketing.mobile.util.NodeConfig.Scope.Subtree;
import static com.adobe.marketing.mobile.util.TestHelper.getDispatchedEventsWith;
import static com.adobe.marketing.mobile.util.TestHelper.getSharedStateFor;
import static com.adobe.marketing.mobile.util.TestHelper.getXDMSharedStateFor;
import static com.adobe.marketing.mobile.util.TestHelper.resetTestExpectations;
import static com.adobe.marketing.mobile.util.TestHelper.waitForThreads;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.edge.consent.util.ConsentFunctionalTestUtil;
import com.adobe.marketing.mobile.edge.consent.util.ConsentTestConstants;
import com.adobe.marketing.mobile.util.CollectionEqualCount;
import com.adobe.marketing.mobile.util.MonitorExtension;
import com.adobe.marketing.mobile.util.TestHelper;
import com.adobe.marketing.mobile.util.TestPersistenceHelper;
import com.adobe.marketing.mobile.util.ValueTypeMatch;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ConsentPublicAPITests {

	@Rule
	public TestRule rule = new TestHelper.SetupCoreRule();

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

		TestHelper.registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Consent.EXTENSION), config);
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
		Map<String, Object> sharedStateMap = getSharedStateFor(ConsentTestConstants.SharedStateName.EVENT_HUB, 1000);

		String expected =
			"{ " +
			"  \"extensions\": { " +
			"    \"com.adobe.edge.consent\": { " +
			"      \"version\": \"" +
			ConsentConstants.EXTENSION_VERSION +
			"\" " +
			"    } " +
			"  } " +
			"}";
		assertExactMatch(expected, sharedStateMap);
	}

	// --------------------------------------------------------------------------------------------
	// Tests for Consent.update() API
	// --------------------------------------------------------------------------------------------
	@Test
	public void testUpdateAPI() throws Exception {
		// test summary
		// -----------------------------------------
		// Type         collect   AdID    Metadata
		// -----------------------------------------
		// Default
		// Updated        YES
		// -------------------------------------------
		// Final          YES      -       available
		// -------------------------------------------
		// verify in (Persistence, ConsentResponse and XDMSharedState)

		// test
		Consent.update(ConsentFunctionalTestUtil.CreateConsentXDMMap("y"));

		// verify edge event dispatched
		List<Event> edgeRequestEvents = getDispatchedEventsWith(EventType.EDGE, EventSource.UPDATE_CONSENT);
		assertEquals(1, edgeRequestEvents.size());
		Map<String, Object> edgeRequestData = edgeRequestEvents.get(0).getEventData();
		String expected =
			"{" +
			"  \"consents\": {" +
			"    \"collect\": {" +
			"      \"val\": \"y\"" +
			"    }," +
			"    \"metadata\": {" +
			"      \"time\": \"STRING_TYPE\"" +
			"    }" +
			"  }" +
			"}";

		// verify that only collect consent and metadata are updated
		assertExactMatch(
			expected,
			edgeRequestData,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);

		// verify consent response event dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(EventType.CONSENT, EventSource.RESPONSE_CONTENT);
		assertEquals(1, consentResponseEvents.size());

		Map<String, Object> consentResponseData = consentResponseEvents.get(0).getEventData();

		// verify that only collect consent and metadata are updated
		assertExactMatch(
			expected,
			consentResponseData,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);

		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000);

		// verify that only collect consent and metadata are updated
		assertExactMatch(
			expected,
			xdmSharedState,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);

		// verify persisted data
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			ConsentConstants.DataStoreKey.DATASTORE_NAME,
			ConsentConstants.DataStoreKey.CONSENT_PREFERENCES
		);

		// verify that only collect consent and metadata are updated
		assertExactMatch(
			expected,
			persistedJson,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);
	}

	@Test
	public void testUpdateAPI_NullData() throws InterruptedException {
		// test
		Consent.update(null);

		// verify no consent update event dispatched
		List<Event> dispatchedEvents = getDispatchedEventsWith(EventType.CONSENT, EventSource.UPDATE_CONSENT);
		assertEquals(0, dispatchedEvents.size());

		// verify xdm shared state is not disturbed
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000);
		assertNull(xdmSharedState);
	}

	@Test
	public void testUpdateAPI_NonXDMCompliantData() throws InterruptedException {
		// test
		Consent.update(
			new HashMap<String, Object>() {
				{
					put("non-XDMKey", 344);
				}
			}
		);

		// verify no consent response, edge request event dispatched
		List<Event> edgeEventDispatched = getDispatchedEventsWith(EventType.EDGE, EventSource.UPDATE_CONSENT);
		List<Event> consentResponseDispatched = getDispatchedEventsWith(
			EventType.CONSENT,
			EventSource.RESPONSE_CONTENT
		);
		assertEquals(0, edgeEventDispatched.size());
		assertEquals(0, consentResponseDispatched.size());

		// verify xdm shared state is not disturbed
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000);
		assertNull(xdmSharedState);
	}

	@Test
	public void testUpdateAPI_MergesWithExistingConsents() throws InterruptedException {
		// test summary
		// -----------------------------------------
		// Type         collect   AdID    Metadata
		// -----------------------------------------
		// Default
		// Updated       YES
		// Updated        NO      YES
		// -------------------------------------------
		// Final          NO      YES       available
		// -------------------------------------------
		// verify in (Persistence, ConsentResponse and XDMSharedState)

		// test
		Consent.update(ConsentFunctionalTestUtil.CreateConsentXDMMap("y"));
		waitForThreads(2000);
		resetTestExpectations();
		Consent.update(ConsentFunctionalTestUtil.CreateConsentXDMMap("n", "y"));

		// verify edge event dispatched
		List<Event> edgeRequestEvents = getDispatchedEventsWith(EventType.EDGE, EventSource.UPDATE_CONSENT);

		String expected =
			"{" +
			"\"consents\": {" +
			"    \"collect\": {" +
			"        \"val\": \"n\"" +
			"    }," +
			"    \"adID\": {" +
			"        \"val\": \"y\"" +
			"    }," +
			"    \"metadata\": {" +
			"        \"time\": \"STRING_TYPE\"" +
			"    }" +
			"}" +
			"}";

		Map<String, Object> edgeRequestData = edgeRequestEvents.get(0).getEventData();

		// verify that collect, adID consent and metadata are updated
		assertExactMatch(
			expected,
			edgeRequestData,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);

		// verify consent response event dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(EventType.CONSENT, EventSource.RESPONSE_CONTENT);
		Map<String, Object> consentResponseData = consentResponseEvents.get(0).getEventData();
		// verify that collect, adID consent and metadata are updated
		assertExactMatch(
			expected,
			consentResponseData,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);

		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000);
		// verify that collect, adID consent and metadata are updated
		assertExactMatch(
			expected,
			xdmSharedState,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);
	}

	// --------------------------------------------------------------------------------------------
	// Tests for Consent.getConsents() API
	// --------------------------------------------------------------------------------------------
	@Test
	public void testGetConsentsAPI() {
		// setup
		Consent.update(ConsentFunctionalTestUtil.CreateConsentXDMMap("y"));

		// test
		Map<String, Object> getConsentResponse = getConsentsSync();

		Map<String, Object> responseMap = (Map) getConsentResponse.get(ConsentTestConstants.GetConsentHelper.VALUE);

		String expected =
			"{" +
			"\"consents\": {" +
			"    \"collect\": {" +
			"        \"val\": \"y\"" +
			"    }," +
			"    \"metadata\": {" +
			"        \"time\": \"STRING_TYPE\"" +
			"    }" +
			"}" +
			"}";

		// verify that only collect consent and metadata are updated
		assertExactMatch(expected, responseMap, new ValueTypeMatch("consents.metadata.time"));
	}

	@Test
	public void testGetConsentsAPI_WhenNoConsent() {
		// test
		Map<String, Object> getConsentResponse = getConsentsSync();

		// returns an xdmFormatted empty consent map
		Map<String, Object> consentResponse = (Map) getConsentResponse.get(ConsentTestConstants.GetConsentHelper.VALUE);
		Map<String, Object> consents = (Map) consentResponse.get(ConsentTestConstants.EventDataKey.CONSENTS);
		assertTrue(consents.isEmpty());
	}

	@Test
	public void testGetConsentsAPI_NoCallback() throws InterruptedException {
		// setup
		Consent.update(ConsentFunctionalTestUtil.CreateConsentXDMMap("y"));

		// test
		Consent.getConsents(null);

		// add a wait time for mobile core to return the shared state before verifying the test
		Thread.sleep(2000);

		// verify shared state set
		Map<String, Object> sharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 2000);
		assertNotNull(sharedState);
	}
}
