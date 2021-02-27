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

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

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

import static com.adobe.marketing.mobile.consent.ConsentTestUtil.CreateConsentXDMMap;
import static com.adobe.marketing.mobile.consent.ConsentTestUtil.CreateConsentsXDMJSONString;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
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
    Application mockApplication;

    @Mock
    Context mockContext;

    @Mock
    ConsentManager mockConsentManager;

    @Mock
    SharedPreferences mockSharedPreference;

    @Mock
    SharedPreferences.Editor mockSharedPreferenceEditor;


    @Before
    public void setup() {
        PowerMockito.mockStatic(MobileCore.class);

        Mockito.when(MobileCore.getApplication()).thenReturn(mockApplication);
        Mockito.when(mockApplication.getApplicationContext()).thenReturn(mockContext);
        Mockito.when(mockContext.getSharedPreferences(ConsentConstants.DataStoreKey.DATASTORE_NAME, 0)).thenReturn(mockSharedPreference);
        Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);

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
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(2));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify the dispatched event
        // verify
        // Initial null and null
        // Updated YES and NO
        // Merged  YES and NO

        // verify consent response event dispatched
        Event consentResponseEvent = eventCaptor.getAllValues().get(0);
        assertEquals(consentResponseEvent.getName(), ConsentConstants.EventNames.CONSENT_PREFERENCES_UPDATED);
        assertEquals(consentResponseEvent.getType(), ConsentConstants.EventType.CONSENT.toLowerCase());
        assertEquals(consentResponseEvent.getSource(), ConsentConstants.EventSource.RESPONSE_CONTENT.toLowerCase());

        assertEquals("y", ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("collect")).get("val"));
        assertEquals("n", ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("adID")).get("val"));
        assertNotNull(((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("metadata")).get("time"));

        // verify edge consent event dispatched
        Event edgeConsentUpdateEvent = eventCaptor.getAllValues().get(1);
        assertEquals(edgeConsentUpdateEvent.getName(), ConsentConstants.EventNames.EDGE_CONSENT_UPDATE);
        assertEquals(edgeConsentUpdateEvent.getType(), ConsentConstants.EventType.EDGE.toLowerCase());
        assertEquals(edgeConsentUpdateEvent.getSource(), ConsentConstants.EventSource.UPDATE_CONSENT.toLowerCase());

        assertEquals("y", ((Map) ((Map) edgeConsentUpdateEvent.getEventData().get("consents")).get("collect")).get("val"));
        assertEquals("n", ((Map) ((Map) edgeConsentUpdateEvent.getEventData().get("consents")).get("adID")).get("val"));
        assertNotNull(((Map) ((Map) edgeConsentUpdateEvent.getEventData().get("consents")).get("metadata")).get("time"));
    }

    @Test
    public void test_handleConsentUpdate_MergesWithExistingConsents() {
        // setup
        setupExistingConsents(CreateConsentsXDMJSONString("n", "n"));
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);

        // test
        Event consentUpdateEvent = buildConsentUpdateEvent("y", null);
        extension.handleConsentUpdate(consentUpdateEvent); // send update event which overrides collect consent to YES

        // verify
        // Initial NO and No
        // Updated YES and null
        // Merged  YES and NO

        // verify dispatched event
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(2));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));

        // verify consent response event dispatched
        Event consentResponseEvent = eventCaptor.getAllValues().get(0);
        assertEquals("y", ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("collect")).get("val"));
        assertEquals("n", ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("adID")).get("val"));
        assertNotNull(((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("metadata")).get("time"));

        // verify edge consent event dispatched
        Event edgeConsentUpdateEvent = eventCaptor.getAllValues().get(1);
        assertEquals("y", ((Map) ((Map) edgeConsentUpdateEvent.getEventData().get("consents")).get("collect")).get("val"));
        assertEquals("n", ((Map) ((Map) edgeConsentUpdateEvent.getEventData().get("consents")).get("adID")).get("val"));
        assertNotNull(((Map) ((Map) edgeConsentUpdateEvent.getEventData().get("consents")).get("metadata")).get("time"));

        // verify XDM shared state
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(consentUpdateEvent), any(ExtensionErrorCallback.class));
        Map<String, Object> sharedState = sharedStateCaptor.getValue();
        assertEquals("y", ((Map) ((Map) sharedState.get("consents")).get("collect")).get("val"));
        assertEquals("n", ((Map) ((Map) sharedState.get("consents")).get("adID")).get("val"));
        assertNotNull(((Map) ((Map) sharedState.get("consents")).get("metadata")).get("time"));
    }

    @Test
    public void test_handleConsentUpdate_NullOrEmptyConsents() {
        // setup event with no valid consents
        setupExistingConsents(CreateConsentsXDMJSONString("n", "n"));

        // test
        extension.handleConsentUpdate(buildConsentUpdateEvent(null, null));

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
        extension.handleConsentUpdate(event);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }


    // ========================================================================================
    // private methods
    // ========================================================================================

    private void setupExistingConsents(final String jsonString) {
        Mockito.when(mockSharedPreference.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null)).thenReturn(jsonString);
        ConsentManager consentManager = new ConsentManager(); // loads the shared preference
        Whitebox.setInternalState(extension, "consentManager", consentManager);
    }

    private Event buildConsentUpdateEvent(final String collectConsentString, final String adIdConsentString) {
        Map<String, Object> eventData = CreateConsentXDMMap(collectConsentString, adIdConsentString);
        Event event = new Event.Builder("Consent Update", ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.UPDATE_CONSENT).setEventData(eventData).build();
        return event;
    }

}
