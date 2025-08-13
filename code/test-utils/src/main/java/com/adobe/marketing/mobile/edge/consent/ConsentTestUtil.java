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

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class providing helper methods for testing Adobe Edge Consent functionality.
 * <p>
 * This class contains static methods for:
 * - Creating and manipulating consent data structures
 * - Reading consent values from Consents objects
 * - Building test events for consent preferences
 * - Synchronously retrieving consent data
 * - Applying default consent configurations
 * <p>
 * The class also includes a builder pattern (ConsentsBuilder) for constructing
 * consent data maps in a fluent manner.
 * 
 * @since 1.0.0
 */
class ConsentTestUtil {

	/** Sample timestamp for testing consent metadata (2019-09-23T18:15:45Z) */
	public static String SAMPLE_METADATA_TIMESTAMP = "2019-09-23T18:15:45Z";
	/** Alternative sample timestamp for testing consent metadata (2020-07-23T18:16:45Z) */
	public static String SAMPLE_METADATA_TIMESTAMP_OTHER = "2020-07-23T18:16:45Z";
	private static final String ADID = "adID";
	private static final String COLLECT = "collect";
	private static final String PERSONALIZE = "personalize";
	private static final String CONTENT = "content";
	private static final String VALUE = "val";
	private static final String MARKETING = "marketing";
	private static final String PREFERRED = "preferred";

	/*
	 * A fully prepared valid consent JSON looks like : { "consents": { "adID": { "val": "n" },
	 * "collect": { "val": "y" }, "personalize": { "content": { "val":"y" } } "metadata": { "time":
	 * "2019-09-23T18:15:45Z" } } }
	 */
	
	/**
	 * Creates an empty consent XDM map structure.
	 * <p>
	 * Returns a map containing only the "consents" key with an empty HashMap as its value.
	 * This is useful for testing scenarios where you need a valid but empty consent structure.
	 * 
	 * @return A Map containing an empty consents structure
	 * <p>
	 * Example:
	 * <pre>{@code
	 * Map<String, Object> emptyConsent = ConsentTestUtil.emptyConsentXDMMap();
	 * // Result: {"consents": {}}
	 * }</pre>
	 */
	static Map<String, Object> emptyConsentXDMMap() {
		Map<String, Object> consentMap = new HashMap<>();
		consentMap.put(ConsentConstants.EventDataKey.CONSENTS, new HashMap<String, Object>());
		return consentMap;
	}

	/**
	 * Builder class for constructing consent data maps in a fluent manner.
	 * <p>
	 * This builder provides methods to set various consent preferences including:
	 * - Data collection consent
	 * - Advertising ID consent  
	 * - Personalization consent
	 * - Marketing consent
	 * - Metadata timestamp
	 * <p>
	 * Usage example:
	 * <pre>{@code
	 * Map<String, Object> consentData = new ConsentsBuilder()
	 *     .setCollect("y")
	 *     .setAdId("n")
	 *     .setPersonalize("y")
	 *     .setTime("2023-01-01T12:00:00Z")
	 *     .buildToMap();
	 * }</pre>
	 */
	public static class ConsentsBuilder {

		Map<String, Object> consents = new HashMap<String, Object>();

		/**
		 * Sets the data collection consent value.
		 * 
		 * @param val The consent value ("y" for yes, "n" for no, or null/empty to remove)
		 * @return This builder instance for method chaining
		 * <p>
		 * Example:
		 * <pre>{@code
		 * .setCollect("y")  // Sets collect consent to yes
		 * .setCollect("n")  // Sets collect consent to no
		 * .setCollect(null) // Removes collect consent
		 * }</pre>
		 */
		public ConsentsBuilder setCollect(String val) {
			if (val != null && !val.isEmpty()) {
				consents.put(
					COLLECT,
					new HashMap<String, Object>() {
						{
							put(VALUE, val);
						}
					}
				);
			} else {
				consents.remove(COLLECT);
			}
			return this;
		}

		/**
		 * Sets the advertising ID consent value.
		 * 
		 * @param val The consent value ("y" for yes, "n" for no, or null/empty to remove)
		 * @return This builder instance for method chaining
		 * <p>
		 * Example:
		 * <pre>{@code
		 * .setAdId("y")  // Sets ad ID consent to yes
		 * .setAdId("n")  // Sets ad ID consent to no
		 * .setAdId(null) // Removes ad ID consent
		 * }</pre>
		 */
		public ConsentsBuilder setAdId(String val) {
			if (val != null && !val.isEmpty()) {
				consents.put(
					ADID,
					new HashMap<String, Object>() {
						{
							put(VALUE, val);
						}
					}
				);
			} else {
				consents.remove(ADID);
			}
			return this;
		}

