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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.MobileCore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

public class ListenerConsentRequestContentTest {

	@Mock
	private ConsentExtension mockConsentExtension;

	private ListenerConsentRequestContent listener;

	@Before
	public void setup() {
		mockConsentExtension = Mockito.mock(ConsentExtension.class);
		MobileCore.start(null);
		listener =
			spy(
				new ListenerConsentRequestContent(
					null,
					ConsentConstants.EventType.CONSENT,
					ConsentConstants.EventSource.REQUEST_CONTENT
				)
			);
	}

	@Test
	public void testHear() {
		// setup
		Event event = new Event.Builder(
			"Consent request content event",
			ConsentConstants.EventType.CONSENT,
			ConsentConstants.EventSource.REQUEST_CONTENT
		)
			.build();
		doReturn(mockConsentExtension).when(listener).getConsentExtension();

		// test
		listener.hear(event);

		// verify
		verify(mockConsentExtension, times(1)).handleRequestContent(event);
	}

	@Test
	public void testHear_WhenParentExtensionNull() {
		// setup
		Event event = new Event.Builder(
			"Consent request content event",
			ConsentConstants.EventType.CONSENT,
			ConsentConstants.EventSource.REQUEST_CONTENT
		)
			.build();
		doReturn(null).when(listener).getConsentExtension();

		// test
		listener.hear(event);

		// verify
		verify(mockConsentExtension, times(0)).handleRequestContent(any(Event.class));
	}

	@Test
	public void testHear_WhenEventNull() {
		// setup
		doReturn(null).when(listener).getConsentExtension();
		doReturn(mockConsentExtension).when(listener).getConsentExtension();

		// test
		listener.hear(null);

		// verify
		verify(mockConsentExtension, times(0)).handleRequestContent(any(Event.class));
	}
}
