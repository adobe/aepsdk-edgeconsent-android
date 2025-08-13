/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.edge.consent.testapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.edge.consent.Consent;
import java.util.HashMap;
import java.util.Map;

public class TestingActivity extends AppCompatActivity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_testing);
	}

	public void btnTestRepeatedSameConsentsClicked(View v) {
		// Expect only the first update to send a network request to Adobe Experience Platform.
		// The other requests will log that the requests are ignored.
		final Map<String, Object> preferences = new HashMap<String, Object>() {
			{
				put(
					"consents",
					new HashMap<String, Object>() {
						{
							put(
								"marketing",
								new HashMap<String, Object>() {
									{
										put("preferred", "none");
										put(
											"push",
											new HashMap<String, Object>() {
												{
													put("val", "y");
												}
											}
										);
									}
								}
							);
						}
					}
				);
			}
		};

		Consent.update(preferences);
		Consent.update(preferences);
		Consent.update(preferences);
		Consent.update(preferences);
	}

	public void btnTestRepeatedDifferentConsentsClicked(View v) {
		// Expect all 4 updates to send network requests to Adobe Experience Platform.
		// The resulting consent preferences should contain:
		// ["consents": [
		//    "marketing": [
		//        "preferred": "email",
		//        "push": ["val": "y"],
		//        "email": ["val": "y"],
		//        "sms": ["val": "n"],
		//    ]
		// ]]

		final Map<String, Object> one = new HashMap<String, Object>() {
			{
				put(
					"consents",
					new HashMap<String, Object>() {
						{
							put(
								"marketing",
								new HashMap<String, Object>() {
									{
										put("preferred", "none");
										put(
											"push",
											new HashMap<String, Object>() {
												{
													put("val", "y");
												}
											}
										);
									}
								}
							);
						}
					}
				);
			}
		};
		Consent.update(one);

		final Map<String, Object> two = new HashMap<String, Object>() {
			{
				put(
					"consents",
					new HashMap<String, Object>() {
						{
							put(
								"marketing",
								new HashMap<String, Object>() {
									{
										put("preferred", "none");
										put(
											"email",
											new HashMap<String, Object>() {
												{
													put("val", "y");
												}
											}
										);
									}
								}
							);
						}
					}
				);
			}
		};
		Consent.update(two);

		final Map<String, Object> three = new HashMap<String, Object>() {
			{
				put(
					"consents",
					new HashMap<String, Object>() {
						{
							put(
								"marketing",
								new HashMap<String, Object>() {
									{
										put("preferred", "sms");
										put(
											"sms",
											new HashMap<String, Object>() {
												{
													put("val", "y");
												}
											}
										);
									}
								}
							);
						}
					}
				);
			}
		};
		Consent.update(three);

		final Map<String, Object> four = new HashMap<String, Object>() {
			{
				put(
					"consents",
					new HashMap<String, Object>() {
						{
							put(
								"marketing",
								new HashMap<String, Object>() {
									{
										put("preferred", "email");
										put(
											"sms",
											new HashMap<String, Object>() {
												{
													put("val", "n");
												}
											}
										);
									}
								}
							);
						}
					}
				);
			}
		};
		Consent.update(four);
	}

	public void btnTestRepeatedMixedConsentsClicked(View v) {
		// Expect only first 2 updates to send network requests to Adobe Experience Platform.
		// The last two requests do not change the preferences and are therefore ignored.

		final Map<String, Object> one = new HashMap<String, Object>() {
			{
				put(
					"consents",
					new HashMap<String, Object>() {
						{
							put(
								"marketing",
								new HashMap<String, Object>() {
									{
										put("preferred", "none");
										put(
											"push",
											new HashMap<String, Object>() {
												{
													put("val", "n");
												}
											}
										);
									}
								}
							);
						}
					}
				);
			}
		};

		final Map<String, Object> two = new HashMap<String, Object>() {
			{
				put(
					"consents",
					new HashMap<String, Object>() {
						{
							put(
								"marketing",
								new HashMap<String, Object>() {
									{
										put("preferred", "none");
										put(
											"email",
											new HashMap<String, Object>() {
												{
													put("val", "n");
												}
											}
										);
									}
								}
							);
						}
					}
				);
			}
		};

		Consent.update(one);
		Consent.update(two);
		Consent.update(one);
		Consent.update(two);
	}

	public void btnGetConsentsClicked(View v) {
		final TextView txtViewConsents = findViewById(R.id.txtViewConsents);
		Consent.getConsents(
			new AdobeCallbackWithError<Map<String, Object>>() {
				@Override
				public void call(Map<String, Object> consents) {
					String prettyPrinted = prettyPrintMap(consents, 0);
					txtViewConsents.setText(prettyPrinted);
				}

				@Override
				public void fail(AdobeError adobeError) {
					Log.d(
						this.getClass().getName(),
						String.format("GetConsents failed with error - %s", adobeError.getErrorName())
					);
				}
			}
		);
	}

	private String prettyPrintMap(Map<String, Object> map, int indentLevel) {
		if (map == null || map.isEmpty()) {
			return "{}";
		}

		StringBuilder sb = new StringBuilder();
		String indent = "  ".repeat(indentLevel);
		String nextIndent = "  ".repeat(indentLevel + 1);

		sb.append("{\n");

		int count = 0;
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			sb.append(nextIndent).append("\"").append(entry.getKey()).append("\": ");

			Object value = entry.getValue();
			if (value instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, Object> nestedMap = (Map<String, Object>) value;
				sb.append(prettyPrintMap(nestedMap, indentLevel + 1));
			} else {
				sb.append("\"").append(value.toString()).append("\"");
			}

			if (++count < map.size()) {
				sb.append(",");
			}
			sb.append("\n");
		}

		sb.append(indent).append("}");
		return sb.toString();
	}
}
