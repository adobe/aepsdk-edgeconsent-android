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

package com.adobe.marketing.mobile.consentTestApp;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.edge.consent.Consent;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

	}

	// Implement the OnClickListener callback
	public void btnCollectNoClicked(View v) {
		Map<String, Object> consents = new HashMap<String, Object>() {
			{
				put("consents", new HashMap<String, Object>() {
					{
						put("collect", new HashMap<String, String>() {
							{
								put("val", "n");
							}
						});
					}
				});
			}
		};
		Consent.update(consents);
	}

	public void btnCollectYESClicked(View v) {
		Map<String, Object> consents = new HashMap<String, Object>() {
			{
				put("consents", new HashMap<String, Object>() {
					{
						put("collect", new HashMap<String, String>() {
							{
								put("val", "y");
							}
						});
					}
				});
			}
		};
		Consent.update(consents);
	}

	public void btnGetConsentsClicked(View v) {
		final TextView txtViewConsents = (TextView) findViewById(R.id.txtViewConsents);
		Consent.getConsents(new AdobeCallbackWithError<Map<String, Object>>() {
			@Override
			public void call(Map<String, Object> consents) {
				txtViewConsents.setText(consents.toString());
			}

			@Override
			public void fail(AdobeError adobeError) {
				Log.d(this.getClass().getName(), String.format("GetConsents failed with error - %s", adobeError.getErrorName()));
			}
		});
	}


}