		/**
		 * Sets the personalization consent value.
		 * <p>
		 * Creates a nested structure with "content" as the key containing the consent value.
		 * 
		 * @param val The consent value ("y" for yes, "n" for no, or null/empty to remove)
		 * @return This builder instance for method chaining
		 * <p>
		 * Example:
		 * <pre>{@code
		 * .setPersonalize("y")  // Sets personalize consent to yes
		 * .setPersonalize("n")  // Sets personalize consent to no
		 * .setPersonalize(null) // Removes personalize consent
		 * }</pre>
		 */
		public ConsentsBuilder setPersonalize(String val) {
			if (val != null && !val.isEmpty()) {
				consents.put(
					PERSONALIZE,
					new HashMap<String, Object>() {
						{
							put(
								CONTENT,
								new HashMap<String, String>() {
									{
										put(VALUE, val);
									}
								}
							);
						}
					}
				);
			} else {
				consents.remove(PERSONALIZE);
			}
			return this;
		}

		/**
		 * Sets the marketing consent with type, value, and preferred settings.
		 * <p>
		 * Creates a marketing consent structure with the specified type, consent value,
		 * and preferred setting.
		 * 
		 * @param type The marketing consent type (e.g., "email", "sms", "push")
		 * @param val The consent value ("y" for yes, "n" for no)
		 * @param preferred The preferred setting ("true" or "false")
		 * @return This builder instance for method chaining
		 * <p>
		 * Example:
		 * <pre>{@code
		 * .setMarketing("email", "y", "true")  // Sets email marketing consent to yes with preferred true
		 * .setMarketing("sms", "n", "false")   // Sets SMS marketing consent to no with preferred false
		 * }</pre>
		 */
		public ConsentsBuilder setMarketing(String type, String val, String preferred) {
			if (
				type != null &&
				val != null &&
				preferred != null &&
				!type.isEmpty() &&
				!val.isEmpty() &&
				!preferred.isEmpty()
			) {
				consents.put(
					MARKETING,
					new HashMap<String, Object>() {
						{
							put(PREFERRED, preferred);
							put(
								type,
								new HashMap<String, Object>() {
									{
										put(VALUE, val);
									}
								}
							);
						}
					}
				);
			} else {
				consents.remove(MARKETING);
			}
			return this;
		}

		/**
		 * Sets the metadata timestamp for the consent data.
		 * 
		 * @param time The timestamp string in ISO 8601 format (e.g., "2023-01-01T12:00:00Z")
		 * @return This builder instance for method chaining
		 * <p>
		 * Example:
		 * <pre>{@code
		 * .setTime("2023-01-01T12:00:00Z")  // Sets consent timestamp
		 * .setTime(null)                     // Removes timestamp
		 * }</pre>
		 */
		public ConsentsBuilder setTime(String time) {
			if (time != null && !time.isEmpty()) {
				consents.put(
					ConsentConstants.EventDataKey.METADATA,
					new HashMap<String, Object>() {
						{
							put(ConsentConstants.EventDataKey.TIME, time);
						}
					}
				);
			} else {
				consents.remove(ConsentConstants.EventDataKey.METADATA);
			}
			return this;
		}

		/**
		 * Builds the consent data as a Map structure.
		 * 
		 * @return A Map containing the constructed consent data with "consents" as the root key
		 * <p>
		 * Example:
		 * <pre>{@code
		 * Map<String, Object> consentMap = new ConsentsBuilder()
		 *     .setCollect("y")
		 *     .buildToMap();
		 * // Result: {"consents": {"collect": {"val": "y"}}}
		 * }</pre>
		 */
		public Map<String, Object> buildToMap() {
			return new HashMap<String, Object>() {
				{
					put(ConsentConstants.EventDataKey.CONSENTS, consents);
				}
			};
		}

		/**
		 * Builds the consent data as a JSON string.
		 * 
		 * @return A JSON string representation of the constructed consent data
		 * <p>
		 * Example:
		 * <pre>{@code
		 * String jsonString = new ConsentsBuilder()
		 *     .setCollect("y")
		 *     .buildToString();
		 * // Result: "{\"consents\":{\"collect\":{\"val\":\"y\"}}}"
		 * }</pre>
		 */
		public String buildToString() {
			Map<String, Object> consentDataMap = this.buildToMap();
			JSONObject jsonObject = new JSONObject(consentDataMap);
			return jsonObject.toString();
		}
	}

	/**
	 * Converts a Consents object to a JSON string representation.
	 * 
	 * @param consents The Consents object to convert
	 * @return A JSON string representation of the consents data, or null if conversion fails
	 * <p>
	 * Example:
	 * <pre>{@code
	 * Consents consentObj = new Consents();
	 * String json = ConsentTestUtil.consentsAsJson(consentObj);
	 * }</pre>
	 */
	static String consentsAsJson(Consents consents) {
		JSONObject jsonObject = new JSONObject(consents.asXDMMap());
		return jsonObject.toString();
	}

