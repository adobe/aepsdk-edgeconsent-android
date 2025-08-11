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

import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.ConsentsBuilder;
import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.applyDefaultConsent;
import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.buildEdgeConsentPreferenceEvent;
import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.buildEdgeConsentPreferenceEventWithConsents;
import static com.adobe.marketing.mobile.util.JSONAsserts.assertExactMatch;
import static com.adobe.marketing.mobile.util.JSONAsserts.assertTypeMatch;
import static com.adobe.marketing.mobile.util.NodeConfig.Scope.Subtree;
import static com.adobe.marketing.mobile.util.TestHelper.getDispatchedEventsWith;
import static com.adobe.marketing.mobile.util.TestHelper.getXDMSharedStateFor;
import static com.adobe.marketing.mobile.util.TestHelper.resetTestExpectations;
import static com.adobe.marketing.mobile.util.TestHelper.waitForThreads;
import static org.junit.Assert.assertEquals;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.util.CollectionEqualCount;
import com.adobe.marketing.mobile.util.MonitorExtension;
import com.adobe.marketing.mobile.util.TestHelper;
import com.adobe.marketing.mobile.util.TestPersistenceHelper;
import com.adobe.marketing.mobile.util.ValueTypeMatch;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class ConsentEdgeResponseHandlingTests {

	static final String SHARED_STATE = "com.adobe.eventSource.sharedState";

	@Rule
	public TestRule rule = new TestHelper.SetupCoreRule();

	// --------------------------------------------------------------------------------------------
	// Setup
	// --------------------------------------------------------------------------------------------

	@Before
	public void setup() throws Exception {
		TestHelper.registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Consent.EXTENSION), null);
	}

	@Test
	public void test_EdgeResponse_MergesWithCurrentConsent() throws Exception {
		// test summary
		// -----------------------------------------
		// Type         collect   AdID    Metadata
		// -----------------------------------------
		// Default      pending     NO      null
		// Updated        YES
		// EdgeResponse    NO
		// -------------------------------------------
		// Final           NO      NO       available
		// -------------------------------------------
		// verify in (Persistence, ConsentResponse and XDMSharedState)

		// setup
		applyDefaultConsent(new ConsentsBuilder().setCollect("p").setAdId("n").buildToMap());
		Consent.update(new ConsentsBuilder().setCollect("y").buildToMap());
		waitForThreads(1000);
		resetTestExpectations();

		MobileCore.dispatchEvent(
			buildEdgeConsentPreferenceEventWithConsents(new ConsentsBuilder().setCollect("n").buildToMap())
		); // edge response sets the collect consent to no
		waitForThreads(1000);

		// verify consent response event dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(EventType.CONSENT, EventSource.RESPONSE_CONTENT);
		assertEquals(1, consentResponseEvents.size());

		Map<String, Object> consentResponseData = consentResponseEvents.get(0).getEventData();

		String expected =
			"{" +
			"  \"consents\": {" +
			"    \"collect\": {" +
			"      \"val\": \"n\"" +
			"    }," +
			"    \"adID\": {" +
			"      \"val\": \"n\"" +
			"    }," +
			"    \"metadata\": {" +
			"      \"time\": \"STRING_TYPE\"" +
			"    }" +
			"  }" +
			"}";

		assertTypeMatch(
			expected,
			consentResponseData,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);

		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000);

		assertExactMatch(
			expected,
			xdmSharedState,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);

		// verify persisted data - default consents are not persisted
		final String persistedJson = TestPersistenceHelper.readPersistedData(
			ConsentConstants.DataStoreKey.DATASTORE_NAME,
			ConsentConstants.DataStoreKey.CONSENT_PREFERENCES
		);

		String expectedPersistedData =
			"{" +
			"  \"consents\": {" +
			"    \"collect\": {" +
			"      \"val\": \"n\"" +
			"    }," +
			"    \"metadata\": {" +
			"      \"time\": \"STRING_TYPE\"" +
			"    }" +
			"  }" +
			"}";

		assertExactMatch(
			expectedPersistedData,
			persistedJson,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);
	}

	@Test
	public void test_EdgeResponse_InvalidPayload() throws Exception {
		// test summary
		// -----------------------------------------
		// Type         collect   AdID    Metadata
		// -----------------------------------------
		// Default      pending
		// Updated        YES
		// EdgeResponse  invalid
		// -------------------------------------------
		// Final           YES            available
		// -------------------------------------------

		// setup
		applyDefaultConsent(new ConsentsBuilder().setCollect("p").buildToMap());
		Consent.update(new ConsentsBuilder().setCollect("y").buildToMap());
		waitForThreads(1000);
		resetTestExpectations();

		// test
		MobileCore.dispatchEvent(
			buildEdgeConsentPreferenceEvent("{\n" + "  \"payload\" : \"not what I expect\"\n" + "}")
		);
		waitForThreads(1000);

		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000);

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

		String expectedPersistedData =
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

		assertExactMatch(
			expectedPersistedData,
			persistedJson,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);
	}

	@Test
	public void test_EdgeResponse_NoConsentChangeAndNoTimestamp() throws Exception {
		// test summary
		// -----------------------------------------
		// Type         collect   AdID    Metadata
		// -----------------------------------------
		// Updated        YES      YES      timestamp
		// EdgeResponse   YES      YES      null
		// -------------------------------------------
		// Final           YES      YES     timestamp
		// -------------------------------------------

		// setup
		Consent.update(new ConsentsBuilder().setCollect("y").buildToMap());
		waitForThreads(1000);
		resetTestExpectations();

		// read timestamp from XDM shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000);
		String timestamp = getTimestampFromXDMSharedState(xdmSharedState);

		// test
		MobileCore.dispatchEvent(
			buildEdgeConsentPreferenceEventWithConsents(new ConsentsBuilder().setCollect("y").buildToMap())
		);
		waitForThreads(1000);

		// verify that shared state and consent response events are not dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(EventType.CONSENT, EventSource.RESPONSE_CONTENT);
		assertEquals(0, consentResponseEvents.size());
		List<Event> sharedStateChangeEvents = getDispatchedEventsWith(EventType.HUB, SHARED_STATE);
		assertEquals(0, sharedStateChangeEvents.size());

		// verify timestamp has not changed
		xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000);
		String expected = "{\"consents\": {\"metadata\": {\"time\": \"" + timestamp + "\"}}}";

		assertExactMatch(expected, xdmSharedState);
	}

	@Test
	public void test_EdgeResponse_NoConsentChangeAndSameTimestamp() throws Exception {
		// test summary
		// -----------------------------------------
		// Type         collect   AdID    Metadata
		// -----------------------------------------
		// Updated        YES      YES      timestamp
		// EdgeResponse   YES      YES      timestamp
		// -------------------------------------------
		// Final           YES      YES     timestamp
		// -------------------------------------------

		// setup
		Consent.update(new ConsentsBuilder().setCollect("y").setAdId("n").buildToMap());
		waitForThreads(1000);
		resetTestExpectations();

		// read timestamp from XDM shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000);
		String timestamp = getTimestampFromXDMSharedState(xdmSharedState);

		// test
		MobileCore.dispatchEvent(
			buildEdgeConsentPreferenceEventWithConsents(
				new ConsentsBuilder().setCollect("y").setAdId("n").setTime(timestamp).buildToMap()
			)
		);
		waitForThreads(1000);

		// verify that shared state and consent response events are not dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(EventType.CONSENT, EventSource.RESPONSE_CONTENT);
		assertEquals(0, consentResponseEvents.size());
		List<Event> sharedStateChangeEvents = getDispatchedEventsWith(EventType.HUB, SHARED_STATE);
		assertEquals(0, sharedStateChangeEvents.size());

		// verify timestamp has not changed
		xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 1000);
		String expected = "{\"consents\": {\"metadata\": {\"time\": \"" + timestamp + "\"}}}";
		assertExactMatch(expected, xdmSharedState);
	}

	private String getTimestampFromXDMSharedState(Map<String, Object> xdmSharedState) {
		if (xdmSharedState == null) {
			return "Error: xdmSharedState is null";
		}

		Object consents = xdmSharedState.get("consents");
		if (consents == null) {
			return "Error: consents is null";
		}

		Object metadata = ((Map<String, Object>) consents).get("metadata");
		if (metadata == null) {
			return "Error: metadata is null";
		}

		Object timestamp = ((Map<String, Object>) metadata).get("time");
		if (timestamp == null) {
			return "Error: timestamp is null";
		}

		return (String) timestamp; // return the timestamp
	}
}
