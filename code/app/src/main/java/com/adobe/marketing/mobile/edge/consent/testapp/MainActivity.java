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

package com.adobe.marketing.mobile.edge.consent.testapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.edge.consent.Consent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

	private LinearLayout llConsentRows;
	private List<View> consentRowViews;
	private int rowCounter = 0;

	// Consent type options
	private static final String[] CONSENT_TYPES = {
		"collect",
		"adId",
		"marketing.push",
		"marketing.email",
		"marketing.sms",
		"marketing.phone",
	};

	// Consent value options
	private static final String[] CONSENT_VALUES = { "yes", "no" };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		llConsentRows = findViewById(R.id.llConsentRows);
		consentRowViews = new ArrayList<>();

		// Make the consents TextView scrollable
		TextView txtViewConsents = findViewById(R.id.txtViewConsents);
		txtViewConsents.setMovementMethod(new android.text.method.ScrollingMovementMethod());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.app_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;

		if (item.getItemId() == R.id.connectToAssurance) {
			intent = new Intent(MainActivity.this, AssuranceActivity.class);
			startActivity(intent);
			return true;
		}

		if (item.getItemId() == R.id.testing) {
			intent = new Intent(MainActivity.this, TestingActivity.class);
			startActivity(intent);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void btnAddConsentClicked(View v) {
		addConsentRow();
	}

	public void btnUpdateConsentsClicked(View v) {
		updateConsents();
	}

	public void btnGetConsentsClicked(View v) {
		final TextView txtViewConsents = findViewById(R.id.txtViewConsents);
		Consent.getConsents(
			new AdobeCallbackWithError<Map<String, Object>>() {
				@Override
				public void call(Map<String, Object> consents) {
					String prettyPrinted = prettyPrintMap(consents);
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

	private String prettyPrintMap(Map<String, Object> map) {
		try {
			return new JSONObject(map == null ? new HashMap<>() : map).toString(2);
		} catch (Exception e) {
			return String.valueOf(map);
		}
	}

	public void btnDeleteClicked(View v) {
		View rowView = (View) v.getParent();
		llConsentRows.removeView(rowView);
		consentRowViews.remove(rowView);

		// Force the ScrollView to recalculate its layout
		llConsentRows.requestLayout();
		llConsentRows.invalidate();
	}

	private void addConsentRow() {
		LayoutInflater inflater = LayoutInflater.from(this);
		View rowView = inflater.inflate(R.layout.consent_preference_row, llConsentRows, false);

		// Set up consent type spinner
		Spinner spinnerConsentType = rowView.findViewById(R.id.spinnerConsentType);
		ArrayAdapter<String> consentTypeAdapter = new ArrayAdapter<>(
			this,
			android.R.layout.simple_spinner_item,
			CONSENT_TYPES
		);
		consentTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerConsentType.setAdapter(consentTypeAdapter);

		// Set up consent value spinner
		Spinner spinnerConsentValue = rowView.findViewById(R.id.spinnerConsentValue);
		ArrayAdapter<String> consentValueAdapter = new ArrayAdapter<>(
			this,
			android.R.layout.simple_spinner_item,
			CONSENT_VALUES
		);
		consentValueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerConsentValue.setAdapter(consentValueAdapter);

		// Set default selections
		spinnerConsentType.setSelection(0); // "collect"
		spinnerConsentValue.setSelection(0); // "yes"

		// Add the row to the layout
		llConsentRows.addView(rowView);
		consentRowViews.add(rowView);
		rowCounter++;
	}

	private void updateConsents() {
		if (consentRowViews.isEmpty()) {
			Log.d("MainActivity", "No consent preferences to update");
			return;
		}

		Map<String, Object> consents = new HashMap<>();
		Map<String, Object> consentPreferences = new HashMap<>();

		for (View rowView : consentRowViews) {
			Spinner spinnerConsentType = rowView.findViewById(R.id.spinnerConsentType);
			Spinner spinnerConsentValue = rowView.findViewById(R.id.spinnerConsentValue);

			String consentType = CONSENT_TYPES[spinnerConsentType.getSelectedItemPosition()];
			String consentValue = CONSENT_VALUES[spinnerConsentValue.getSelectedItemPosition()];

			// Create the consent value map
			Map<String, String> valueMap = new HashMap<>();
			valueMap.put("val", consentValue.equals("yes") ? "y" : "n");

			// Handle nested consent types (containing periods)
			if (consentType.contains(".")) {
				String[] parts = consentType.split("\\.");
				if (parts.length == 2) {
					// Create nested structure: {"parent": {"child": {"val": "y"}}}
					Map<String, Object> childMap = new HashMap<>();
					childMap.put(parts[1], valueMap);

					// Check if parent already exists in consentPreferences
					if (consentPreferences.containsKey(parts[0])) {
						// Parent exists, add child to existing parent map
						@SuppressWarnings("unchecked")
						Map<String, Object> existingParent = (Map<String, Object>) consentPreferences.get(parts[0]);
						existingParent.put(parts[1], valueMap);
					} else {
						// Parent doesn't exist, create new parent map
						consentPreferences.put(parts[0], childMap);
					}

					if ("marketing".equals(parts[0])) {
						@SuppressWarnings("unchecked")
						Map<String, Object> existingParent = (Map<String, Object>) consentPreferences.get(parts[0]);
						existingParent.put("preferred", "none");
					}
				}
			} else {
				// Simple consent type without periods
				consentPreferences.put(consentType, valueMap);
			}
		}

		consents.put("consents", consentPreferences);

		// Update consents
		Consent.update(consents);

		// Clear the UI
		clearConsentRows();
	}

	private void clearConsentRows() {
		llConsentRows.removeAllViews();
		consentRowViews.clear();
		rowCounter = 0;

		// Force the ScrollView to recalculate its layout
		llConsentRows.requestLayout();
		llConsentRows.invalidate();
	}
}
