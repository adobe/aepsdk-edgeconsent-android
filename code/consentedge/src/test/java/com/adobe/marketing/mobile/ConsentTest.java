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

package com.adobe.marketing.mobile;

import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;


public class ConsentTest {
    static AsyncHelper asyncHelper = new AsyncHelper();

    @Mock
    private Application mockApplication;

    @Before
    public void setup() {
        MobileCore.setApplication(mockApplication);
        MobileCore.start(null);
    }

    @Test
    public void testRegistration() {
        // test
        Consent.registerExtension();
        asyncHelper.waitForAppThreads(500, true);

        // verify
        Map<String, Extension> extensions = getAllThirdpartyExtensions();
        Extension consent = extensions.get(ConsentTestConstants.EXTENSION_NAME);
        assertNotNull(consent);
    }

    // ========================================================================================
    // Private methods
    // ========================================================================================

    private static Map<String, Extension> getAllThirdpartyExtensions() {
        Map<String, Extension> thirdPartyExtensions = new HashMap<>();
        Collection<Module> allExtensions = MobileCore.getCore().eventHub.getActiveModules();
        for (Module currentModule : allExtensions) {
            if (currentModule instanceof ExtensionApi) {
                ExtensionApi extensionApi = (ExtensionApi) currentModule;
                Extension ext = extensionApi.getExtension();
                thirdPartyExtensions.put(ext.getName(), ext);
            }
        }
        return thirdPartyExtensions;
    }
}
