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

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

class ConsentTestUtil {

	public static String SAMPLE_METADATA_TIMESTAMP = "2019-09-23T18:15:45Z";
	public static String SAMPLE_METADATA_TIMESTAMP_OTHER = "2020-07-23T18:16:45Z";
	private static final String ADID = "adID";
	private static final String COLLECT = "collect";
	private static final String PERSONALIZE = "personalize";
	private static final String CONTENT = "content";
	private static final String VALUE = "val";

	/**
	 * A fully prepared valid consent JSON looks like :
	 * {
	 *   "consents": {
	 *     "adID": {
	 *       "val": "n"
	 *     },
	 *     "collect": {
	 *       "val": "y"
	 *     },
	 *     "personalize": {
	 *           "content": {
	 *               "val":"y"
	 *            }
	 *      }
	 *     "metadata": {
	 *       "time": "2019-09-23T18:15:45Z"
	 *     }
	 *   }
	 * }
	 */

	static Map<String, Object> emptyConsentXDMMap() {
		Map<String, Object> consentMap = new HashMap<>();
		consentMap.put(ConsentConstants.EventDataKey.CONSENTS, new HashMap<String, Object>());
		return consentMap;
	}

	static String CreateConsentsXDMJSONString(final String collectConsentString) {
		return CreateConsentsXDMJSONString(collectConsentString, null);
	}

	static String CreateConsentsXDMJSONString(final String collectConsentString, final String adIDConsentString) {
		return CreateConsentsXDMJSONString(collectConsentString, adIDConsentString, null);
	}

	static String CreateConsentsXDMJSONString(
		final String collectConsentString,
		final String adIDConsentString,
		final String time
	) {
		return CreateConsentsXDMJSONString(collectConsentString, adIDConsentString, null, time);
	}

	static String CreateConsentsXDMJSONString(
		final String collectConsentString,
		final String adIDConsentString,
		final String personalizeConsentString,
		final String time
	) {
		Map<String, Object> consentDataMap = CreateConsentXDMMap(
			collectConsentString,
			adIDConsentString,
			personalizeConsentString,
			time
		);
		JSONObject jsonObject = new JSONObject(consentDataMap);
		return jsonObject.toString();
	}

	static Map<String, Object> CreateConsentXDMMap(final String collectConsentString) {
		return CreateConsentXDMMap(collectConsentString, null);
	}

	static Map<String, Object> CreateConsentXDMMap(final String collectConsentString, final String adIDConsentString) {
		return CreateConsentXDMMap(collectConsentString, adIDConsentString, null);
	}

	static Map<String, Object> CreateConsentXDMMap(
		final String collectConsentString,
		final String adIDConsentString,
		final String time
	) {
		return CreateConsentXDMMap(collectConsentString, adIDConsentString, null, time);
	}

	static Map<String, Object> CreateConsentXDMMap(
		final String collectConsentString,
		final String adIDConsentString,
		final String personalizeConsentString,
		final String time
	) {
		Map<String, Object> consentData = new HashMap<String, Object>();
		Map<String, Object> consents = new HashMap<String, Object>();

		if (collectConsentString != null) {
			consents.put(
				COLLECT,
				new HashMap<String, String>() {
					{
						put(VALUE, collectConsentString);
					}
				}
			);
		}

		if (adIDConsentString != null) {
			consents.put(
				ADID,
				new HashMap<String, String>() {
					{
						put(VALUE, adIDConsentString);
					}
				}
			);
		}

		if (personalizeConsentString != null) {
			consents.put(
				PERSONALIZE,
				new HashMap<String, Object>() {
					{
						put(
							CONTENT,
							new HashMap<String, String>() {
								{
									put(VALUE, personalizeConsentString);
								}
							}
						);
					}
				}
			);
		}

		if (time != null) {
			Map<String, String> metaDataMap = new HashMap<String, String>();
			metaDataMap.put(ConsentConstants.EventDataKey.TIME, time);
			consents.put(ConsentConstants.EventDataKey.METADATA, metaDataMap);
		}

		consentData.put(ConsentConstants.EventDataKey.CONSENTS, consents);
		return consentData;
	}

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

	static Event buildEdgeConsentPreferenceEvent(final String jsonString) throws JSONException {
		Map<String, Object> eventData = JSONUtils.toMap(new JSONObject(jsonString));
		return new Event.Builder("Edge Consent Preference", EventType.EDGE, EventSource.CONSENT_PREFERENCE)
			.setEventData(eventData)
			.build();
	}

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

	private static boolean isNullOrEmpty(final Map map) {
		return (map == null || map.isEmpty());
	}
}