	/**
	 * Extracts the timestamp from a Consents object.
	 * <p>
	 * Reads the metadata timestamp from the consents structure.
	 * 
	 * @param consents The Consents object to read from
	 * @return The timestamp string, or null if not found or invalid
	 * <p>
	 * Example:
	 * <pre>{@code
	 * String timestamp = ConsentTestUtil.readTimestamp(consents);
	 * // Returns: "2023-01-01T12:00:00Z" or null
	 * }</pre>
	 */
	static String readTimestamp(Consents consents) {
		Map<String, Object> allConsentMap = getAllConsentsMap(consents);

		if (isNullOrEmpty(allConsentMap)) {
			return null;
		}

		Map<String, Object> collectMap = (Map<String, Object>) allConsentMap.get(
			ConsentConstants.EventDataKey.METADATA
		);

		if (isNullOrEmpty(collectMap)) {
			return null;
		}

		return (String) collectMap.get(ConsentConstants.EventDataKey.TIME);
	}

	/**
	 * Extracts the data collection consent value from a Consents object.
	 * 
	 * @param consents The Consents object to read from
	 * @return The collect consent value ("y", "n"), or null if not found
	 * <p>
	 * Example:
	 * <pre>{@code
	 * String collectConsent = ConsentTestUtil.readCollectConsent(consents);
	 * // Returns: "y", "n", or null
	 * }</pre>
	 */
	static String readCollectConsent(Consents consents) {
		Map<String, Object> allConsentMap = getAllConsentsMap(consents);

		if (isNullOrEmpty(allConsentMap)) {
			return null;
		}

		Map<String, Object> collectMap = (Map<String, Object>) allConsentMap.get(COLLECT);

		if (isNullOrEmpty(collectMap)) {
			return null;
		}

		return (String) collectMap.get("val");
	}

	/**
	 * Extracts the advertising ID consent value from a Consents object.
	 * 
	 * @param consents The Consents object to read from
	 * @return The ad ID consent value ("y", "n"), or null if not found
	 * <p>
	 * Example:
	 * <pre>{@code
	 * String adIdConsent = ConsentTestUtil.readAdIdConsent(consents);
	 * // Returns: "y", "n", or null
	 * }</pre>
	 */
	static String readAdIdConsent(Consents consents) {
		Map<String, Object> allConsentMap = getAllConsentsMap(consents);

		if (isNullOrEmpty(allConsentMap)) {
			return null;
		}

		Map<String, Object> adIdMap = (Map<String, Object>) allConsentMap.get(ADID);

		if (isNullOrEmpty(adIdMap)) {
			return null;
		}

		return (String) adIdMap.get("val");
	}

	/**
	 * Extracts the personalization consent value from a Consents object.
	 * <p>
	 * Reads the nested personalization consent value from the content structure.
	 * 
	 * @param consents The Consents object to read from
	 * @return The personalize consent value ("y", "n"), or null if not found
	 * <p>
	 * Example:
	 * <pre>{@code
	 * String personalizeConsent = ConsentTestUtil.readPersonalizeConsent(consents);
	 * // Returns: "y", "n", or null
	 * }</pre>
	 */
	static String readPersonalizeConsent(Consents consents) {
		Map<String, Object> allConsentMap = getAllConsentsMap(consents);

		if (isNullOrEmpty(allConsentMap)) {
			return null;
		}

		Map<String, Object> personalize = (Map<String, Object>) allConsentMap.get(PERSONALIZE);

		if (isNullOrEmpty(personalize)) {
			return null;
		}

		Map<String, String> contentMap = (Map<String, String>) personalize.get(CONTENT);

		if (isNullOrEmpty(contentMap)) {
			return null;
		}

		return contentMap.get("val");
	}

	/**
	 * Synchronously retrieves current consent data.
	 * <p>
	 * This method blocks until the consent data is retrieved or an error occurs.
	 * Uses a CountDownLatch to convert the asynchronous Consent.getConsents() call
	 * into a synchronous operation for testing purposes.
	 * 
	 * @return A Map containing either the consent data under "value" key or
	 *         an AdobeError under "error" key, or null if an exception occurs
	 * <p>
	 * Example:
	 * <pre>{@code
	 * Map<String, Object> result = ConsentTestUtil.getConsentsSync();
	 * if (result.containsKey("value")) {
	 *     Map<String, Object> consents = (Map<String, Object>) result.get("value");
	 * } else if (result.containsKey("error")) {
	 *     AdobeError error = (AdobeError) result.get("error");
	 * }
	 * }</pre>
	 */
	static Map<String, Object> getConsentsSync() {
		try {
			final HashMap<String, Object> getConsentResponse = new HashMap<String, Object>();
			final CountDownLatch latch = new CountDownLatch(1);
			Consent.getConsents(
					new AdobeCallbackWithError<Map<String, Object>>() {
						@Override
						public void call(Map<String, Object> consents) {
							getConsentResponse.put(ConsentTestConstants.GetConsentHelper.VALUE, consents);
							latch.countDown();
						}

						@Override
						public void fail(AdobeError adobeError) {
							getConsentResponse.put(ConsentTestConstants.GetConsentHelper.ERROR, adobeError);
							latch.countDown();
						}
					}
			);
			latch.await();

			return getConsentResponse;
		} catch (Exception exp) {
			return null;
		}
	}

