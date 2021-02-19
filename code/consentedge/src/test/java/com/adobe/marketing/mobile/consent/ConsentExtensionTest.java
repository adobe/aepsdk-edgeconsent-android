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

package com.adobe.marketing.mobile.consent;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.MobileCore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.Map;

import static com.adobe.marketing.mobile.consent.ConsentTestUtil.CreateConsentDataMap;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Event.class, MobileCore.class, ExtensionApi.class})
public class ConsentExtensionTest {
    private ConsentExtension extension;

    @Mock
    ExtensionApi mockExtensionApi;

    @Mock
    ConsentManager mockConsentManager;


    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);
        extension = new ConsentExtension(mockExtensionApi);
    }

    // ========================================================================================
    // constructor
    // ========================================================================================
    @Test
    public void test_ListenersRegistration() {
        // setup
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // test
        // constructor is called in the setup step()

        // verify 2 listeners are registered
        verify(mockExtensionApi, times(2)).registerEventListener(anyString(),
                anyString(), any(Class.class), any(ExtensionErrorCallback.class));

        // verify listeners are registered with correct event source and type
        verify(mockExtensionApi, times(1)).registerEventListener(ArgumentMatchers.eq(ConsentConstants.EventType.CONSENT),
                ArgumentMatchers.eq(ConsentConstants.EventSource.UPDATE_CONSENT), eq(ConsentListenerConsentUpdateConsent.class), any(ExtensionErrorCallback.class));
        verify(mockExtensionApi, times(1)).registerEventListener(eq(ConsentConstants.EventType.EDGE),
                eq(ConsentConstants.EventSource.CONSENT_PREFERENCE), eq(ConsentListenerEdgeConsentPreference.class), callbackCaptor.capture());

        // verify the callback
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
        Assert.assertNotNull("The extension callback should not be null", extensionErrorCallback);

        // TODO - enable when ExtensionError creation is available
        //extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
    }

    // ========================================================================================
    // getName
    // ========================================================================================
    @Test
    public void test_getName() {
        // test
        String moduleName = extension.getName();
        assertEquals("getName should return the correct module name", ConsentConstants.EXTENSION_NAME, moduleName);
    }

    // ========================================================================================
    // getVersion
    // ========================================================================================
    @Test
    public void test_getVersion() {
        // test
        String moduleVersion = extension.getVersion();
        assertEquals("getVersion should return the correct module version", ConsentConstants.EXTENSION_VERSION,
                moduleVersion);
    }

    // ========================================================================================
    // handleConsentUpdate
    // ========================================================================================
    @Test
    public void test_handleConsentUpdate() {
        // setup
        Event event = buildConsentUpdateEvent("y", "n");
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleConsentUpdate(event);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify the dispatched event
        // verify
        // Initial null and null
        // Updated YES and NO
        // Merged  YES and NO
        Event dispatchedEvent = eventCaptor.getValue();
        assertEquals(dispatchedEvent.getName(), ConsentConstants.EventNames.EDGE_CONSENT_UPDATE);
        assertEquals(dispatchedEvent.getType(), ConsentConstants.EventType.EDGE.toLowerCase());
        assertEquals(dispatchedEvent.getSource(), ConsentConstants.EventSource.UPDATE_CONSENT.toLowerCase());
        assertEquals(dispatchedEvent.getEventData(), CreateConsentDataMap("y", "n"));
    }

    @Test
    public void test_handleConsentUpdate_MergesWithExistingConsents() {
        // setup
        Whitebox.setInternalState(extension, "consentManager", mockConsentManager);
        Mockito.when(mockConsentManager.getCurrentConsents()).thenReturn(new Consents(CreateConsentDataMap("n", "n"))); // set existingConsent to No and No
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleConsentUpdate(buildConsentUpdateEvent("y", null)); // send second event which overrides collect consent to YES

        // verify
        // Initial NO and No
        // Updated YES and null
        // Merged  YES and NO
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        Event lastDispatchedEvent = eventCaptor.getValue();
        assertEquals(lastDispatchedEvent.getEventData(), CreateConsentDataMap("y", "n"));
    }

    @Test
    public void test_handleConsentUpdate_NullOrEmptyConsents() {
        // setup
        Whitebox.setInternalState(extension, "consentManager", mockConsentManager);
        Mockito.when(mockConsentManager.getCurrentConsents()).thenReturn(new Consents(CreateConsentDataMap("n", "n"))); // set existingConsent to No and No

        // test
        extension.handleConsentUpdate(buildConsentUpdateEvent(null, null)); // send second event which overrides collect consent to YES

        // verify
        // Initial NO and NO
        // Updated null and null
        // No edge update event  dispatched
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleConsentUpdate_NullEventData() {
        // setup
        Event event = new Event.Builder("Consent Response", ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.UPDATE_CONSENT).setEventData(null).build();

        // test
        extension.handleConsentUpdate(event); // send second event which overrides collect consent to YES

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }


    // ========================================================================================
    // private methods
    // ========================================================================================

    private Event buildConsentUpdateEvent(final String collectConsentString, final String adIdConsentString) {
        Map<String, Object> eventData = CreateConsentDataMap(collectConsentString, adIdConsentString);
        Event event = new Event.Builder("Consent Response", ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.UPDATE_CONSENT).setEventData(eventData).build();
        return event;
    }

}
