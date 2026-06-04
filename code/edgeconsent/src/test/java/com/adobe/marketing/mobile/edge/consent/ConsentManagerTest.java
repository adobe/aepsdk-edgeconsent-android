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

import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.*;
import static com.adobe.marketing.mobile.util.JSONAsserts.assertExactMatch;
import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.adobe.marketing.mobile.services.NamedCollection;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConsentManagerTest {

	@Mock
	NamedCollection mockNamedCollection;

	private ConsentManager consentManager;

	// ========================================================================================
	// Test Scenario    : consentManager load consents from persistence on boot
	// Test method      : constructor, loadFromPersistence , getCurrentConsents
	// ========================================================================================

	@Before
	public void setup() {}

	@Test
	public void test_Constructor_LoadsFromPersistence() {
		// setup
		final String updatedConsentsJSON = new ConsentsBuilder()
			.setCollect("y")
			.setTime(SAMPLE_METADATA_TIMESTAMP)
			.buildToString();
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(updatedConsentsJSON);

		// test
		consentManager = new ConsentManager(mockNamedCollection);

		// verify
		Consents currentConsents = consentManager.getCurrentConsents();
		assertEquals("y", readCollectConsent(currentConsents));
		assertNull(readAdIdConsent(currentConsents));
		assertNull(ConsentTestUtil.readPersonalizeConsent(currentConsents));
		assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(currentConsents));
	}

	@Test
	public void test_LoadFromPersistence_whenNull() {
		// setup
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(null);

		// test
		consentManager = new ConsentManager(mockNamedCollection);

		// verify
		Consents currentConsents = consentManager.getCurrentConsents();
		assertTrue(currentConsents.isEmpty());
	}

	@Test
	public void test_LoadFromPersistence_whenInvalidJSON() {
		// setup
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn("{InvalidJSON}[]$62&23Fsd^%");

		// test
		consentManager = new ConsentManager(mockNamedCollection);

		// verify
		Consents currentConsents = consentManager.getCurrentConsents();
		assertTrue(currentConsents.isEmpty());
	}

	@Test(expected = Test.None.class)
	public void test_LoadFromPersistence_whenNullNamedCollection() {
		// test
		consentManager = new ConsentManager(null);

		// verify
		Consents currentConsents = consentManager.getCurrentConsents();
		assertTrue(currentConsents.isEmpty());
		// no exception is expected when attempting to read current consents or write new consents
	}

	// ========================================================================================
	// Test Scenario    : consentManager ability to merge with current consent and persist
	// Test method      : mergeAndPersist, saveToPersistence
	// ========================================================================================

	@Test
	public void test_MergeAndPersist() {
		// setup currentConsent
		final String persistedJSON = new ConsentsBuilder()
			.setCollect("y")
			.setAdId("n")
			.setPersonalize("vi")
			.setTime(SAMPLE_METADATA_TIMESTAMP)
			.buildToString();
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(persistedJSON);
		consentManager = new ConsentManager(mockNamedCollection); // consentManager now loads the persisted data

		// test
		Consents newConsent = new Consents(
			new ConsentsBuilder()
				.setCollect("n")
				.setPersonalize("pi")
				.setTime(SAMPLE_METADATA_TIMESTAMP_OTHER)
				.buildToMap()
		);
		boolean result = consentManager.mergeAndPersist(newConsent);
		Consents mergedConsent = consentManager.getCurrentConsents();

		// verify return value is true since consents have changed
		assertTrue(result);

		// verify
		assertEquals("n", readCollectConsent(mergedConsent)); // assert CollectConsent value has changed on merge
		assertEquals("n", readAdIdConsent(mergedConsent)); // assert adIdConsent value has not changed on merge
		assertEquals("pi", readPersonalizeConsent(mergedConsent)); // assert PersonalizeConsent value has changed on merge
		assertEquals(SAMPLE_METADATA_TIMESTAMP_OTHER, ConsentTestUtil.readTimestamp(mergedConsent)); // assert time has changed on merge

		// verify if correct data is written in persistence
		verify(mockNamedCollection, times(1))
			.setString(
				ConsentConstants.DataStoreKey.CONSENT_PREFERENCES,
				new ConsentsBuilder()
					.setCollect("n")
					.setAdId("n")
					.setPersonalize("pi")
					.setTime(SAMPLE_METADATA_TIMESTAMP_OTHER)
					.buildToString()
			);
	}

	@Test
	public void test_MergeAndPersistNestedPreferences() {
		// setup currentConsent
		ConsentsBuilder preferenceBuilder = new ConsentsBuilder()
			.setCollect("y")
			.setMarketing("push", "y", "none")
			.setTime(SAMPLE_METADATA_TIMESTAMP);
		final String persistedJSON = preferenceBuilder.buildToString();
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(persistedJSON);
		consentManager = new ConsentManager(mockNamedCollection); // consentManager now loads the persisted data

		// test
		Consents newConsent = new Consents(
			preferenceBuilder.setMarketing("sms", "y", "sms").setTime(SAMPLE_METADATA_TIMESTAMP_OTHER).buildToMap()
		);
		boolean result = consentManager.mergeAndPersist(newConsent);
		Consents mergedConsent = consentManager.getCurrentConsents();

		// verify return value is true since consents have changed
		assertTrue(result);

		String actualResult = consentsAsJson(mergedConsent);
		String expectedResult =
			"{" +
			"  \"consents\": {" +
			"      \"collect\": {" +
			"        \"val\": \"y\"" +
			"      }," +
			"      \"marketing\": {" +
			"        \"preferred\": \"sms\"," +
			"        \"push\": {\"val\": \"y\"}," +
			"        \"sms\": {\"val\": \"y\"}," +
			"      }," +
			"      \"metadata\": {\"time\": \"" +
			SAMPLE_METADATA_TIMESTAMP_OTHER +
			"\"}" +
			"    }" +
			"}";

		assertExactMatch(expectedResult, actualResult);

		// verify if correct data is written in persistence
		final ArgumentCaptor<String> persistedConsents = ArgumentCaptor.forClass(String.class);
		verify(mockNamedCollection, times(1))
			.setString(eq(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES), persistedConsents.capture());

		assertExactMatch(expectedResult, persistedConsents.getValue());
	}

	@Test
	public void test_MergeAndPersist_nullConsent() {
		// setup currentConsent
		final String persistedJSON = new ConsentsBuilder()
			.setCollect("y")
			.setAdId("n")
			.setTime(SAMPLE_METADATA_TIMESTAMP)
			.buildToString();
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(persistedJSON);
		consentManager = new ConsentManager(mockNamedCollection); // consentManager now loads the persisted data

		// test
		boolean result = consentManager.mergeAndPersist(null);
		Consents mergedConsent = consentManager.getCurrentConsents();

		// verify return value is false since consents have not changed
		assertFalse(result);

		// verify that no value has changed
		assertEquals("y", readCollectConsent(mergedConsent)); // assert CollectConsent value has not changed on merge
		assertEquals("n", readAdIdConsent(mergedConsent)); // assert adIdConsent value has not changed on merge
		assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(mergedConsent)); // assert time has not changed on merge

		// verify persistence data is correct
		verify(mockNamedCollection, times(1))
			.setString(
				ConsentConstants.DataStoreKey.CONSENT_PREFERENCES,
				new ConsentsBuilder().setCollect("y").setAdId("n").setTime(SAMPLE_METADATA_TIMESTAMP).buildToString()
			);
	}

	@Test
	public void test_MergeAndPersist_emptyConsent() {
		// setup currentConsent
		final String persistedJSON = new ConsentsBuilder()
			.setCollect("y")
			.setAdId("n")
			.setTime(SAMPLE_METADATA_TIMESTAMP)
			.buildToString();
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(persistedJSON);
		consentManager = new ConsentManager(mockNamedCollection); // consentManager now loads the persisted data

		// test
		boolean result = consentManager.mergeAndPersist(new Consents(new HashMap<String, Object>()));
		Consents mergedConsent = consentManager.getCurrentConsents();

		// verify return value is false since consents have not changed
		assertFalse(result);

		// verify that no value has changed
		assertEquals("y", readCollectConsent(mergedConsent)); // assert CollectConsent value has not changed on merge
		assertEquals("n", readAdIdConsent(mergedConsent)); // assert adIdConsent value has not changed on merge
		assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(mergedConsent)); // assert time has not changed on merge

		// verify persistence data is correct
		verify(mockNamedCollection, times(1))
			.setString(
				ConsentConstants.DataStoreKey.CONSENT_PREFERENCES,
				new ConsentsBuilder().setCollect("y").setAdId("n").setTime(SAMPLE_METADATA_TIMESTAMP).buildToString()
			);
	}

	@Test
	public void test_MergeAndPersist_whenExistingConsentsNull_AndNewConsentValid() {
		// setup currentConsent
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(null);
		consentManager = new ConsentManager(mockNamedCollection); // consentManager now loads nothing from persisted
		// data

		// test
		Consents newConsent = new Consents(new ConsentsBuilder().setCollect("n").buildToMap());
		consentManager.mergeAndPersist(newConsent);
		Consents mergedConsent = consentManager.getCurrentConsents();

		// verify that no value has changed
		assertEquals("n", readCollectConsent(mergedConsent)); // assert CollectConsent value has not changed on merge
		assertNull(readAdIdConsent(mergedConsent)); // assert adID consent is null
		assertNull(ConsentTestUtil.readTimestamp(mergedConsent)); // assert timestamp is null

		// verify persistence is not disturbed
		verify(mockNamedCollection, times(1))
			.setString(
				ConsentConstants.DataStoreKey.CONSENT_PREFERENCES,
				new ConsentsBuilder().setCollect("n").buildToString()
			);
	}

	@Test(expected = Test.None.class)
	public void test_MergeAndPersist_whenNullNamedCollection() {
		consentManager = new ConsentManager(null);

		// test
		Consents newConsent = new Consents(new ConsentsBuilder().setCollect("n").buildToMap());
		consentManager.mergeAndPersist(newConsent);
		Consents mergedConsent = consentManager.getCurrentConsents();

		// verify that in-memory variable are still correct
		assertEquals("n", readCollectConsent(mergedConsent)); // assert CollectConsent value is merged
		// no exception is expected when attempting to read current consents or write new consents
	}

	@Test
	public void test_MergeAndPersist_whenExistingAndNewConsentEmpty() {
		// setup currentConsent to be null
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(null);
		consentManager = new ConsentManager(mockNamedCollection); // consentManager now loads the persisted data

		// test
		Consents newConsent = new Consents(new HashMap<String, Object>());
		consentManager.mergeAndPersist(newConsent);
		Consents mergedConsent = consentManager.getCurrentConsents();

		// verify
		assertTrue(mergedConsent.isEmpty());
		assertTrue(consentManager.getCurrentConsents().isEmpty());

		// verify that consents is removed from persistence
		verify(mockNamedCollection, times(1)).remove(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES);
	}

	@Test
	public void test_updateDefaultConsents() {
		// Scenario
		// setup
		// Current Consent             <---- null ---->
		// Default Consent          Collect  NO AdID   NO

		// verify
		// Updated  = YES
		// Updated Current Consent  Collect  NO AdID   NO

		consentManager = new ConsentManager(mockNamedCollection);

		// test
		// update default consent with Collect NO
		boolean isCurrentConsentChanged = consentManager.updateDefaultConsents(
			new Consents(new ConsentsBuilder().setCollect("n").buildToMap())
		);

		// verify
		assertTrue(isCurrentConsentChanged);

		// verify currentConsent
		Consents currentConsent = consentManager.getCurrentConsents();
		assertEquals("n", readCollectConsent(currentConsent));
		assertNull(readAdIdConsent(currentConsent)); // assert adID consent is null

		// verify defaultConsent
		Consents defaultConsents = consentManager.defaultConsents;
		assertEquals("n", readCollectConsent(defaultConsents));
		assertNull(readAdIdConsent(defaultConsents)); // assert adID consent is null
	}

	@Test
	public void test_updateDefaultConsents_whenCurrentConsentAlreadySet_ShouldNotUpdate() {
		// Scenario
		// setup
		// Current Consent          Collect YES AdID   NO Personalise vi

		// test
		// Default Consent          Collect  NO AdID   NO

		// verify
		// Updated  = NO
		// Updated Current Consent  Collect YES AdID  NO

		// setup
		consentManager = new ConsentManager(mockNamedCollection);
		consentManager.mergeAndPersist(
			new Consents(
				new ConsentsBuilder()
					.setCollect("y")
					.setAdId("n")
					.setPersonalize("vi")
					.setTime(SAMPLE_METADATA_TIMESTAMP)
					.buildToMap()
			)
		);

		// test
		// update default consent with Collect NO adID NO
		boolean isCurrentConsentChanged = consentManager.updateDefaultConsents(
			new Consents(new ConsentsBuilder().setCollect("n").setAdId("n").buildToMap())
		);

		// verify
		assertFalse(isCurrentConsentChanged);

		// verify currentConsent should be the same
		Consents currentConsent = consentManager.getCurrentConsents();
		assertEquals("y", readCollectConsent(currentConsent));
		assertEquals("n", readAdIdConsent(currentConsent));
		assertEquals("vi", readPersonalizeConsent(currentConsent));
		assertEquals(SAMPLE_METADATA_TIMESTAMP, readTimestamp(currentConsent));

		// verify defaultConsent internal variable
		Consents defaultConsents = consentManager.defaultConsents;
		assertEquals("n", readCollectConsent(defaultConsents));
		assertEquals("n", readAdIdConsent(defaultConsents));
	}

	@Test
	public void test_updateDefaultConsents_whenCurrentConsentNotSet_ShouldUpdate() {
		// Scenario
		// setup
		// Current Consent          Collect YES AdID null

		// test
		// Default Consent          Collect  NO AdID  YES

		// verify
		// Updated  = YES
		// Updated Current Consent  Collect YES AdID  YES

		// setup
		consentManager = new ConsentManager(mockNamedCollection);
		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("y").buildToMap()));

		// test
		// update default consent with Collect NO adID NO
		boolean isCurrentConsentChanged = consentManager.updateDefaultConsents(
			new Consents(new ConsentsBuilder().setCollect("n").setAdId("n").buildToMap())
		);

		// verify
		assertTrue(isCurrentConsentChanged);

		// verify currentConsent
		Consents currentConsent = consentManager.getCurrentConsents();
		assertEquals("y", readCollectConsent(currentConsent));
		assertEquals("n", readAdIdConsent(currentConsent));

		// verify defaultConsent
		Consents defaultConsents = consentManager.defaultConsents;
		assertEquals("n", readCollectConsent(defaultConsents));
		assertEquals("n", readAdIdConsent(defaultConsents));
	}

	@Test
	public void test_updateDefaultConsents__RemovalOfDefaultConsent() {
		// Scenario
		// setup 1
		// Default Consent          Collect  NO AdID  NO

		// setup 2
		// Update Consent           Collect YES

		// test
		// Default Consent          Collect  NO

		// verify
		// Updated = YES
		// Updated Current Consent  Collect YES

		// setup
		consentManager = new ConsentManager(mockNamedCollection);
		assertTrue(
			consentManager.updateDefaultConsents(
				new Consents(new ConsentsBuilder().setCollect("n").setAdId("n").buildToMap())
			)
		);

		// setup 2
		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("y").buildToMap()));

		// test
		boolean isCurrentConsentChanged = consentManager.updateDefaultConsents(
			new Consents(new ConsentsBuilder().setCollect("n").buildToMap())
		);

		// verify
		assertTrue(isCurrentConsentChanged);

		// verify currentConsent
		Consents currentConsent = consentManager.getCurrentConsents();
		assertEquals("y", readCollectConsent(currentConsent));
		assertNull(readAdIdConsent(currentConsent));

		// verify defaultConsent
		Consents defaultConsents = consentManager.defaultConsents;
		assertEquals("n", readCollectConsent(defaultConsents));
		assertNull(readAdIdConsent(defaultConsents));
	}

	@Test
	public void test_MergeAndPersist_sameConsentsDifferentTimestamp() {
		// setup currentConsent
		final String persistedJSON = new ConsentsBuilder()
			.setCollect("y")
			.setAdId("n")
			.setTime(SAMPLE_METADATA_TIMESTAMP)
			.buildToString();
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(persistedJSON);
		consentManager = new ConsentManager(mockNamedCollection); // consentManager now loads the persisted data

		// test - merge with same consents but different timestamp
		Consents newConsent = new Consents(
			new ConsentsBuilder().setCollect("y").setAdId("n").setTime(SAMPLE_METADATA_TIMESTAMP_OTHER).buildToMap()
		);
		boolean result = consentManager.mergeAndPersist(newConsent);
		Consents mergedConsent = consentManager.getCurrentConsents();

		// verify return value is false since consents have not changed (only timestamp changed)
		assertFalse(result);

		// verify that values have not changed, only timestamp
		assertEquals("y", readCollectConsent(mergedConsent));
		assertEquals("n", readAdIdConsent(mergedConsent));
		assertEquals(SAMPLE_METADATA_TIMESTAMP_OTHER, ConsentTestUtil.readTimestamp(mergedConsent));

		// verify persistence data is correct
		verify(mockNamedCollection, times(1))
			.setString(
				ConsentConstants.DataStoreKey.CONSENT_PREFERENCES,
				new ConsentsBuilder()
					.setCollect("y")
					.setAdId("n")
					.setTime(SAMPLE_METADATA_TIMESTAMP_OTHER)
					.buildToString()
			);
	}

	@Test
	public void test_MergeAndPersist_sameConsentsSameTimestamp() {
		// setup currentConsent
		final String persistedJSON = new ConsentsBuilder()
			.setCollect("y")
			.setAdId("n")
			.setTime(SAMPLE_METADATA_TIMESTAMP)
			.buildToString();
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(persistedJSON);
		consentManager = new ConsentManager(mockNamedCollection); // consentManager now loads the persisted data

		// test - merge with same consents and same timestamp
		Consents newConsent = new Consents(
			new ConsentsBuilder().setCollect("y").setAdId("n").setTime(SAMPLE_METADATA_TIMESTAMP).buildToMap()
		);
		boolean result = consentManager.mergeAndPersist(newConsent);
		Consents mergedConsent = consentManager.getCurrentConsents();

		// verify return value is false since consents have not changed
		assertFalse(result);

		// verify that values have not changed
		assertEquals("y", readCollectConsent(mergedConsent));
		assertEquals("n", readAdIdConsent(mergedConsent));
		assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(mergedConsent));

		// verify persistence data is correct
		verify(mockNamedCollection, times(1))
			.setString(
				ConsentConstants.DataStoreKey.CONSENT_PREFERENCES,
				new ConsentsBuilder().setCollect("y").setAdId("n").setTime(SAMPLE_METADATA_TIMESTAMP).buildToString()
			);
	}

	// ========================================================================================
	// Test Scenario    : evaluateCollectConsentTransition (collectConsentResyncRequired)
	// Test method      : evaluateCollectConsentTransition
	//
	// Original mergeAndPersist / updateDefaultConsents signatures are unchanged — the
	// transition signal lives in a separate method that callers invoke explicitly.
	// Tests below exercise the matrix documented in the plan's invariants table.
	// ========================================================================================

	/** null -> "y": first definitive observation after fresh install must fire the flag. */
	@Test
	public void test_evaluateCollectConsentTransition_nullToYes_returnsResyncRequired() {
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn(null);
		consentManager = new ConsentManager(mockNamedCollection);

		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("y").buildToMap()));

		assertTrue(consentManager.evaluateCollectConsentTransition());
		verify(mockNamedCollection, times(1))
			.setString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, "y");
	}

	/** "n" -> "y": classic recovery transition must fire the flag. */
	@Test
	public void test_evaluateCollectConsentTransition_nToYes_returnsResyncRequired() {
		// Simulate previous-session persisted "n" via the named collection mock
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("n");
		consentManager = new ConsentManager(mockNamedCollection);

		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("y").buildToMap()));

		assertTrue(consentManager.evaluateCollectConsentTransition());
		verify(mockNamedCollection, times(1))
			.setString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, "y");
	}

	/** "y" -> "y": idempotent. No transition. */
	@Test
	public void test_evaluateCollectConsentTransition_yToYes_doesNotReturnResyncRequired() {
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("y");
		consentManager = new ConsentManager(mockNamedCollection);

		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("y").buildToMap()));

		assertFalse(consentManager.evaluateCollectConsentTransition());
		// "y" already persisted — no redundant write
		verify(mockNamedCollection, Mockito.never())
			.setString(eq(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT), Mockito.anyString());
	}

	/**
	 * <b>Load-bearing test for the user-flagged case.</b>
	 * "y" -> "p" -> "y": pending must NOT overwrite the prior "y"; the final "y" compares
	 * against {@code lastDefinitive = "y"} and must NOT fire the flag.
	 */
	@Test
	public void test_evaluateCollectConsentTransition_yToPToY_doesNotReturnResyncRequired() {
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("y");
		consentManager = new ConsentManager(mockNamedCollection);

		// "p" must not advance lastDefinitive (and must not write to persistence)
		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("p").buildToMap()));
		assertFalse(consentManager.evaluateCollectConsentTransition());

		// Subsequent "y" — lastDefinitive should still be "y" (mock continues to return "y")
		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("y").buildToMap()));
		assertFalse("y -> p -> y must not fire the flag", consentManager.evaluateCollectConsentTransition());
		// LAST_DEFINITIVE_COLLECT_CONSENT must NEVER have been written during this sequence
		verify(mockNamedCollection, Mockito.never())
			.setString(eq(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT), Mockito.anyString());
	}

	/**
	 * "n" -> "p" -> "y": pending in the middle must not erase the "n"; the final "y" is a
	 * transition from "n" and must fire the flag.
	 */
	@Test
	public void test_evaluateCollectConsentTransition_nToPToY_returnsResyncRequired() {
		// Persisted lastDefinitive starts at "n" (returned by both reads since "p" doesn't update it).
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("n");
		consentManager = new ConsentManager(mockNamedCollection);

		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("p").buildToMap()));
		assertFalse(consentManager.evaluateCollectConsentTransition());

		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("y").buildToMap()));
		assertTrue(consentManager.evaluateCollectConsentTransition());

		verify(mockNamedCollection, times(1))
			.setString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, "y");
	}

	/**
	 * "y" -> "n" -> "p" -> "y": the "p" in the middle must not erase the prior "n" that
	 * landed in lastDefinitive. The final "y" must still fire the flag because the
	 * effective comparison is against "n" (not "y").
	 */
	@Test
	public void test_evaluateCollectConsentTransition_yToNToPToY_returnsResyncRequired() {
		// Start with lastDefinitive = "y" — Mockito returns "y" until we overwrite via the setter.
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("y");
		consentManager = new ConsentManager(mockNamedCollection);

		// "y" -> "n": flag false, lastDefinitive advances to "n"
		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("n").buildToMap()));
		assertFalse(consentManager.evaluateCollectConsentTransition());
		verify(mockNamedCollection, times(1))
			.setString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, "n");

		// Switch the mock to return "n" now (mimicking what the prior setString did)
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("n");

		// "n" -> "p": flag false, lastDefinitive MUST NOT advance (still "n")
		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("p").buildToMap()));
		assertFalse(consentManager.evaluateCollectConsentTransition());
		// No additional setString for the pending event
		verify(mockNamedCollection, Mockito.times(1))
			.setString(eq(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT), Mockito.anyString());

		// "p" -> "y": flag MUST fire because lastDefinitive is still "n"
		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("y").buildToMap()));
		assertTrue(consentManager.evaluateCollectConsentTransition());
		verify(mockNamedCollection, times(1))
			.setString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, "y");
	}

	/**
	 * "p" as the first-ever event must NOT persist anything in lastDefinitiveCollectConsent
	 * and must NOT fire the flag.
	 */
	@Test
	public void test_evaluateCollectConsentTransition_pendingAlone_doesNotPersistOrFire() {
		// Note: evaluateCollectConsentTransition early-returns on "p" before reading
		// LAST_DEFINITIVE_COLLECT_CONSENT, so we deliberately do NOT stub that getString
		// — adding an unused stub would trigger Mockito's strict runner.
		consentManager = new ConsentManager(mockNamedCollection);

		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("p").buildToMap()));
		assertFalse(consentManager.evaluateCollectConsentTransition());

		// Pending alone must not write to lastDefinitive
		verify(mockNamedCollection, Mockito.never())
			.setString(eq(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT), Mockito.anyString());
		// And it certainly must not REMOVE either (no prior value to clear).
		verify(mockNamedCollection, Mockito.never())
			.remove(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT);
	}

	/**
	 * A change to a non-{@code collect} dimension (e.g. adID) must NOT fire the flag, even
	 * though {@code mergeAndPersist} returns true (state did change).
	 */
	@Test
	public void test_evaluateCollectConsentTransition_otherDimensionChange_doesNotFireFlag() {
		// Persisted lastDefinitive = "y" so the y -> y case applies and no flag fires.
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("y");
		// Existing persisted consent already has collect=y
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(new ConsentsBuilder().setCollect("y").buildToString());
		consentManager = new ConsentManager(mockNamedCollection);

		// Update only adID — collect stays at "y"
		final boolean changed = consentManager.mergeAndPersist(
			new Consents(new ConsentsBuilder().setAdId("n").buildToMap())
		);
		assertTrue("adID change should still be a state change", changed);
		assertFalse(consentManager.evaluateCollectConsentTransition());
	}

	/**
	 * Defensive: when the manager is constructed with a {@code null} {@link NamedCollection}
	 * (e.g. the data-store service was unavailable), {@link ConsentManager#evaluateCollectConsentTransition()}
	 * must not crash. The read returns {@code null}, the write is a no-op, and the flag
	 * is still computed correctly from in-memory state.
	 *
	 * <p>This exercises the {@code namedCollection == null} guard in both
	 * {@code getLastDefinitiveCollectConsent} and {@code setLastDefinitiveCollectConsent}.
	 */
	@Test(expected = Test.None.class)
	public void test_evaluateCollectConsentTransition_nullNamedCollection_doesNotCrash() {
		consentManager = new ConsentManager(null);
		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setCollect("y").buildToMap()));

		// Read returns null (no persistence), write is a no-op.
		assertNull(consentManager.getLastDefinitiveCollectConsent());
		// With null persistence the "previous definitive" is null and the new collect is "y",
		// so the transition still fires correctly from in-memory state.
		assertTrue(consentManager.evaluateCollectConsentTransition());
	}

	/**
	 * If the effective {@code collect.val} becomes {@code null} (e.g. a merge produces a
	 * state with no {@code collect} key) and there is a previously persisted definitive
	 * value, the tracker must be cleared via {@link NamedCollection#remove(String)} rather
	 * than written. Exercises the {@code value == null} branch of
	 * {@code setLastDefinitiveCollectConsent}.
	 */
	@Test
	public void test_evaluateCollectConsentTransition_currentCollectAbsent_clearsPersistedTracker() {
		// Persisted lastDefinitive = "y" so the value transitions to null on the next merge
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("y");
		// Persisted consents have only adID — no collect key at all
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(new ConsentsBuilder().setAdId("n").buildToString());
		consentManager = new ConsentManager(mockNamedCollection);

		// newCollectVal is null (no collect key); previousDefinitive is "y" → not equal → clear
		assertFalse(consentManager.evaluateCollectConsentTransition());

		verify(mockNamedCollection, times(1)).remove(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT);
		// Must NOT also write a string
		verify(mockNamedCollection, Mockito.never())
			.setString(eq(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT), Mockito.anyString());
	}

	/**
	 * When both the effective {@code collect.val} AND the persisted definitive are {@code null}
	 * — i.e. fresh manager observing only non-collect updates — the equality check returns true,
	 * so neither {@code setString} nor {@code remove} is called. Exercises the
	 * {@code a == null && b == null} branch of the private {@code equals} helper.
	 */
	@Test
	public void test_evaluateCollectConsentTransition_bothNull_noPersistedWrite() {
		// Mock returns null by default for LAST_DEFINITIVE_COLLECT_CONSENT (no stub needed).
		consentManager = new ConsentManager(mockNamedCollection);

		// Merge a non-collect-only consent update so the current state has no collect key
		consentManager.mergeAndPersist(new Consents(new ConsentsBuilder().setAdId("y").buildToMap()));

		assertFalse(consentManager.evaluateCollectConsentTransition());

		// Neither path of setLastDefinitiveCollectConsent should be exercised
		verify(mockNamedCollection, Mockito.never())
			.setString(eq(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT), Mockito.anyString());
		verify(mockNamedCollection, Mockito.never())
			.remove(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT);
	}
}
