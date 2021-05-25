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

import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.CreateConsentXDMMap;
import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.SAMPLE_METADATA_TIMESTAMP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
		Map<String, Object> consentData = CreateConsentXDMMap("y", "n", "vi", SAMPLE_METADATA_TIMESTAMP);

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
		Map<String, Object> consentData = CreateConsentXDMMap("n");

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
		Map<String, Object> consentData = CreateConsentXDMMap(null, null);

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

	// ========================================================================================
	// Test Scenarios   : All possible Consent object values
	// Test method      : Copy Constructor, isEmpty
	// ========================================================================================

	@Test
	public void test_CopyConstructor() {
		// setup
		Map<String, Object> consentData = CreateConsentXDMMap("y", "n");
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
		Map<String, Object> consentData = CreateConsentXDMMap("y", "n", "vi", SAMPLE_METADATA_TIMESTAMP);

		// test and verify
		Consents consents = new Consents(consentData);
		assertEquals(consentData, consents.asXDMMap());
	}

	@Test
	public void test_AsXDMMap_whenEmptyConsents() {
		// setup
		Map<String, Object> consentData = CreateConsentXDMMap(null, null);
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
		Consents firstOverridingConsent = new Consents(CreateConsentXDMMap("y"));
		baseConsent.merge(firstOverridingConsent);

		// verify
		assertEquals("y", ConsentTestUtil.readCollectConsent(baseConsent));
		assertNull(ConsentTestUtil.readAdIdConsent(baseConsent));
		assertNull(ConsentTestUtil.readTimestamp(baseConsent));

		// test again
		Consents secondOverridingConsent = new Consents(CreateConsentXDMMap("n", "n", SAMPLE_METADATA_TIMESTAMP));
		baseConsent.merge(secondOverridingConsent);

		assertEquals("n", ConsentTestUtil.readCollectConsent(baseConsent));
		assertEquals("n", ConsentTestUtil.readAdIdConsent(baseConsent));
		assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(baseConsent));
	}

	@Test
	public void test_merge_NullConsent() {
		// setup
		Consents baseConsent = new Consents(CreateConsentXDMMap("n", null, SAMPLE_METADATA_TIMESTAMP));

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
	@Test
	public void test_setTimeStamp() {
		// setup
		Consents consents = new Consents(CreateConsentXDMMap("n"));

		// test
		long currentTimestamp = System.currentTimeMillis();
		String iso8601DateString = DateUtility.dateToISO8601String(new Date(currentTimestamp));
		consents.setTimestamp(currentTimestamp);

		// verify
		assertEquals(iso8601DateString, ConsentTestUtil.readTimestamp(consents));
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
		Consents consents = new Consents(CreateConsentXDMMap("n"));
		assertTrue(consents.equals(consents));
	}

	@Test
	public void test_equals_WhenDifferentClass() {
		Consents first = new Consents(CreateConsentXDMMap("n"));

		assertFalse(first.equals("sd"));
		assertFalse(first.equals(new Object()));
	}

	@Test
	public void test_equals_WhenDifferent() {
		Consents first = new Consents(CreateConsentXDMMap("n"));
		Consents second = new Consents(CreateConsentXDMMap("y"));

		assertFalse(first.equals(second));
		assertFalse(second.equals(first));
	}

	@Test
	public void test_equals_WhenSame() {
		Consents first = new Consents(CreateConsentXDMMap("y"));
		Consents second = new Consents(CreateConsentXDMMap("y"));

		assertTrue(first.equals(second));
		assertTrue(second.equals(first));
	}

	@Test
	public void test_equals_WhenNull() {
		Consents first = new Consents(CreateConsentXDMMap("y"));
		assertFalse(first.equals(null));
	}

	@Test
	public void test_equals_WhenEmptyAndLoaded() {
		Consents first = new Consents(CreateConsentXDMMap("y"));
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
		Consents consents = new Consents(CreateConsentXDMMap("n"));
		assertTrue(consents.equalsIgnoreTimestamp(consents));

		Consents consentWithTimestamp = new Consents(CreateConsentXDMMap("n"));
		consentWithTimestamp.setTimestamp(1616985318);
		assertTrue(consents.equalsIgnoreTimestamp(consents));
	}

	@Test
	public void test_equalsIgnoreTimeStamp_whenNull() {
		Consents consents = new Consents(CreateConsentXDMMap("y"));
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
		Consents first = new Consents(CreateConsentXDMMap("y"));
		Consents second = new Consents(CreateConsentXDMMap("y"));

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
		Consents first = new Consents(CreateConsentXDMMap("y"));
		Consents second = new Consents(CreateConsentXDMMap("y"));
		Consents third = new Consents(CreateConsentXDMMap("y"));

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
		Consents first = new Consents(CreateConsentXDMMap("n"));
		Consents second = new Consents(CreateConsentXDMMap("y"));

		first.setTimestamp(1616985318);
		second.setTimestamp(1616985318);

		assertFalse(first.equalsIgnoreTimestamp(second));
		assertFalse(second.equalsIgnoreTimestamp(first));
	}

	@Test
	public void test_equalsIgnoreTimeStamp_DifferentConsentsAndDifferentTimestamp() {
		Consents first = new Consents(CreateConsentXDMMap("n"));
		Consents second = new Consents(CreateConsentXDMMap("y"));

		first.setTimestamp(1616985318);
		second.setTimestamp(1616985320);

		assertFalse(first.equalsIgnoreTimestamp(second));
		assertFalse(second.equalsIgnoreTimestamp(first));
	}
}
