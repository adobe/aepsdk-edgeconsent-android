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

import org.json.JSONException;
import org.json.JSONObject;
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
import static org.junit.Assert.assertTrue;
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

        // verify 3 listeners are registered
        verify(mockExtensionApi, times(3)).registerEventListener(anyString(),
                anyString(), any(Class.class), any(ExtensionErrorCallback.class));

        // verify listeners are registered with correct event source and type
        verify(mockExtensionApi, times(1)).registerEventListener(ArgumentMatchers.eq(ConsentConstants.EventType.CONSENT),
                ArgumentMatchers.eq(ConsentConstants.EventSource.UPDATE_CONSENT), eq(ListenerConsentUpdateConsent.class), any(ExtensionErrorCallback.class));
        verify(mockExtensionApi, times(1)).registerEventListener(eq(ConsentConstants.EventType.EDGE),
                eq(ConsentConstants.EventSource.CONSENT_PREFERENCE), eq(ListenerEdgeConsentPreference.class), callbackCaptor.capture());
        verify(mockExtensionApi, times(1)).registerEventListener(eq(ConsentConstants.EventType.CONSENT),
                eq(ConsentConstants.EventSource.REQUEST_CONTENT), eq(ListenerConsentRequestContent.class), callbackCaptor.capture());

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
    // handleRequestContent
    // ========================================================================================
    @Test
    public void test_handleRequestContent() {
        // setup
        setupExistingConsents(CreateConsentsXDMJSONString("n", "n"));
        Event event = new Event.Builder("Get Consent Request", ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.RESPONSE_CONTENT).setEventData(null).build();
        final ArgumentCaptor<Event> dispatchEventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Event> pairedEventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<ExtensionErrorCallback> errorCallbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // test
        extension.handleRequestContent(event);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchResponseEvent(dispatchEventCaptor.capture(), pairedEventCaptor.capture(), errorCallbackCaptor.capture());

        // verify that response for the request event is dispatched
        Event dispatchedEvent = dispatchEventCaptor.getValue();
        assertEquals(ConsentConstants.EventNames.GET_CONSENTS_RESPONSE,dispatchedEvent.getName());
        assertEquals(ConsentConstants.EventType.CONSENT.toLowerCase(),dispatchedEvent.getType());
        assertEquals(ConsentConstants.EventSource.RESPONSE_CONTENT.toLowerCase(),dispatchedEvent.getSource());
        assertEquals(CreateConsentXDMMap("n","n"),dispatchedEvent.getEventData());

        // verify that the request event is attached as the paired event for the response
        Event pairedEvent = pairedEventCaptor.getValue();
        assertEquals(pairedEvent,event);

        // TODO - enable when ExtensionError creation is available
        //extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
    }

    @Test
    public void test_handleRequestContent_NullCurrentConsents() {
        // setup
        Event event = new Event.Builder("Get Consent Request", ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.RESPONSE_CONTENT).setEventData(null).build();
        final ArgumentCaptor<Event> responseEventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<Event> requestEventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<ExtensionErrorCallback> errorCallbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // test
        extension.handleRequestContent(event);

        // verify
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchResponseEvent(responseEventCaptor.capture(), requestEventCaptor.capture(), errorCallbackCaptor.capture());

        // verify that the response event is dispatched with empty event data
        Event dispatchedEvent = responseEventCaptor.getValue();
        Map consentMap = (Map)dispatchedEvent.getEventData().get("consents");
        assertTrue(consentMap.isEmpty());
    }

    // ========================================================================================
    // handleEdgeConsentPreference
    // ========================================================================================
    @Test
    public void test_handleEdgeConsentPreference()throws Exception {
        // setup
        Event event = buildEdgeConsentPreferenceEvent("{\n" +
                "                            \"payload\": [\n" +
                "                                {\n" +
                "                                    \"collect\": {\n" +
                "                                        \"val\":\"y\"\n" +
                "                                    },\n" +
                "                                    \"adID\": {\n" +
                "                                        \"val\":\"n\"\n" +
                "                                    },\n" +
                "                                    \"personalize\": {\n" +
                "                                        \"content\": {\n" +
                "                                           \"val\": \"y\"\n" +
                "                                         }\n" +
                "                                    }\n" +
                "                                }\n" +
                "                            ],\n" +
                "                            \"type\": \"consent:preferences\"\n" +
                "                        }");
        final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleEdgeConsentPreference(event);


        // verify
        // Initial null and null and null
        // Updated  YES and   NO and  YES
        // Merged   YES and   NO and  YES

        // verify XDM shared state is set
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(event), any(ExtensionErrorCallback.class));
        Map<String, Object> sharedState = sharedStateCaptor.getValue();
        assertEquals("y", ((Map) ((Map) sharedState.get("consents")).get("collect")).get("val"));
        assertEquals("n", ((Map) ((Map) sharedState.get("consents")).get("adID")).get("val"));
        assertEquals("y", ((Map) ((Map) ((Map) sharedState.get("consents")).get("personalize")).get("content")).get("val"));
        assertNotNull(((Map) ((Map) sharedState.get("consents")).get("metadata")).get("time"));


        // verify consent response event is dispatched
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        Event consentResponseEvent = eventCaptor.getAllValues().get(0);
        assertEquals("y", ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("collect")).get("val"));
        assertEquals("n", ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("adID")).get("val"));
        assertEquals("y", ((Map) ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("personalize")).get("content")).get("val"));
        assertNotNull(((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("metadata")).get("time"));
    }

    @Test
    public void test_handleEdgeConsentPreference_MergesWithExistingConsents() throws Exception {
        // setup
        setupExistingConsents(CreateConsentsXDMJSONString("n", "n"));
        Event event = buildEdgeConsentPreferenceEvent("{\n" +
                "                            \"payload\": [\n" +
                "                                {\n" +
                "                                    \"collect\": {\n" +
                "                                        \"val\":\"y\"\n" +
                "                                    },\n" +
                "                                    \"personalize\": {\n" +
                "                                        \"content\": {\n" +
                "                                           \"val\": \"y\"\n" +
                "                                         }\n" +
                "                                    }\n" +
                "                                }\n" +
                "                            ],\n" +
                "                            \"type\": \"consent:preferences\"\n" +
                "                        }");
        final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        // test
        extension.handleEdgeConsentPreference(event);


        // verify
        // Initial  NO and   NO and null
        // Updated YES and null and YES
        // Merged  YES and   NO and YES

        // verify XDM shared state is set
        verify(mockExtensionApi, times(1)).setXDMSharedEventState(sharedStateCaptor.capture(), eq(event), any(ExtensionErrorCallback.class));
        Map<String, Object> sharedState = sharedStateCaptor.getValue();
        assertEquals("y", ((Map) ((Map) sharedState.get("consents")).get("collect")).get("val"));
        assertEquals("n", ((Map) ((Map) sharedState.get("consents")).get("adID")).get("val"));
        assertEquals("y", ((Map) ((Map) ((Map) sharedState.get("consents")).get("personalize")).get("content")).get("val"));
        assertNotNull(((Map) ((Map) sharedState.get("consents")).get("metadata")).get("time"));

        // verify consent response event is dispatched
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), any(ExtensionErrorCallback.class));
        Event consentResponseEvent = eventCaptor.getAllValues().get(0);
        assertEquals("y", ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("collect")).get("val"));
        assertEquals("n", ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("adID")).get("val"));
        assertEquals("y", ((Map) ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("personalize")).get("content")).get("val"));
        assertNotNull(((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("metadata")).get("time"));
    }

    @Test
    public void test_handleEdgeConsentPreference_InvalidPayload() throws Exception{
        // test
        extension.handleEdgeConsentPreference(buildEdgeConsentPreferenceEvent("{\n" +
                "  \"payload\": {\n" +
                "    \"adId\": {\n" +
                "      \"val\": \"n\"\n" +
                "    }\n" +
                "  }\n" +
                "}"));

        // verify shared state is not set
        verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
        PowerMockito.verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_handleEdgeConsentPreference_NullEventData() {
        // setup
        Event event = new Event.Builder("Edge Consent Response", ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.UPDATE_CONSENT).setEventData(null).build();

        // test
        extension.handleEdgeConsentPreference(event);

        // verify shared state is not set
        verify(mockExtensionApi, times(0)).setXDMSharedEventState(any(Map.class), any(Event.class), any(ExtensionErrorCallback.class));
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
        return new Event.Builder("Consent Update", ConsentConstants.EventType.CONSENT, ConsentConstants.EventSource.UPDATE_CONSENT).setEventData(eventData).build();
    }

    private Event buildEdgeConsentPreferenceEvent(final String jsonString) throws JSONException {
        Map<String, Object> eventData = Utility.toMap(new JSONObject(jsonString));
        return new Event.Builder("Edge Consent Preference", ConsentConstants.EventType.EDGE, ConsentConstants.EventSource.CONSENT_PREFERENCE).setEventData(eventData).build();
    }

}
