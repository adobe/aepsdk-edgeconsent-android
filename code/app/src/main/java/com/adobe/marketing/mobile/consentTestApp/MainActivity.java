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
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.adobe.marketing.mobile.AdobeCallback;
import com.adobe.marketing.mobile.consent.Consent;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnCollectYES = (Button) findViewById(R.id.btnCollectYES);
        Button btnCollectNO = (Button) findViewById(R.id.btnCollectNO);
        Button btnGetConsents = (Button) findViewById(R.id.btnGetConsents);
        final TextView txtViewConsents = (TextView) findViewById(R.id.txtViewConsents);


        /********* Collect Consent YES **********/
        btnCollectYES.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String,Object> consents = new HashMap<String, Object>() {{
                 put("consents", new HashMap<String,Object>(){{
                     put("collect", new HashMap<String,String>(){{
                         put("val","y");
                     }});
                 }});
                }};
                Consent.update(consents);
            }
        });

        /********* Collect Consent NO **********/
        btnCollectNO.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String,Object> consents = new HashMap<String, Object>() {{
                    put("consents", new HashMap<String,Object>(){{
                        put("collect", new HashMap<String,String>(){{
                            put("val","n");
                        }});
                    }});
                }};
                Consent.update(consents);
            }
        });

        /********* Get Consents**********/
        btnGetConsents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Consent.getConsents(new AdobeCallback<Map<String, Object>>() {
                    @Override
                    public void call(Map<String, Object> consents) {
                        txtViewConsents.setText(consents.toString());
                    }
                });
            }
        });


    }


}
