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

import static com.adobe.marketing.mobile.edge.consent.util.ConsentFunctionalTestUtil.CreateConsentXDMMap;
import static com.adobe.marketing.mobile.edge.consent.util.ConsentFunctionalTestUtil.SAMPLE_METADATA_TIMESTAMP;
import static com.adobe.marketing.mobile.edge.consent.util.ConsentFunctionalTestUtil.buildEdgeConsentPreferenceEventWithConsents;
import static com.adobe.marketing.mobile.edge.consent.util.ConsentFunctionalTestUtil.getConsentsSync;
import static com.adobe.marketing.mobile.util.JSONAsserts.assertExactMatch;
import static com.adobe.marketing.mobile.util.NodeConfig.Scope.Subtree;
import static com.adobe.marketing.mobile.util.TestHelper.getDispatchedEventsWith;
import static com.adobe.marketing.mobile.util.TestHelper.getXDMSharedStateFor;
import static com.adobe.marketing.mobile.util.TestHelper.registerExtensions;
import static com.adobe.marketing.mobile.util.TestHelper.resetTestExpectations;
import static com.adobe.marketing.mobile.util.TestHelper.waitForThreads;
import static org.junit.Assert.assertEquals;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.edge.consent.util.ConsentTestConstants;
import com.adobe.marketing.mobile.util.CollectionEqualCount;
import com.adobe.marketing.mobile.util.JSONAsserts;
import com.adobe.marketing.mobile.util.MonitorExtension;
import com.adobe.marketing.mobile.util.TestHelper;
import com.adobe.marketing.mobile.util.TestPersistenceHelper;
import com.adobe.marketing.mobile.util.ValueTypeMatch;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

public class ConsentBootUpTests {

	@Rule
	public TestRule rule = new TestHelper.SetupCoreRule();

	@Test
	public void test_BootUp_loadsFromPersistence() throws Exception {
		// test summary
		// ------------------------------------------------------------
		// Type         collect   AdID    personalize    metadata
		// ------------------------------------------------------------
		// Persistence  pending    no        vi         available
		//
		// -------------------------------------------------------------
		// Final        pending    no        vi         available
		// -------------------------------------------------------------
		// verify in (ConsentResponse and XDMSharedState)

		// test
		initExtensionWithPersistedDataAndDefaults(CreateConsentXDMMap("p", "n", "vi", SAMPLE_METADATA_TIMESTAMP), null);
		waitForThreads(2000);

		// verify consent response event dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(EventType.CONSENT, EventSource.RESPONSE_CONTENT);
		assertEquals(1, consentResponseEvents.size());
		Map<String, Object> consentResponseData = consentResponseEvents.get(0).getEventData();

		String expected =
			"{" +
			"  \"consents\": {" +
			"    \"collect\": {" +
			"      \"val\": \"p\"" +
			"    }," +
			"    \"adID\": {" +
			"      \"val\": \"n\"" +
			"    }," +
			"    \"personalize\": {" +
			"      \"content\": {" +
			"        \"val\": \"vi\"" +
			"      }" +
			"    }," +
			"    \"metadata\": {" +
			"      \"time\": \"" +
			SAMPLE_METADATA_TIMESTAMP +
			"\"" +
			"    }" +
			"  }" +
			"}";

		JSONAsserts.assertEquals(expected, consentResponseData);

		//  verify getConsent API
		Map<String, Object> getConsentResponse = getConsentsSync();
		Map<String, Object> responseMap = (Map) getConsentResponse.get(ConsentTestConstants.GetConsentHelper.VALUE);

		JSONAsserts.assertEquals(expected, responseMap);

		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 2000);

