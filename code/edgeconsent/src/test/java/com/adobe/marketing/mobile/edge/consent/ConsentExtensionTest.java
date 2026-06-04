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
		setupExistingConsents(new ConsentsBuilder().setCollect("y").buildToString());
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
		Event configEvent = buildConfigurationResponseEvent(new ConsentsBuilder().setCollect("y").buildToString());
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
		Event configEvent = buildConfigurationResponseEvent(new ConsentsBuilder().setCollect("y").buildToString());
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
		Event configEvent = buildConfigurationResponseEvent(new ConsentsBuilder().setCollect("y").buildToString());
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
		setupExistingConsents(new ConsentsBuilder().setCollect("n").setAdId("n").buildToString());
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
		setupExistingConsents(new ConsentsBuilder().setCollect("n").setAdId("n").buildToString());
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
		setupExistingConsents(new ConsentsBuilder().setCollect("n").setAdId("n").buildToString());
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test
		Event consentUpdateEvent = buildConsentUpdateEvent("y", "n");
		extension.handleConsentUpdate(consentUpdateEvent);

		// verify
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

		// verify consent response event has correct timestamp
		Event consentResponseEvent = eventCaptor.getAllValues().get(0);
		Map<String, Object> metadata = (Map) ((Map) consentResponseEvent.getEventData().get("consents")).get(
				"metadata"
			);
		final String expectedTimestamp = TimeUtils.getISO8601UTCDateWithMilliseconds(
			new Date(consentUpdateEvent.getTimestamp())
		);
		assertEquals(expectedTimestamp, metadata.get("time"));
	}

	@Test
	public void test_handleConsentUpdate_MergesConsentsCorrectly() {
		// setup
		setupExistingConsents(new ConsentsBuilder().setCollect("y").setAdId("n").buildToString());
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
	public void test_handleConsentUpdate_doesNotDispatchEdgeEvent_ifConsentUpdateIsWithinIgnoreInterval() {
		// setup
		setupExistingConsents(new ConsentsBuilder().setCollect("y").setAdId("n").buildToString());

		Event consentUpdateEvent = buildConsentUpdateEvent("y", "n");
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test, send consent update event with same values
		extension.handleConsentUpdate(consentUpdateEvent);

		// verify, expect 2 events to be dispatched
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

		// test, send consent update event with same values
		extension.handleConsentUpdate(consentUpdateEvent);

		// verify, expect no new events to be dispatched as the timestamp is within the ignore interval
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());
	}

	@Test
	public void test_handleConsentUpdate_doesDispatchEdgeEvent_ifConsentUpdateIsWithinIgnoreIntervalButPreferenceIsDifferent() {
		// setup
		setupExistingConsents(new ConsentsBuilder().setCollect("y").setAdId("n").buildToString());

		Event consentUpdateEvent1 = buildConsentUpdateEvent("y", "n");
		Event consentUpdateEvent2 = buildConsentUpdateEvent("y", "y");
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test, send consent update event with same values
		extension.handleConsentUpdate(consentUpdateEvent1);

		// verify, expect 2 events to be dispatched
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

		// test, send consent update event with different values
		extension.handleConsentUpdate(consentUpdateEvent2);

		// verify, expect new events to be dispatched as the preferences have changed
		verify(mockExtensionApi, times(4)).dispatch(eventCaptor.capture());
	}

	@Test
	public void test_handleConsentUpdate_dispatchesEdgeEvent_ifConsentUpdateIsOutsideIgnoreInterval() throws Exception {
		// setup
		setupExistingConsents(new ConsentsBuilder().setCollect("y").setAdId("n").buildToString());

		Event consentUpdateEvent = buildConsentUpdateEvent("y", "n");
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

		// test, send initial consent update request
		extension.handleConsentUpdate(consentUpdateEvent);

		// verify, expect 2 events to be dispatched
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

		// test, send consent update event with same values, timestamp 1000 ms later
		Thread.sleep(1100); // sleep to add time to next event
		Event repeatConsentUpdateEvent = buildConsentUpdateEvent("y", "n");
		extension.handleConsentUpdate(repeatConsentUpdateEvent);

		// verify, expect new events to be dispatched as the timestamp is outside the ignore interval
		verify(mockExtensionApi, times(4)).dispatch(eventCaptor.capture());
	}

	// ========================================================================================
	// handleRequestContent
	// ========================================================================================
	@Test
	public void test_handleRequestContent() {
		// setup
		ConsentsBuilder consentsBuilder = new ConsentsBuilder().setCollect("n").setAdId("n");
		setupExistingConsents(consentsBuilder.buildToString());

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
		assertEquals(consentsBuilder.buildToMap(), dispatchedEvent.getEventData());
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
		setupExistingConsents(new ConsentsBuilder().setCollect("n").setAdId("n").buildToString());
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
		ConsentsBuilder consentsBuilder = new ConsentsBuilder().setCollect("y").setAdId("y").setTime("sometime");
		setupExistingConsents(consentsBuilder.buildToString());
		Event event = buildEdgeConsentPreferenceEventWithConsents(consentsBuilder.buildToMap());

		// test
		extension.handleEdgeConsentPreferenceHandle(event);

		// verify
		verifyNoSharedStateChange();
		verifyNoEventDispatched();
	}

	@Test
	public void test_handleEdgeConsentPreferenceHandle_SameConsentAndNoTimeStamp() throws Exception {
		// setup
		ConsentsBuilder consentsBuilder = new ConsentsBuilder().setCollect("y").setAdId("y").setTime("sometime");
		setupExistingConsents(consentsBuilder.buildToString());
		Event event = buildEdgeConsentPreferenceEventWithConsents(consentsBuilder.setTime(null).buildToMap());

		// test
		extension.handleEdgeConsentPreferenceHandle(event);

		// verify
		verifyNoSharedStateChange();
		verifyNoEventDispatched();
	}

	@Test
	public void test_handleEdgeConsentPreferenceHandle_SameConsentAndDifferentTimeStamp() throws Exception {
		// setup
		ConsentsBuilder consentsBuilder = new ConsentsBuilder().setCollect("y").setAdId("y").setTime("sometime");
		setupExistingConsents(consentsBuilder.buildToString());
		Event event = buildEdgeConsentPreferenceEventWithConsents(consentsBuilder.setTime("different").buildToMap());
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
		setupExistingConsents(new ConsentsBuilder().setCollect("y").setAdId("y").setTime("sometime").buildToString());
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
		Map<String, Object> eventData = new ConsentsBuilder()
			.setCollect(collectConsentString)
			.setAdId(adIdConsentString)
			.buildToMap();
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

	// ========================================================================================
	// collectConsentResyncRequired flag in dispatched CONSENT_PREFERENCES_UPDATED events
	// ========================================================================================

	/**
	 * Public-API path: an "n" -> "y" sequence must produce CONSENT_PREFERENCES_UPDATED
	 * events; the one corresponding to the n->y transition carries the flag, others do not.
	 */
	@Test
	public void test_consentUpdate_collectYesFromN_dispatchesPreferencesUpdatedWithFlag() {
		// First update: collect = "n" — establishes lastDefinitive = "n"
		setupExistingConsents(null);
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn(null);
		extension.handleConsentUpdate(buildConsentUpdateEvent("n", null));

		ArgumentCaptor<Event> firstCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi, times(2)).dispatch(firstCaptor.capture());

		Event firstPrefsUpdated = firstCaptor.getAllValues().get(0);
		assertEquals(ConsentConstants.EventNames.CONSENT_PREFERENCES_UPDATED, firstPrefsUpdated.getName());
		assertNull(
			"First 'n' event must not carry the flag",
			firstPrefsUpdated.getEventData().get(ConsentConstants.EventDataKey.COLLECT_CONSENT_RESYNC_REQUIRED)
		);

		// Now simulate the persisted lastDefinitive flipping to "n" (which the previous merge wrote)
		Mockito.reset(mockExtensionApi);
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("n");
		// Recreate extension so ConsentManager re-reads current state with lastDefinitive = "n"
		setupExistingConsents(new ConsentsBuilder().setCollect("n").buildToString());

		// Second update: collect = "y" — should fire the flag
		extension.handleConsentUpdate(buildConsentUpdateEvent("y", null));

		ArgumentCaptor<Event> secondCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi, times(2)).dispatch(secondCaptor.capture());
		Event secondPrefsUpdated = secondCaptor.getAllValues().get(0);
		assertEquals(ConsentConstants.EventNames.CONSENT_PREFERENCES_UPDATED, secondPrefsUpdated.getName());
		assertEquals(
			"n -> y transition must carry the flag",
			Boolean.TRUE,
			secondPrefsUpdated.getEventData().get(ConsentConstants.EventDataKey.COLLECT_CONSENT_RESYNC_REQUIRED)
		);
	}

	/**
	 * Repeated "y" updates: the persisted lastDefinitive is already "y", so the event
	 * must not carry the flag. (The first y-update still dispatches because the
	 * timeout has elapsed since `lastConsentUpdateTime = 0`.)
	 */
	@Test
	public void test_consentUpdate_collectYesRepeated_secondHasNoFlag() {
		// Persisted "y" — established by a prior session.
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("y");
		setupExistingConsents(new ConsentsBuilder().setCollect("y").buildToString());

		// Send "y" — no transition (lastDefinitive already "y"), but the dispatch still
		// happens because outsideTimeout is true on the first call.
		extension.handleConsentUpdate(buildConsentUpdateEvent("y", null));

		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		// CONSENT_PREFERENCES_UPDATED + EDGE_CONSENT_UPDATE = 2 dispatches
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());
		Event prefsUpdated = null;
		for (Event evt : eventCaptor.getAllValues()) {
			if (ConsentConstants.EventNames.CONSENT_PREFERENCES_UPDATED.equals(evt.getName())) {
				prefsUpdated = evt;
				break;
			}
		}
		assertNotNull("Expected a CONSENT_PREFERENCES_UPDATED dispatch", prefsUpdated);
		assertNull(
			"Repeated y must not carry the flag",
			prefsUpdated.getEventData().get(ConsentConstants.EventDataKey.COLLECT_CONSENT_RESYNC_REQUIRED)
		);
	}

	/**
	 * GET_CONSENTS_RESPONSE (response to the public getConsents() query) must NEVER carry
	 * the transition flag — it answers a different question (current state).
	 */
	@Test
	public void test_getConsents_responseDoesNotIncludeFlag() {
		setupExistingConsents(new ConsentsBuilder().setCollect("y").buildToString());

		Event requestEvent = new Event.Builder(
			ConsentConstants.EventNames.GET_CONSENTS_REQUEST,
			EventType.CONSENT,
			EventSource.REQUEST_CONTENT
		)
			.setEventData(new HashMap<String, Object>())
			.build();
		extension.handleRequestContent(requestEvent);

		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi).dispatch(eventCaptor.capture());
		Event response = eventCaptor.getValue();
		assertEquals(ConsentConstants.EventNames.GET_CONSENTS_RESPONSE, response.getName());
		assertNull(
			"GET_CONSENTS_RESPONSE must never carry the transition flag",
			response.getEventData().get(ConsentConstants.EventDataKey.COLLECT_CONSENT_RESYNC_REQUIRED)
		);
	}

	/**
	 * EDGE_CONSENT_UPDATE (Edge-bound) must never carry the transition flag.
	 */
	@Test
	public void test_edgeConsentUpdate_doesNotIncludeFlag() {
		setupExistingConsents(null);
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn(null);

		extension.handleConsentUpdate(buildConsentUpdateEvent("y", null));

		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());

		Event edgeUpdate = null;
		for (Event evt : eventCaptor.getAllValues()) {
			if (ConsentConstants.EventNames.EDGE_CONSENT_UPDATE.equals(evt.getName())) {
				edgeUpdate = evt;
				break;
			}
		}
		assertNotNull("Expected an EDGE_CONSENT_UPDATE dispatch", edgeUpdate);
		assertNull(
			"EDGE_CONSENT_UPDATE must never carry the transition flag",
			edgeUpdate.getEventData().get(ConsentConstants.EventDataKey.COLLECT_CONSENT_RESYNC_REQUIRED)
		);
	}

	/**
	 * Load-bearing invariant for the refactor: the XDM shared state must NOT contain
	 * the {@code collectConsentResyncRequired} flag, even though {@code shareCurrentConsents}
	 * mutates the same {@code xdmConsents} map in place after calling
	 * {@code createXDMSharedState}. The shared state describes <i>current state</i>; the
	 * flag describes a <i>transient transition</i> and only belongs on the dispatched event.
	 *
	 * <p>This relies on the production {@code ExtensionApi.createXDMSharedState} contract
	 * of snapshotting the supplied map synchronously. The test uses {@code doAnswer} to
	 * mimic that snapshot at call time — a plain {@code ArgumentCaptor} would hold a
	 * reference and incorrectly see the post-call mutation.
	 */
	@Test
	public void test_sharedStateDoesNotCarryFlag_onTransitionDispatch() {
		// Persisted lastDefinitive = "n" so the next "y" update triggers a transition
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("n");
		setupExistingConsents(new ConsentsBuilder().setCollect("n").buildToString());

		// Snapshot the shared-state map at the moment createXDMSharedState is invoked,
		// mimicking the production EventHub's internal deep-copy behavior. Without this,
		// the captured reference would see post-call mutations and the assertion below
		// would be meaningless.
		final Map<String, Object> snapshottedSharedState = new HashMap<>();
		Mockito
			.doAnswer(invocation -> {
				@SuppressWarnings("unchecked")
				final Map<String, Object> arg = invocation.getArgument(0);
				snapshottedSharedState.clear();
				snapshottedSharedState.putAll(arg);
				return null;
			})
			.when(mockExtensionApi)
			.createXDMSharedState(any(Map.class), any(Event.class));

		extension.handleConsentUpdate(buildConsentUpdateEvent("y", null));

		// Shared state MUST NOT contain the flag (snapshot was taken before the in-place mutation)
		assertNull(
			"XDM shared state must not carry the transient transition flag",
			snapshottedSharedState.get(ConsentConstants.EventDataKey.COLLECT_CONSENT_RESYNC_REQUIRED)
		);

		// Dispatched CONSENT_PREFERENCES_UPDATED event MUST contain the flag
		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi, times(2)).dispatch(eventCaptor.capture());
		Event prefsUpdated = null;
		for (Event evt : eventCaptor.getAllValues()) {
			if (ConsentConstants.EventNames.CONSENT_PREFERENCES_UPDATED.equals(evt.getName())) {
				prefsUpdated = evt;
				break;
			}
		}
		assertNotNull("Expected a CONSENT_PREFERENCES_UPDATED dispatch", prefsUpdated);
		assertEquals(
			"CONSENT_PREFERENCES_UPDATED must carry the flag on n -> y",
			Boolean.TRUE,
			prefsUpdated.getEventData().get(ConsentConstants.EventDataKey.COLLECT_CONSENT_RESYNC_REQUIRED)
		);
	}

	/**
	 * Parity coverage: the Edge handle dispatch path
	 * ({@code handleEdgeConsentPreferenceHandle}) is one of the three callers of
	 * {@code shareCurrentConsents}; it must also fire the flag on a non-y -> y transition.
	 */
	@Test
	public void test_edgeConsentPreferenceHandle_collectYesFromN_dispatchesFlag() throws Exception {
		// Persisted lastDefinitive = "n" so the next "y" arriving via Edge handle triggers a transition
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("n");
		setupExistingConsents(new ConsentsBuilder().setCollect("n").buildToString());

		// Build an Edge consent:preferences handle whose payload sets collect=y
		final String jsonString =
			"{ \"payload\": [ { \"collect\": { \"val\": \"y\" } } ], \"type\": \"consent:preferences\" }";
		extension.handleEdgeConsentPreferenceHandle(buildEdgeConsentPreferenceEvent(jsonString));

		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		// Only a CONSENT_PREFERENCES_UPDATED is dispatched on the Edge handle path
		// (no EDGE_CONSENT_UPDATE — that only goes out from handleConsentUpdate).
		verify(mockExtensionApi).dispatch(eventCaptor.capture());
		Event prefsUpdated = eventCaptor.getValue();
		assertEquals(ConsentConstants.EventNames.CONSENT_PREFERENCES_UPDATED, prefsUpdated.getName());
		assertEquals(
			"Edge handle path must fire the flag on n -> y",
			Boolean.TRUE,
			prefsUpdated.getEventData().get(ConsentConstants.EventDataKey.COLLECT_CONSENT_RESYNC_REQUIRED)
		);
	}

	/**
	 * Parity coverage: the configuration-defaults dispatch path
	 * ({@code handleConfigurationResponse}) is the third caller of
	 * {@code shareCurrentConsents}; flipping the default from "n" to "y" with no
	 * persisted user preference must fire the flag.
	 */
	@Test
	public void test_configurationResponse_defaultCollectYesFromN_dispatchesFlag() throws Exception {
		// Persisted lastDefinitive = "n" so updating defaults to "y" triggers a transition
		Mockito
			.when(mockNamedCollection.getString(ConsentConstants.DataStoreKey.LAST_DEFINITIVE_COLLECT_CONSENT, null))
			.thenReturn("n");
		// Seed an existing default of "n" so the subsequent "y" default flips the effective state.
		extension.handleConfigurationResponse(
			buildConfigurationResponseEvent(new ConsentsBuilder().setCollect("n").buildToString())
		);
		Mockito.reset(mockExtensionApi);

		// Now flip the default to "y" — effective collect goes n -> y
		extension.handleConfigurationResponse(
			buildConfigurationResponseEvent(new ConsentsBuilder().setCollect("y").buildToString())
		);

		ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		verify(mockExtensionApi).dispatch(eventCaptor.capture());
		Event prefsUpdated = eventCaptor.getValue();
		assertEquals(ConsentConstants.EventNames.CONSENT_PREFERENCES_UPDATED, prefsUpdated.getName());
		assertEquals(
			"Defaults path must fire the flag on n -> y",
			Boolean.TRUE,
			prefsUpdated.getEventData().get(ConsentConstants.EventDataKey.COLLECT_CONSENT_RESYNC_REQUIRED)
		);
	}
}
