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

package com.adobe.marketing.mobile.edge.consent;

import static com.adobe.marketing.mobile.edge.consent.ConsentTestUtil.*;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.EventSource;
import com.adobe.marketing.mobile.EventType;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionEventListener;
import com.adobe.marketing.mobile.services.NamedCollection;
import com.adobe.marketing.mobile.util.JSONUtils;
import com.adobe.marketing.mobile.util.TimeUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConsentExtensionTest {

	private ConsentExtension extension;

	@Mock
	ExtensionApi mockExtensionApi;

	@Mock
	NamedCollection mockNamedCollection;

	@Before
	public void setup() {
		Mockito.reset(mockExtensionApi);
		Mockito.reset(mockNamedCollection);
		extension = new ConsentExtension(mockExtensionApi, mockNamedCollection);
	}

	// ========================================================================================
	// constructor
	// ========================================================================================
	@Test
	public void test_listenerRegistration() {
		ArgumentCaptor<String> eventTypeCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> eventSourceCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ExtensionEventListener> extensionEventListenerArgumentCaptor = ArgumentCaptor.forClass(
			ExtensionEventListener.class
		);
		extension.onRegistered();

		verify(mockExtensionApi, times(4))
			.registerEventListener(
				eventTypeCaptor.capture(),
				eventSourceCaptor.capture(),
				extensionEventListenerArgumentCaptor.capture()
			);

		// Extract captured values into lists
		List<String> eventTypes = eventTypeCaptor.getAllValues();
		List<String> eventSources = eventSourceCaptor.getAllValues();
		List<ExtensionEventListener> extensionEventListenerList = extensionEventListenerArgumentCaptor.getAllValues();

		// Verify: 1st Consent event listener
		assertEquals(EventType.EDGE, eventTypes.get(0));
		assertEquals(EventSource.CONSENT_PREFERENCE, eventSources.get(0));
		assertNotNull(extensionEventListenerList.get(0));

		// Verify: 2nd Consent event listener
		assertEquals(EventType.CONSENT, eventTypes.get(1));
		assertEquals(EventSource.UPDATE_CONSENT, eventSources.get(1));
		assertNotNull(extensionEventListenerList.get(1));

		// Verify: 3nd Consent event listener
		assertEquals(EventType.CONSENT, eventTypes.get(2));
		assertEquals(EventSource.REQUEST_CONTENT, eventSources.get(2));
		assertNotNull(extensionEventListenerList.get(2));

		// Verify: 4th Consent event listener
		assertEquals(EventType.CONFIGURATION, eventTypes.get(3));
		assertEquals(EventSource.RESPONSE_CONTENT, eventSources.get(3));
		assertNotNull(extensionEventListenerList.get(3));
	}

	@Test
	public void test_OnBootUp_SharesXDMSharedState() {
		// setup
		setupExistingConsents(CreateConsentsXDMJSONString("y"));
		ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<Event> sharedStateEventCaptor = ArgumentCaptor.forClass(Event.class);
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension = new ConsentExtension(mockExtensionApi, mockNamedCollection);
		extension.handleInitialization();

		verify(mockExtensionApi, times(1))
			.createXDMSharedState(sharedStateCaptor.capture(), sharedStateEventCaptor.capture());

		Map<String, Object> sharedState = sharedStateCaptor.getValue();
		assertEquals("y", ((Map) ((Map) sharedState.get("consents")).get("collect")).get("val"));

		// verify consent response event is dispatched
		verify(mockExtensionApi).dispatch(eventCaptor.capture());
		Event consentResponseEvent = eventCaptor.getAllValues().get(0);
		assertEquals(
			"y",
			((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("collect")).get("val")
		);
	}

	@Test
	public void test_OnBootUp_WhenNothingInPersistence_DoesNotShareXDMSharedState() {
		// setup
		setupExistingConsents(null);
		ArgumentCaptor<Event> dispatchedEventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension = new ConsentExtension(mockExtensionApi, mockNamedCollection);
		extension.handleInitialization();

		verify(mockExtensionApi, never()).createXDMSharedState(any(), any());
		verify(mockExtensionApi, never()).dispatch(dispatchedEventCaptor.capture());
	}

	//
	// ========================================================================================
	// getName
	// ========================================================================================
	@Test
	public void test_getName() {
		// test
		String moduleName = extension.getName();
		assertEquals("getName should return the correct module name", ConsentConstants.EXTENSION_NAME, moduleName);
	}

	//
	// ========================================================================================
	// getFriendlyName
	// ========================================================================================
	@Test
	public void test_getFriendlyName() {
		// test
		String moduleName = extension.getFriendlyName();
		assertEquals(
			"getFriendlyName should return the correct module name",
			ConsentConstants.FRIENDLY_NAME,
			moduleName
		);
	}

	// ========================================================================================
	// getVersion
	// ========================================================================================
	@Test
	public void test_getVersion() {
		// test
		String moduleVersion = extension.getVersion();
		assertEquals(
			"getVersion should return the correct module version",
			ConsentConstants.EXTENSION_VERSION,
			moduleVersion
		);
	}

	// ========================================================================================
	// handleConfigurationResponse
	// ========================================================================================
	@Test
	public void test_handleConfigurationResponse() throws Exception {
		// setup
		Event configEvent = buildConfigurationResponseEvent(CreateConsentsXDMJSONString("y"));
		ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleConfigurationResponse(configEvent);

		// verify XDM shared state is set
		verify(mockExtensionApi, times(1)).createXDMSharedState(sharedStateCaptor.capture(), eq(configEvent));

		Map<String, Object> sharedState = sharedStateCaptor.getValue();
		assertEquals("y", ((Map) ((Map) sharedState.get("consents")).get("collect")).get("val"));

		// verify consent response event is dispatched
		verify(mockExtensionApi).dispatch(eventCaptor.capture());
		Event consentResponseEvent = eventCaptor.getAllValues().get(0);
		assertEquals(
			"y",
			((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("collect")).get("val")
		);
	}

	@Test
	public void test_handleConfigurationResponse_multipleTimesWithSameDefaults() throws Exception {
		// setup
		Event configEvent = buildConfigurationResponseEvent(CreateConsentsXDMJSONString("y"));
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleConfigurationResponse(configEvent);
		extension.handleConfigurationResponse(configEvent);
		extension.handleConfigurationResponse(configEvent);

		// verify XDM shared state is set
		verify(mockExtensionApi, times(1)).createXDMSharedState(any(Map.class), eq(configEvent));

		// verify consent response event is dispatched
		verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());
	}

	@Test
	public void test_handleConfigurationResponse_RemoveDefault() throws Exception {
		// setup
		Event configEvent = buildConfigurationResponseEvent(CreateConsentsXDMJSONString("y"));
		Event emptyConfigEvent = buildConfigurationResponseEvent("{}");
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleConfigurationResponse(configEvent);
		extension.handleConfigurationResponse(emptyConfigEvent);

		// verify XDM shared state is set twice with the correct data
		verify(mockExtensionApi, times(1)).createXDMSharedState(any(Map.class), eq(configEvent));
		verify(mockExtensionApi, times(1))
			.createXDMSharedState(eq(ConsentTestUtil.emptyConsentXDMMap()), eq(emptyConfigEvent));

		// verify consent response event is dispatched twice
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());
	}

	@Test
	public void test_handleConfigurationResponse_WhenNoEventData() {
		// setup
		Event configEvent = new Event.Builder(
			"Configuration Response Event",
			EventType.CONFIGURATION,
			EventSource.RESPONSE_CONTENT
		)
			.build();
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleConfigurationResponse(configEvent);

		// verify no shared state is set and no event is dispatched
		verify(mockExtensionApi, times(0)).createXDMSharedState(any(Map.class), any(Event.class));

		// verify no consent response event is dispatched
		verify(mockExtensionApi, times(0)).dispatch(eventCaptor.capture());
	}

	// ========================================================================================
	// handleConsentUpdate
	// ========================================================================================
	@Test
	public void test_handleConsentUpdate() {
		// setup
		Event event = buildConsentUpdateEvent("y", "n");
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleConsentUpdate(event);

		// verify
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

		// verify the dispatched event
		// verify
		// Initial null and null
		// Updated YES and NO
		// Merged  YES and NO

		// verify consent response event dispatched
		Event consentResponseEvent = eventCaptor.getAllValues().get(0);
		assertEquals(consentResponseEvent.getName(), ConsentConstants.EventNames.CONSENT_PREFERENCES_UPDATED);
		assertEquals(consentResponseEvent.getType(), EventType.CONSENT);
		assertEquals(consentResponseEvent.getSource(), EventSource.RESPONSE_CONTENT);

		assertEquals(
			"y",
			((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("collect")).get("val")
		);
		assertEquals("n", ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("adID")).get("val"));
		assertNotNull(((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("metadata")).get("time"));

		// verify edge consent event dispatched
		Event edgeConsentUpdateEvent = eventCaptor.getAllValues().get(1);
		assertEquals(edgeConsentUpdateEvent.getName(), ConsentConstants.EventNames.EDGE_CONSENT_UPDATE);
		assertEquals(edgeConsentUpdateEvent.getType(), EventType.EDGE);
		assertEquals(edgeConsentUpdateEvent.getSource(), EventSource.UPDATE_CONSENT);

		assertEquals(
			"y",
			((Map) ((Map) edgeConsentUpdateEvent.getEventData().get("consents")).get("collect")).get("val")
		);
		assertEquals("n", ((Map) ((Map) edgeConsentUpdateEvent.getEventData().get("consents")).get("adID")).get("val"));
		assertNotNull(
			((Map) ((Map) edgeConsentUpdateEvent.getEventData().get("consents")).get("metadata")).get("time")
		);
	}

	@Test
	public void test_handleConsentUpdate_MergesWithExistingConsents() {
		// setup
		setupExistingConsents(CreateConsentsXDMJSONString("n", "n"));
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);

		// test
		Event consentUpdateEvent = buildConsentUpdateEvent("y", null);
		extension.handleConsentUpdate(consentUpdateEvent); // send update event which overrides collect consent to YES

		// verify
		// Initial NO and NO
		// Updated YES and null
		// Merged  YES and NO

		// verify dispatched event
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

		// verify consent response event dispatched
		Event consentResponseEvent = eventCaptor.getAllValues().get(0);
		assertEquals(
			"y",
			((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("collect")).get("val")
		);
		assertEquals("n", ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("adID")).get("val"));
		assertNotNull(((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("metadata")).get("time"));

		// verify edge consent event dispatched (dispatches only the requested update consents)
		Event edgeConsentUpdateEvent = eventCaptor.getAllValues().get(1);
		assertEquals(
			"y",
			((Map) ((Map) edgeConsentUpdateEvent.getEventData().get("consents")).get("collect")).get("val")
		);
		assertNull(((Map) ((Map) edgeConsentUpdateEvent.getEventData().get("consents")).get("adID")));
		assertNotNull(
			((Map) ((Map) edgeConsentUpdateEvent.getEventData().get("consents")).get("metadata")).get("time")
		);

		// verify XDM shared state
		verify(mockExtensionApi, times(1)).createXDMSharedState(sharedStateCaptor.capture(), eq(consentUpdateEvent));
		Map<String, Object> sharedState = sharedStateCaptor.getValue();
		assertEquals("y", ((Map) ((Map) sharedState.get("consents")).get("collect")).get("val"));
		assertEquals("n", ((Map) ((Map) sharedState.get("consents")).get("adID")).get("val"));
		assertNotNull(((Map) ((Map) sharedState.get("consents")).get("metadata")).get("time"));
	}

	@Test
	public void test_handleConsentUpdate_NullOrEmptyConsents() {
		// setup event with no valid consents
		setupExistingConsents(CreateConsentsXDMJSONString("n", "n"));
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleConsentUpdate(buildConsentUpdateEvent(null, null));

		// verify
		// Initial NO and NO
		// Updated null and null
		// No edge update event dispatched
		verify(mockExtensionApi, times(0)).dispatch(eventCaptor.capture());
	}

	@Test
	public void test_handleConsentUpdate_NullEventData() {
		// setup
		Event event = new Event.Builder("Consent Response", EventType.CONSENT, EventSource.UPDATE_CONSENT)
			.setEventData(null)
			.build();
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleConsentUpdate(event);

		// verify
		verify(mockExtensionApi, times(0)).dispatch(eventCaptor.capture());
	}

	@Test
	public void test_handleConsentUpdate_SetsTimestamp() {
		// setup
		setupExistingConsents(CreateConsentsXDMJSONString("n", "n"));
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		Event consentUpdateEvent = buildConsentUpdateEvent("y", "n");
		extension.handleConsentUpdate(consentUpdateEvent);

		// verify
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

		// verify consent response event has correct timestamp
		Event consentResponseEvent = eventCaptor.getAllValues().get(0);
		Map<String, Object> metadata = (Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("metadata");
		final String expectedTimestamp = TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(consentUpdateEvent.getTimestamp()));
		assertEquals(expectedTimestamp, metadata.get("time"));
	}

	@Test
	public void test_handleConsentUpdate_MergesConsentsCorrectly() {
		// setup
		setupExistingConsents(CreateConsentsXDMJSONString("y", "n"));
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		Event consentUpdateEvent = buildConsentUpdateEvent("n", "y");
		extension.handleConsentUpdate(consentUpdateEvent);

		// verify
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

		// verify edge consent update event has merged consents
		Event edgeConsentEvent = eventCaptor.getAllValues().get(1);
		Map<String, Object> eventData = edgeConsentEvent.getEventData();
		Map<String, Object> consents = (Map) eventData.get("consents");
		Map<String, Object> collect = (Map) consents.get("collect");
		Map<String, Object> adID = (Map) consents.get("adID");

		assertEquals("n", collect.get("val"));
		assertEquals("y", adID.get("val"));
	}

	@Test
	public void test_handleConsentUpdate_SameConsentValuesNotDispatched() {
		// setup
		Event consentUpdateEvent = buildConsentUpdateEvent("y", "n");
		final String isoTimestamp = TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(consentUpdateEvent.getTimestamp()));

		setupExistingConsents(CreateConsentsXDMJSONString("y", "n", isoTimestamp));
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test - update with same values
		extension.handleConsentUpdate(consentUpdateEvent);

		// verify events are not dispatched
		verify(mockExtensionApi, times(0)).dispatch(eventCaptor.capture());
	}

	@Test
	public void test_handleConsentUpdate_SameConsentValuesDifferentTimestampNotDispatched() throws Exception {
		// setup
		Event consentUpdateEvent = buildConsentUpdateEvent("y", "n");
		final String isoTimestamp = TimeUtils.getISO8601UTCDateWithMilliseconds(new Date(consentUpdateEvent.getTimestamp() - 10000)); // move timestamp 10 seconds back

		setupExistingConsents(CreateConsentsXDMJSONString("y", "n", isoTimestamp));
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test - update with same values but different timestamps
		extension.handleConsentUpdate(consentUpdateEvent);

		// verify events are not dispatched
		verify(mockExtensionApi, times(0)).dispatch(eventCaptor.capture());
	}

	// ========================================================================================
	// handleRequestContent
	// ========================================================================================
	@Test
	public void test_handleRequestContent() {
		// setup
		setupExistingConsents(CreateConsentsXDMJSONString("n", "n"));

		Event event = new Event.Builder("Get Consent Request", EventType.CONSENT, EventSource.RESPONSE_CONTENT)
			.setEventData(null)
			.build();

		final ArgumentCaptor<Event> dispatchEventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleRequestContent(event);

		// verify
		verify(mockExtensionApi, times(1)).dispatch(dispatchEventCaptor.capture());

		// verify that response for the request event is dispatched
		Event dispatchedEvent = dispatchEventCaptor.getValue();

		assertEquals(ConsentConstants.EventNames.GET_CONSENTS_RESPONSE, dispatchedEvent.getName());
		assertEquals(EventType.CONSENT, dispatchedEvent.getType());
		assertEquals(EventSource.RESPONSE_CONTENT, dispatchedEvent.getSource());
		assertEquals(CreateConsentXDMMap("n", "n"), dispatchedEvent.getEventData());
	}

	@Test
	public void test_handleRequestContent_NullCurrentConsents() {
		// setup
		Event event = new Event.Builder("Get Consent Request", EventType.CONSENT, EventSource.RESPONSE_CONTENT)
			.setEventData(null)
			.build();

		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleRequestContent(event);

		// verify
		verify(mockExtensionApi, times(1)).dispatch(eventCaptor.capture());

		// verify that the response event is dispatched with empty event data
		Event dispatchedEvent = eventCaptor.getValue();
		Map consentMap = (Map) dispatchedEvent.getEventData().get("consents");
		assertTrue(consentMap.isEmpty());
	}

	// ========================================================================================
	// handleEdgeConsentPreferenceHandle
	// ========================================================================================
	@Test
	public void test_handleEdgeConsentPreferenceHandle() throws Exception {
		// setup
		Event event = buildEdgeConsentPreferenceEvent(
			"{\n" +
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
			"                        }"
		);
		final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleEdgeConsentPreferenceHandle(event);

		// verify
		// Initial null and null and null
		// Updated  YES and   NO and  YES
		// Merged   YES and   NO and  YES

		// verify XDM shared state is set
		verify(mockExtensionApi, times(1)).createXDMSharedState(sharedStateCaptor.capture(), eq(event));
		Map<String, Object> sharedState = sharedStateCaptor.getValue();
		assertEquals("y", ((Map) ((Map) sharedState.get("consents")).get("collect")).get("val"));
		assertEquals("n", ((Map) ((Map) sharedState.get("consents")).get("adID")).get("val"));
		assertEquals(
			"y",
			((Map) ((Map) ((Map) sharedState.get("consents")).get("personalize")).get("content")).get("val")
		);
		assertNotNull(((Map) ((Map) sharedState.get("consents")).get("metadata")).get("time"));

		// verify consent response event is dispatched
		verify(mockExtensionApi).dispatch(eventCaptor.capture());
		Event consentResponseEvent = eventCaptor.getAllValues().get(0);
		assertEquals(
			"y",
			((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("collect")).get("val")
		);
		assertEquals("n", ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("adID")).get("val"));
		assertEquals(
			"y",
			(
				(Map) ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("personalize")).get(
						"content"
					)
			).get("val")
		);
		assertNotNull(((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("metadata")).get("time"));
	}

	@Test
	public void test_handleEdgeConsentPreferenceHandle_MergesWithExistingConsents() throws Exception {
		// setup
		setupExistingConsents(CreateConsentsXDMJSONString("n", "n"));
		Event event = buildEdgeConsentPreferenceEvent(
			"{\n" +
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
			"                        }"
		);
		final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleEdgeConsentPreferenceHandle(event);

		// verify
		// Initial  NO and   NO and null
		// Updated YES and null and YES
		// Merged  YES and   NO and YES

		// verify XDM shared state is set
		verify(mockExtensionApi, times(1)).createXDMSharedState(sharedStateCaptor.capture(), eq(event));
		Map<String, Object> sharedState = sharedStateCaptor.getValue();
		assertEquals("y", ((Map) ((Map) sharedState.get("consents")).get("collect")).get("val"));
		assertEquals("n", ((Map) ((Map) sharedState.get("consents")).get("adID")).get("val"));
		assertEquals(
			"y",
			((Map) ((Map) ((Map) sharedState.get("consents")).get("personalize")).get("content")).get("val")
		);
		assertNotNull(((Map) ((Map) sharedState.get("consents")).get("metadata")).get("time"));

		// verify consent response event is dispatched
		verify(mockExtensionApi).dispatch(eventCaptor.capture());
		Event consentResponseEvent = eventCaptor.getAllValues().get(0);
		assertEquals(
			"y",
			((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("collect")).get("val")
		);
		assertEquals("n", ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("adID")).get("val"));
		assertEquals(
			"y",
			(
				(Map) ((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("personalize")).get(
						"content"
					)
			).get("val")
		);
		assertNotNull(((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("metadata")).get("time"));
	}

	@Test
	public void test_handleEdgeConsentPreferenceHandle_SameConsentAndTimeStamp() throws Exception {
		// setup
		setupExistingConsents(CreateConsentsXDMJSONString("y", "y", "sometime"));
		Event event = buildEdgeConsentPreferenceEventWithConsents(CreateConsentXDMMap("y", "y", "sometime"));

		// test
		extension.handleEdgeConsentPreferenceHandle(event);

		// verify
		verifyNoSharedStateChange();
		verifyNoEventDispatched();
	}

	@Test
	public void test_handleEdgeConsentPreferenceHandle_SameConsentAndNoTimeStamp() throws Exception {
		// setup
		setupExistingConsents(CreateConsentsXDMJSONString("y", "y", "sometime"));
		Event event = buildEdgeConsentPreferenceEventWithConsents(CreateConsentXDMMap("y", "y"));

		// test
		extension.handleEdgeConsentPreferenceHandle(event);

		// verify
		verifyNoSharedStateChange();
		verifyNoEventDispatched();
	}

	@Test
	public void test_handleEdgeConsentPreferenceHandle_SameConsentAndDifferentTimeStamp() throws Exception {
		// setup
		setupExistingConsents(CreateConsentsXDMJSONString("y", "y", "sometime"));
		Event event = buildEdgeConsentPreferenceEventWithConsents(CreateConsentXDMMap("y", "y", "different"));
		final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		extension.handleEdgeConsentPreferenceHandle(event);

		// verify XDM shared state is set
		verify(mockExtensionApi, times(1)).createXDMSharedState(sharedStateCaptor.capture(), eq(event));
		Map<String, Object> sharedState = sharedStateCaptor.getValue();
		assertNotNull(((Map) ((Map) sharedState.get("consents")).get("metadata")).get("time"));

		// verify consent response event is dispatched
		verify(mockExtensionApi).dispatch(eventCaptor.capture());
		Event consentResponseEvent = eventCaptor.getAllValues().get(0);
		assertNotNull(((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("metadata")).get("time"));
	}

	@Test
	public void test_handleEdgeConsentPreferenceHandle_SameConsentAndDifferentMetadataWithNoTimeStamp()
		throws Exception {
		// setup
		setupExistingConsents(CreateConsentsXDMJSONString("y", "y", "sometime"));
		Event event = buildEdgeConsentPreferenceEvent(
			"{\n" +
			"  \"payload\": [\n" +
			"    {\n" +
			"      \"collect\": {\n" +
			"        \"val\": \"y\"\n" +
			"      },\n" +
			"      \"adID\": {\n" +
			"        \"val\": \"y\"\n" +
			"      },\n" +
			"      \"metadata\": {\n" +
			"        \"key\": \"value\"\n" +
			"      }\n" +
			"    }\n" +
			"  ],\n" +
			"  \"type\": \"consent:preferences\"\n" +
			"}"
		);

		// test
		extension.handleEdgeConsentPreferenceHandle(event);
		final ArgumentCaptor<Map> sharedStateCaptor = ArgumentCaptor.forClass(Map.class);
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// verify
		// verify XDM shared state is set
		verify(mockExtensionApi, times(1)).createXDMSharedState(sharedStateCaptor.capture(), eq(event));
		Map<String, Object> sharedState = sharedStateCaptor.getValue();
		assertNotNull(((Map) ((Map) sharedState.get("consents")).get("metadata")).get("time"));

		// verify consent response event is dispatched
		verify(mockExtensionApi).dispatch(eventCaptor.capture());
		Event consentResponseEvent = eventCaptor.getAllValues().get(0);
		assertNotNull(((Map) ((Map) consentResponseEvent.getEventData().get("consents")).get("metadata")).get("time"));
	}

	@Test
	public void test_handleEdgeConsentPreferenceHandle_InvalidPayload() throws Exception {
		// test
		extension.handleEdgeConsentPreferenceHandle(
			buildEdgeConsentPreferenceEvent(
				"{\n" +
				"  \"payload\": {\n" +
				"    \"adId\": {\n" +
				"      \"val\": \"n\"\n" +
				"    }\n" +
				"  }\n" +
				"}"
			)
		);

		// verify
		verifyNoSharedStateChange();
		verifyNoEventDispatched();
	}

	@Test
	public void test_handleEdgeConsentPreferenceHandle_NullEventData() {
		// setup
		Event event = new Event.Builder("Edge Consent Response", EventType.CONSENT, EventSource.UPDATE_CONSENT)
			.setEventData(null)
			.build();

		// test
		extension.handleEdgeConsentPreferenceHandle(event);

		// verify
		verifyNoSharedStateChange();
		verifyNoEventDispatched();
	}

	// ========================================================================================
	// private methods
	// ========================================================================================

	/**
	 * Sets up existing consents values in persistence by setting up a mocked return value for the
	 * mocked {@link NamedCollection} using the passed JSON consents {@link String}.
	 *
	 * <p>Note that the {@link ConsentManager} only fetches the values from persistence once at
	 * instantiation time, so this method <b>recreates the {@link ConsentExtension} instance used in
	 * the test run</b>. However, it does not reset the mocks themselves.
	 *
	 * @param jsonString the consents JSON string to set in mocked persistence
	 */
	private void setupExistingConsents(final String jsonString) {
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.CONSENT_PREFERENCES, null))
			.thenReturn(jsonString);
		extension = new ConsentExtension(mockExtensionApi, mockNamedCollection);
	}

	private Event buildConsentUpdateEvent(final String collectConsentString, final String adIdConsentString) {
		Map<String, Object> eventData = CreateConsentXDMMap(collectConsentString, adIdConsentString);
		return new Event.Builder("Consent Update", EventType.CONSENT, EventSource.UPDATE_CONSENT)
			.setEventData(eventData)
			.build();
	}

	private Event buildEdgeConsentPreferenceEvent(final String jsonString) throws JSONException {
		Map<String, Object> eventData = JSONUtils.toMap(new JSONObject(jsonString));
		return new Event.Builder("Edge Consent Preference", EventType.EDGE, EventSource.CONSENT_PREFERENCE)
			.setEventData(eventData)
			.build();
	}

	private Event buildConfigurationResponseEvent(final String jsonString) throws JSONException {
		final Map<String, Object> consentMap = JSONUtils.toMap(new JSONObject(jsonString));
		Map<String, Object> configEventData = new HashMap<String, Object>() {
			{
				put(ConsentConstants.ConfigurationKey.DEFAULT_CONSENT, consentMap);
			}
		};
		return new Event.Builder("Configuration Response Event", EventType.CONFIGURATION, EventSource.RESPONSE_CONTENT)
			.setEventData(configEventData)
			.build();
	}

	private void verifyNoSharedStateChange() {
		verify(mockExtensionApi, times(0)).createXDMSharedState(any(Map.class), any(Event.class));
	}

	private void verifyNoEventDispatched() {
		ArgumentCaptor<Event> eventCaptor2 = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi, times(0)).dispatch(any(Event.class));
	}
}