	/**
	 * Applies default consent configuration to MobileCore.
	 * <p>
	 * Updates the MobileCore configuration with the provided default consent map.
	 * This is useful for setting up test scenarios with predefined consent defaults.
	 * 
	 * @param defaultConsentMap The default consent configuration map to apply
	 * <p>
	 * Example:
	 * <pre>{@code
	 * Map<String, Object> defaultConsent = new ConsentsBuilder()
	 *     .setCollect("y")
	 *     .buildToMap();
	 * ConsentTestUtil.applyDefaultConsent(defaultConsent);
	 * }</pre>
	 */
	static void applyDefaultConsent(final Map defaultConsentMap) {
		HashMap<String, Object> config = new HashMap<String, Object>() {
			{
				put(ConsentTestConstants.ConfigurationKey.DEFAULT_CONSENT, defaultConsentMap);
			}
		};
		MobileCore.updateConfiguration(config);
	}

	/**
	 * Builds an Edge consent preference event with the provided consent data.
	 * <p>
	 * Creates an Event object with type "consent:preferences" and the specified
	 * consent data as payload.
	 * 
	 * @param consents The consent data map to include in the event payload
	 * @return An Event object configured for Edge consent preferences
	 * <p>
	 * Example:
	 * <pre>{@code
	 * Map<String, Object> consentData = new ConsentsBuilder()
	 *     .setCollect("y")
	 *     .buildToMap();
	 * Event event = ConsentTestUtil.buildEdgeConsentPreferenceEventWithConsents(consentData);
	 * }</pre>
	 */
	static Event buildEdgeConsentPreferenceEventWithConsents(final Map<String, Object> consents) {
		List<Map<String, Object>> payload = new ArrayList<>();
		payload.add((Map) (consents.get("consents")));
		Map<String, Object> eventData = new HashMap<>();
		eventData.put("payload", payload);
		eventData.put("type", "consent:preferences");
		return new Event.Builder("Edge Consent Preference", EventType.EDGE, EventSource.CONSENT_PREFERENCE)
			.setEventData(eventData)
			.build();
	}

	/**
	 * Builds an Edge consent preference event from a JSON string.
	 * <p>
	 * Parses the provided JSON string and creates an Event object with type
	 * "consent:preferences".
	 * 
	 * @param jsonString The JSON string containing consent data
	 * @return An Event object configured for Edge consent preferences
	 * @throws JSONException if the JSON string is malformed
	 * <p>
	 * Example:
	 * <pre>{@code
	 * String jsonData = "{\"consents\":{\"collect\":{\"val\":\"y\"}}}";
	 * Event event = ConsentTestUtil.buildEdgeConsentPreferenceEvent(jsonData);
	 * }</pre>
	 */
	static Event buildEdgeConsentPreferenceEvent(final String jsonString) throws JSONException {
		Map<String, Object> eventData = JSONUtils.toMap(new JSONObject(jsonString));
		return new Event.Builder("Edge Consent Preference", EventType.EDGE, EventSource.CONSENT_PREFERENCE)
			.setEventData(eventData)
			.build();
	}

	/**
	 * Extracts the consents map from a Consents object.
	 * <p>
	 * Helper method that retrieves the nested consents data from the XDM map
	 * structure of a Consents object.
	 * 
	 * @param consents The Consents object to extract data from
	 * @return The consents map, or null if not found or invalid
	 */
	private static Map<String, Object> getAllConsentsMap(Consents consents) {
		Map<String, Object> xdmMap = consents.asXDMMap();

		if (isNullOrEmpty(xdmMap)) {
			return null;
		}

		Map<String, Object> allConsents = (Map<String, Object>) xdmMap.get(ConsentConstants.EventDataKey.CONSENTS);

		if (isNullOrEmpty(allConsents)) {
			return null;
		}

		return allConsents;
	}

	/**
	 * Checks if a Map is null or empty.
	 * 
	 * @param map The Map to check
	 * @return true if the map is null or empty, false otherwise
	 */
	private static boolean isNullOrEmpty(final Map map) {
		return (map == null || map.isEmpty());
	}
}
