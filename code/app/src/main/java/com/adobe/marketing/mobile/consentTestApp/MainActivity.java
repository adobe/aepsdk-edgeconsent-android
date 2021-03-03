/* ******************************************************************************
 * ADOBE CONFIDENTIAL
 *  ___________________
 *
 *  Copyright 2021 Adobe
 *  All Rights Reserved.
 *
 *  NOTICE: All information contained herein is, and remains
 *  the property of Adobe and its suppliers, if any. The intellectual
 *  and technical concepts contained herein are proprietary to Adobe
 *  and its suppliers and are protected by all applicable intellectual
 *  property laws, including trade secret and copyright laws.
 *  Dissemination of this information or reproduction of this material
 *  is strictly forbidden unless prior written permission is obtained
 *  from Adobe.
 ******************************************************************************/

package com.adobe.marketing.mobile.consentTestApp;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.adobe.marketing.mobile.AdobeCallbackWithError;
import com.adobe.marketing.mobile.AdobeError;
import com.adobe.marketing.mobile.consent.Consent;

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
        Map<String,Object> consents = new HashMap<String, Object>() {{
            put("consents", new HashMap<String,Object>(){{
                put("collect", new HashMap<String,String>(){{
                    put("val","n");
                }});
            }});
        }};
        Consent.update(consents);
    }

    public void btnCollectYESClicked(View v) {
        Map<String,Object> consents = new HashMap<String, Object>() {{
            put("consents", new HashMap<String,Object>(){{
                put("collect", new HashMap<String,String>(){{
                    put("val","y");
                }});
            }});
        }};
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