		JSONAsserts.assertEquals(expected, xdmSharedState);
	}

	@Test
	public void test_BootUp_noPersistedData_withConfigDefault() throws Exception {
		// test summary
		// --------------------------------------------
		// Type         collect   AdID   metadata
		// --------------------------------------------
		// Persistence     -
		// Default         y
		// --------------------------------------------
		// Final           y       -       -
		// --------------------------------------------
		// verify in (ConsentResponse and XDMSharedState and GetConsent API)

		// test
		initExtensionWithPersistedDataAndDefaults(null, CreateConsentXDMMap("y"));
		waitForThreads(2000);

		// verify consent response event dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(EventType.CONSENT, EventSource.RESPONSE_CONTENT);
		assertEquals(1, consentResponseEvents.size());
		Map<String, Object> consentResponseData = consentResponseEvents.get(0).getEventData();
		String expected = "{\"consents\": {\"collect\": {\"val\": \"y\"}}}";

		JSONAsserts.assertEquals(expected, consentResponseData);

		//  verify getConsent API
		Map<String, Object> getConsentResponse = getConsentsSync();
		Map<String, Object> responseMap = (Map) getConsentResponse.get(ConsentTestConstants.GetConsentHelper.VALUE);

		JSONAsserts.assertEquals(expected, responseMap);

		// verify xdm shared state //
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 2000);
		JSONAsserts.assertEquals(expected, xdmSharedState);
	}

	@Test
	public void test_BootUp_withPersistedData_withConfigDefault() throws Exception {
		// test summary
		// --------------------------------------------
		// Type         collect   AdID   metadata
		// --------------------------------------------
		// Persistence     n
		// Default         y
		// --------------------------------------------
		// Final           n       -       -
		// --------------------------------------------
		// verify in (ConsentResponse and XDMSharedState and GetConsent API)

		// setup and test
		initExtensionWithPersistedDataAndDefaults(CreateConsentXDMMap("n"), CreateConsentXDMMap("y"));
		waitForThreads(2000);

		// verify consent response event dispatched
		List<Event> consentResponseEvents = getDispatchedEventsWith(EventType.CONSENT, EventSource.RESPONSE_CONTENT);
		assertEquals(1, consentResponseEvents.size());
		Map<String, Object> consentResponseData = consentResponseEvents.get(0).getEventData();

		String expected = "{\"consents\": {\"collect\": {\"val\": \"n\"}}}";

		JSONAsserts.assertExactMatch(expected, consentResponseData);

		//  verify getConsent API
		Map<String, Object> getConsentResponse = getConsentsSync();
		Map<String, Object> responseMap = (Map<String, Object>) getConsentResponse.get(
			ConsentTestConstants.GetConsentHelper.VALUE
		);

		JSONAsserts.assertEquals(expected, responseMap);

		// verify xdm shared state //
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 2000);

		JSONAsserts.assertEquals(expected, xdmSharedState);
	}

	@Test
	public void test_BootUp_CompleteWorkflow() throws Exception {
		// test summary
		// ----------------------------------------------------------
		// Type         collect   AdID   personalize    metadata
		// ----------------------------------------------------------
		// Persistence     y
		// Default         y       y
		// Update          n
		// EdgeResponse    n                vi        available
		// ChangeDefault   y       n
		// ----------------------------------------------------------
		// Final           n       n        vi        available
		// ----------------------------------------------------------
		// verify in (ConsentResponse and XDMSharedState and GetConsent API and persistence)

		// setup and test
		initExtensionWithPersistedDataAndDefaults(CreateConsentXDMMap("y"), CreateConsentXDMMap("y", "y"));
		Consent.update(CreateConsentXDMMap("n"));
		MobileCore.dispatchEvent(
			buildEdgeConsentPreferenceEventWithConsents(CreateConsentXDMMap("n", null, "vi", SAMPLE_METADATA_TIMESTAMP))
		);
		HashMap<String, Object> config = new HashMap<String, Object>() {
			{
				put(ConsentTestConstants.ConfigurationKey.DEFAULT_CONSENT, CreateConsentXDMMap("y", "n"));
			}
		};
		waitForThreads(2000);
		resetTestExpectations(); // reset here so we only assert on the last set of events
		MobileCore.updateConfiguration(config);
		waitForThreads(2000);

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
			"    \"personalize\": {" +
			"      \"content\": {" +
			"        \"val\": \"vi\"" +
			"      }" +
			"    }," +
			"    \"metadata\": {" +
			"      \"time\": \"STRING_TYPE\"" +
			"    }" +
			"  }" +
			"}";

		JSONAsserts.assertExactMatch(
			expected,
			consentResponseData,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);

		//  verify getConsent API
		Map<String, Object> getConsentResponse = getConsentsSync();
		Map<String, Object> responseMap = (Map<String, Object>) getConsentResponse.get(
			ConsentTestConstants.GetConsentHelper.VALUE
		);

		JSONAsserts.assertExactMatch(
			expected,
			responseMap,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);

		// verify xdm shared state
		Map<String, Object> xdmSharedState = getXDMSharedStateFor(ConsentConstants.EXTENSION_NAME, 2000);
		assertExactMatch(
			expected,
			xdmSharedState,
			new CollectionEqualCount(Subtree),
			new ValueTypeMatch("consents.metadata.time")
		);
	}

	// --------------------------------------------------------------------------------------------
	// private helper methods
	// --------------------------------------------------------------------------------------------

	private void initExtensionWithPersistedDataAndDefaults(
		final Map<String, Object> persistedData,
		final Map<String, Object> defaultConsentMap
	) throws InterruptedException {
		if (persistedData != null) {
			final JSONObject persistedJSON = new JSONObject(persistedData);
			TestPersistenceHelper.updatePersistence(
				ConsentConstants.DataStoreKey.DATASTORE_NAME,
				ConsentConstants.DataStoreKey.CONSENT_PREFERENCES,
				persistedJSON.toString()
			);
		}

		if (defaultConsentMap != null) {
			HashMap<String, Object> config = new HashMap<String, Object>() {
				{
					put(ConsentTestConstants.ConfigurationKey.DEFAULT_CONSENT, defaultConsentMap);
				}
			};
			MobileCore.updateConfiguration(config);
		}

		registerExtensions(Arrays.asList(MonitorExtension.EXTENSION, Consent.EXTENSION), null);
	}
}
