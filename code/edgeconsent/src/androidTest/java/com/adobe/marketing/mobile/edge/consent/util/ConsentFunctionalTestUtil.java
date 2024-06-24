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

package com.adobe.marketing.mobile.edge.consent.util;

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.MobileCore;
import com.adobe.marketing.mobile.edge.consent.Consent;
import com.adobe.marketing.mobile.util.JSONUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;

public class ConsentFunctionalTestUtil {

	private static final String LOG_SOURCE = "ConsentFunctionalTestUtil";
	public static String SAMPLE_METADATA_TIMESTAMP = "2019-09-23T18:15:45Z";
	private static final String ADID = "adID";
	private static final String COLLECT = "collect";
	private static final String PERSONALIZE = "personalize";
	private static final String CONTENT = "content";
	private static final String VALUE = "val";
	private static final long REGISTRATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(2);

	/**
	 * A fully prepared valid consent JSON looks like : { "consents": { "adID": { "val": "n" },
	 * "collect": { "val": "y" }, "personalize": { "content": { "val":"y" } } "metadata": { "time":
	 * "2019-09-23T18:15:45Z" } } }
	 */
	public static String CreateConsentsXDMJSONString(
		final String collectConsentString,
		final String adIDConsentString,
		final String time
	) {
		return CreateConsentsXDMJSONString(collectConsentString, adIDConsentString, null, time);
	}

	public static String CreateConsentsXDMJSONString(
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

	public static Map<String, Object> CreateConsentXDMMap(final String collectConsentString) {
		return CreateConsentXDMMap(collectConsentString, null);
	}

	public static Map<String, Object> CreateConsentXDMMap(
		final String collectConsentString,
		final String adIDConsentString
	) {
		return CreateConsentXDMMap(collectConsentString, adIDConsentString, null);
	}

	public static Map<String, Object> CreateConsentXDMMap(
		final String collectConsentString,
		final String adIDConsentString,
		final String time
	) {
		return CreateConsentXDMMap(collectConsentString, adIDConsentString, null, time);
	}

	public static Map<String, Object> CreateConsentXDMMap(
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
			metaDataMap.put(ConsentTestConstants.EventDataKey.TIME, time);
			consents.put(ConsentTestConstants.EventDataKey.METADATA, metaDataMap);
		}

		consentData.put(ConsentTestConstants.EventDataKey.CONSENTS, consents);
		return consentData;
	}

	public static Map<String, Object> getConsentsSync() {
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

	public static void applyDefaultConsent(final Map defaultConsentMap) {
		HashMap<String, Object> config = new HashMap<String, Object>() {
			{
				put(ConsentTestConstants.ConfigurationKey.DEFAULT_CONSENT, defaultConsentMap);
			}
		};
		MobileCore.updateConfiguration(config);
	}

	public static Event buildEdgeConsentPreferenceEventWithConsents(final Map<String, Object> consents) {
		List<Map<String, Object>> payload = new ArrayList<>();
		payload.add((Map) (consents.get("consents")));
		Map<String, Object> eventData = new HashMap<>();
		eventData.put("payload", payload);
		eventData.put("type", "consent:preferences");
		return new Event.Builder("Edge Consent Preference", EventType.EDGE, EventSource.CONSENT_PREFERENCE)
			.setEventData(eventData)
			.build();
	}

	public static Event buildEdgeConsentPreferenceEvent(final String jsonString) throws JSONException {
		Map<String, Object> eventData = JSONUtils.toMap(new JSONObject(jsonString));
		return new Event.Builder("Edge Consent Preference", EventType.EDGE, EventSource.CONSENT_PREFERENCE)
			.setEventData(eventData)
			.build();
	}
}
