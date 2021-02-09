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
import java.util.concurrent.ConcurrentLinkedQueue;

import static junit.framework.TestCase.assertEquals;


public class ConsentExtensionTest {

    @Mock
    private Application mockApplication;


    static AsyncHelper asyncHelper = new AsyncHelper();

    @Before
    public void setup() {
        MobileCore.setApplication(mockApplication);
        MobileCore.start(null);
    }

    @Test
    public void test_ListenersRegistration() {
        Consent.registerExtension();
        asyncHelper.waitForAppThreads(500, true);

        ConcurrentLinkedQueue<EventListener> listeners = getAllListeners();
        assertEquals(2, listeners.size());
        EventListener listener1 = listeners.poll();
        assert listener1 != null;
        assertEquals(ConsentTestConstants.EventSource.UPDATE_CONSENT.toLowerCase(), listener1.getEventSource().getName());
        assertEquals(ConsentTestConstants.EventType.CONSENT.toLowerCase(), listener1.getEventType().getName());
        assertEquals("com.adobe.marketing.mobile.ConsentListenerConsentUpdateConsent", listener1.getClass().getName());

        EventListener listener2 = listeners.poll();
        assert listener2 != null;
        assertEquals(ConsentTestConstants.EventSource.CONSENT_PREFERENCE.toLowerCase(), listener2.getEventSource().getName());
        assertEquals(ConsentTestConstants.EventType.EDGE.toLowerCase(), listener2.getEventType().getName());
        assertEquals("com.adobe.marketing.mobile.ConsentListenerEdgeConsentPreference", listener2.getClass().getName());
    }


    // ========================================================================================
    // Private methods
    // ========================================================================================

    static ConcurrentLinkedQueue<EventListener> getAllListeners() {
        ConcurrentLinkedQueue<EventListener> listeners = new ConcurrentLinkedQueue<>();
        Collection<Module> allExtensions = MobileCore.getCore().eventHub.getActiveModules();

        for (Module currentModule : allExtensions) {
            if (currentModule instanceof ExtensionApi) {
                ExtensionApi extensionApi = (ExtensionApi) currentModule;
                listeners = MobileCore.getCore().eventHub.getModuleListeners(currentModule);
            }
        }

        return listeners;
    }
}
