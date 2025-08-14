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
import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.SAMPLE_METADATA_TIMESTAMP;
import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.SAMPLE_METADATA_TIMESTAMP_OTHER;
import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.consentsAsJson;
import static com.adobe.marketing.mobile.util.JSONAsserts.assertExactMatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.adobe.marketing.mobile.util.TimeUtils;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ConsentsTest {

	// ========================================================================================
	// Test Scenarios   : All possible XDMFormatted Map values
	// Test method      : Constructor, isEmpty
	// ========================================================================================
	@Test
	public void test_ConsentsCreation_With_ConsentDataMap() {
		// setup
		Map<String, Object> consentData = new ConsentsBuilder()
			.setCollect("y")
			.setAdId("n")
			.setPersonalize("vi")
			.setTime(SAMPLE_METADATA_TIMESTAMP)
			.buildToMap();

		// test
		Consents consents = new Consents(consentData);

		// verify
		assertEquals("y", ConsentTestUtil.readCollectConsent(consents));
		assertEquals("n", ConsentTestUtil.readAdIdConsent(consents));
		assertEquals("vi", ConsentTestUtil.readPersonalizeConsent(consents));
		assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(consents));
		assertFalse(consents.isEmpty());
	}

	//
	@Test
	public void test_ConsentsCreation_With_CollectConsentOnly() {
		// setup
		Map<String, Object> consentData = new ConsentsBuilder().setCollect("n").buildToMap();

		// test
		Consents consents = new Consents(consentData);

		// verify
		assertEquals("n", ConsentTestUtil.readCollectConsent(consents));
		assertNull(ConsentTestUtil.readAdIdConsent(consents));
		assertNull(ConsentTestUtil.readPersonalizeConsent(consents));
		assertNull(ConsentTestUtil.readTimestamp(consents));
	}

	@Test
	public void test_ConsentsCreation_With_NoConsentDetailsInMap() {
		// setup
		Map<String, Object> consentData = new ConsentsBuilder().buildToMap();

		// test
		Consents consents = new Consents(consentData);

		// verify
		assertTrue(consents.isEmpty());
	}

	@Test
	public void test_ConsentsCreation_With_EmptyConsentMap() {
		// test
		Consents consents = new Consents(new HashMap<String, Object>());

		// verify
		assertTrue(consents.isEmpty());
	}

	@Test
	public void test_ConsentsCreation_With_NullConsentMap() {
		// test
		Map<String, Object> xdmMap = null;
		Consents consents = new Consents(xdmMap);

		// verify
		assertTrue(consents.isEmpty());
	}

	@Test
	public void test_ConsentsCreation_With_InvalidMap() {
		// test
		Consents consents = new Consents(
			new HashMap<String, Object>() {
				{
					put("invalidKey", 30034);
				}
			}
		);

		// verify
		assertTrue(consents.isEmpty());
	}

	@Test
	public void test_ConsentsCreation_With_InvalidConsentMap() {
		// test
		Consents consents = new Consents(
			new HashMap<String, Object>() {
				{
					put("consents", 30034);
				}
			}
		);

		// verify
		assertTrue(consents.isEmpty());
	}

	@Test
	public void test_ConsentsCreation_With_ImmutableMap() {
		// setup
		Map<String, Object> xdmMap = new ConsentsBuilder().setCollect("y").setAdId("y").buildToMap();
		Map<String, Object> immutableXdmMap = Collections.unmodifiableMap(xdmMap);
		Consents baseConsent = new Consents(immutableXdmMap);

		// test
		Consents overridingConsent = new Consents(
			new ConsentsBuilder().setCollect("n").setAdId("n").setTime(SAMPLE_METADATA_TIMESTAMP).buildToMap()
		);
		baseConsent.merge(overridingConsent);

		// verify
		assertEquals("n", ConsentTestUtil.readCollectConsent(baseConsent));
		assertEquals("n", ConsentTestUtil.readAdIdConsent(baseConsent));
		assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(baseConsent));
	}

	// ========================================================================================
	// Test Scenarios   : All possible Consent object values
	// Test method      : Copy Constructor, isEmpty
	// ========================================================================================

	@Test
	public void test_CopyConstructor() {
		// setup
		Map<String, Object> consentData = new ConsentsBuilder().setCollect("y").setAdId("n").buildToMap();
		Consents originalConsent = new Consents(consentData);

		// test
		Consents copiedConsent = new Consents(originalConsent);

		assertEquals("y", ConsentTestUtil.readCollectConsent(copiedConsent));
		assertEquals("n", ConsentTestUtil.readAdIdConsent(copiedConsent));
	}

	@Test
	public void test_CopyConstructor_nullConsents() {
		// setup
		Consents originalConsent = null;

		// test
		Consents copiedConsent = new Consents(originalConsent);

		// verify
		assertTrue(copiedConsent.isEmpty());
	}

	// ========================================================================================
	// Test method : AsXDMMap
	// ========================================================================================
	@Test
	public void test_AsXDMMap() {
		// setup
		Map<String, Object> consentData = new ConsentsBuilder()
			.setCollect("y")
			.setAdId("n")
			.setPersonalize("vi")
			.setTime(SAMPLE_METADATA_TIMESTAMP)
			.buildToMap();

		// test and verify
		Consents consents = new Consents(consentData);
		assertEquals(consentData, consents.asXDMMap());
	}

	@Test
	public void test_AsXDMMap_whenEmptyConsents() {
		// setup
		Map<String, Object> consentData = new ConsentsBuilder().buildToMap();
		Consents consents = new Consents(consentData);

		// test and verify
		assertEquals(ConsentTestUtil.emptyConsentXDMMap(), consents.asXDMMap());
	}

	// ========================================================================================
	// Test method : Merge
	// ========================================================================================
	@Test
	public void test_merge() {
		// setup
		Map<String, Object> xdmMap = null;
		Consents baseConsent = new Consents(xdmMap);

		// test
		Consents firstOverridingConsent = new Consents(new ConsentsBuilder().setCollect("y").buildToMap());
		baseConsent.merge(firstOverridingConsent);

		// verify
		assertEquals("y", ConsentTestUtil.readCollectConsent(baseConsent));
		assertNull(ConsentTestUtil.readAdIdConsent(baseConsent));
		assertNull(ConsentTestUtil.readTimestamp(baseConsent));

		// test again
		Consents secondOverridingConsent = new Consents(
			new ConsentsBuilder().setCollect("n").setAdId("n").setTime(SAMPLE_METADATA_TIMESTAMP).buildToMap()
		);
		baseConsent.merge(secondOverridingConsent);

		assertEquals("n", ConsentTestUtil.readCollectConsent(baseConsent));
		assertEquals("n", ConsentTestUtil.readAdIdConsent(baseConsent));
		assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(baseConsent));
	}

	@Test
	public void test_merge_nestedMaps() {
		// setup
		Consents baseConsent = new Consents(new HashMap<>());

		// test
		ConsentsBuilder preferences = new ConsentsBuilder()
			.setCollect("y")
			.setMarketing("push", "y", "none")
			.setTime(SAMPLE_METADATA_TIMESTAMP);
		Consents firstOverridingConsent = new Consents(preferences.buildToMap());
		baseConsent.merge(firstOverridingConsent);

		// verify
		String expectedFirst =
			"{\n" +
			"  \"consents\": {\n" +
			"    \"collect\": {\"val\": \"y\"},\n" +
			"    \"marketing\": {\n" +
			"      \"preferred\": \"none\",\n" +
			"      \"push\": {\"val\": \"y\"}\n" +
			"    },\n" +
			"    \"metadata\": {\"time\": \"" +
			SAMPLE_METADATA_TIMESTAMP +
			"\"}" +
			"  }\n" +
			"}";
		String actualFirst = consentsAsJson(baseConsent);
		assertExactMatch(expectedFirst, actualFirst);

		// test again
		preferences.setMarketing("sms", "y", "sms").setTime(SAMPLE_METADATA_TIMESTAMP_OTHER);
		Consents secondOverridingConsent = new Consents(preferences.buildToMap());
		baseConsent.merge(secondOverridingConsent);

		String expectedSecond =
			"{\n" +
			"  \"consents\": {\n" +
			"    \"collect\": {\"val\": \"y\"},\n" +
			"    \"marketing\": {\n" +
			"      \"preferred\": \"sms\",\n" +
			"      \"push\": {\"val\": \"y\"},\n" +
			"      \"sms\": {\"val\": \"y\"}\n" +
			"    },\n" +
			"    \"metadata\": {\"time\": \"" +
			SAMPLE_METADATA_TIMESTAMP_OTHER +
			"\"}" +
			"  }\n" +
			"}";
		String actualSecond = consentsAsJson(baseConsent);

		assertExactMatch(expectedSecond, actualSecond);
	}

	@Test
	public void test_merge_NullConsent() {
		// setup
		Consents baseConsent = new Consents(
			new ConsentsBuilder().setCollect("n").setTime(SAMPLE_METADATA_TIMESTAMP).buildToMap()
		);

		// test
		baseConsent.merge(null);

		// verify
		assertEquals("n", ConsentTestUtil.readCollectConsent(baseConsent));
		assertNull(ConsentTestUtil.readAdIdConsent(baseConsent));
		assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(baseConsent));
	}

	// ========================================================================================
	// Test method : setTimestamp
	// ========================================================================================
	public void test_setTimeStamp() {
		// setup
		Consents consents = new Consents(new ConsentsBuilder().setCollect("n").buildToMap());

		// test
		long currentTimestamp = System.currentTimeMillis();
		String iso8601DateString = TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(currentTimestamp));
		consents.setTimestamp(currentTimestamp);

		// verify
		String consentTimeStamp = ConsentTestUtil.readTimestamp(consents);
		assertEquals(iso8601DateString, consentTimeStamp);
	}

	@Test
	public void test_setTimeStamp_whenConsentsEmpty() {
		// setup
		Consents consents = new Consents(new HashMap<String, Object>());

		// test
		consents.setTimestamp(System.currentTimeMillis());

		// verify
		assertNull(ConsentTestUtil.readTimestamp(consents));
	}

	// ========================================================================================
	// Test method : isEqual
	// ========================================================================================
	@Test
	public void test_equals_sameObject() {
		ConsentsBuilder consentsBuilder = new ConsentsBuilder();
		Consents consents = new Consents(consentsBuilder.setCollect("n").buildToMap());
		assertTrue(consents.equals(consents));
	}

	@Test
	public void test_equals_WhenDifferentClass() {
		ConsentsBuilder consentsBuilder = new ConsentsBuilder();
		Consents first = new Consents(consentsBuilder.setCollect("n").buildToMap());

		assertFalse(first.equals("sd"));
		assertFalse(first.equals(new Object()));
	}

	@Test
	public void test_equals_WhenDifferent() {
		ConsentsBuilder consentsBuilder = new ConsentsBuilder();
		Consents first = new Consents(consentsBuilder.setCollect("n").buildToMap());
		Consents second = new Consents(consentsBuilder.setCollect("y").buildToMap());

		assertFalse(first.equals(second));
		assertFalse(second.equals(first));
	}

	@Test
	public void test_equals_WhenSame() {
		ConsentsBuilder consentsBuilder = new ConsentsBuilder();
		Consents first = new Consents(consentsBuilder.setCollect("y").buildToMap());
		Consents second = new Consents(consentsBuilder.setCollect("y").buildToMap());

		assertTrue(first.equals(second));
		assertTrue(second.equals(first));
	}

	@Test
	public void test_equals_WhenNull() {
		ConsentsBuilder consentsBuilder = new ConsentsBuilder();
		Consents first = new Consents(consentsBuilder.setCollect("y").buildToMap());
		assertFalse(first.equals(null));
	}

	@Test
	public void test_equals_WhenEmptyAndLoaded() {
		ConsentsBuilder consentsBuilder = new ConsentsBuilder();
		Consents first = new Consents(consentsBuilder.setCollect("y").buildToMap());
		Consents second = new Consents(new HashMap<String, Object>());
		assertFalse(first.equals(second));
		assertFalse(second.equals(first));
	}

	@Test
	public void test_equals_WhenEmptyConsentAndEqual() {
		Consents first = new Consents(new HashMap<String, Object>());
		Consents second = new Consents(new HashMap<String, Object>());
		assertTrue(first.equals(second));
		assertTrue(second.equals(first));
	}

	// ========================================================================================
	// Test method : isEqualIgnoresTimestamp
	// ========================================================================================
	@Test
	public void test_equalsIgnoreTimeStamp_sameObject() {
		ConsentsBuilder consentsBuilder = new ConsentsBuilder();
		Consents consents = new Consents(consentsBuilder.setCollect("n").buildToMap());
		assertTrue(consents.equalsIgnoreTimestamp(consents));

		Consents consentWithTimestamp = new Consents(consentsBuilder.setCollect("n").buildToMap());
		consentWithTimestamp.setTimestamp(1616985318);
		assertTrue(consents.equalsIgnoreTimestamp(consents));
	}

	@Test
	public void test_equalsIgnoreTimeStamp_whenNull() {
		ConsentsBuilder consentsBuilder = new ConsentsBuilder();
		Consents consents = new Consents(consentsBuilder.setCollect("y").buildToMap());
		assertFalse(consents.equalsIgnoreTimestamp(null));
	}

	@Test
	public void test_equalsIgnoreTimeStamp_WhenEmptyConsentAndEqual() {
		Consents first = new Consents(new HashMap<String, Object>());
		Consents second = new Consents(new HashMap<String, Object>());
		assertTrue(first.equalsIgnoreTimestamp(second));
		assertTrue(second.equalsIgnoreTimestamp(first));
	}

	@Test
	public void test_equalsIgnoreTimeStamp_SameConsentAndSameTimeStamp() {
		ConsentsBuilder consentsBuilder = new ConsentsBuilder();
		Consents first = new Consents(consentsBuilder.setCollect("y").buildToMap());
		Consents second = new Consents(consentsBuilder.setCollect("y").buildToMap());

		assertTrue(first.equals(second));
		assertTrue(second.equals(first));

		// compare after setting timestamp
		first.setTimestamp(1616985318);
		second.setTimestamp(1616985318);

		assertTrue(first.equalsIgnoreTimestamp(second));
		assertTrue(second.equalsIgnoreTimestamp(first));
	}

	@Test
	public void test_equalsIgnoreTimeStamp_SameConsentsAndDifferentTimestamp() {
		ConsentsBuilder consentsBuilder = new ConsentsBuilder();
		Consents first = new Consents(consentsBuilder.setCollect("y").buildToMap());
		Consents second = new Consents(consentsBuilder.setCollect("y").buildToMap());
		Consents third = new Consents(consentsBuilder.setCollect("y").buildToMap());

		first.setTimestamp(1616985318);
		second.setTimestamp(1616985319);

		// compare first and second
		assertTrue(first.equalsIgnoreTimestamp(second));
		assertTrue(second.equalsIgnoreTimestamp(first));

		// compare first and third
		assertTrue(first.equalsIgnoreTimestamp(third));
		assertTrue(third.equalsIgnoreTimestamp(first));
	}

	@Test
	public void test_equalsIgnoreTimeStamp_DifferentConsentsAndSameTimestamp() {
		ConsentsBuilder consentsBuilder = new ConsentsBuilder();
		Consents first = new Consents(consentsBuilder.setCollect("n").buildToMap());
		Consents second = new Consents(consentsBuilder.setCollect("y").buildToMap());

		first.setTimestamp(1616985318);
		second.setTimestamp(1616985318);

		assertFalse(first.equalsIgnoreTimestamp(second));
		assertFalse(second.equalsIgnoreTimestamp(first));
	}

	@Test
	public void test_equalsIgnoreTimeStamp_DifferentConsentsAndDifferentTimestamp() {
		ConsentsBuilder consentsBuilder = new ConsentsBuilder();
		Consents first = new Consents(consentsBuilder.setCollect("n").buildToMap());
		Consents second = new Consents(consentsBuilder.setCollect("y").buildToMap());

		first.setTimestamp(1616985318);
		second.setTimestamp(1616985320);

		assertFalse(first.equalsIgnoreTimestamp(second));
		assertFalse(second.equalsIgnoreTimestamp(first));
	}
}
