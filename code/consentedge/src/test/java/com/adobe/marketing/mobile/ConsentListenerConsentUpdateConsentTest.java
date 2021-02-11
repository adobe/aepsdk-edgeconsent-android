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

package com.adobe.marketing.mobile;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ConsentListenerConsentUpdateConsentTest {

    @Mock
    private ConsentExtension mockConsentExtension;

    private ConsentListenerConsentUpdateConsent listener;

    @Before
    public void setup() {
        mockConsentExtension = Mockito.mock(ConsentExtension.class);
        MobileCore.start(null);
        listener = spy(new ConsentListenerConsentUpdateConsent(null, EventType.EDGE.getName(), ConsentConstants.EventSource.CONSENT_PREFERENCE));
    }

    @Test
    public void testHear() {
        // setup
        Event event = new Event.Builder("Edge consent preference response event", EventType.EDGE.getName(),
                ConsentConstants.EventSource.CONSENT_PREFERENCE).build();
        doReturn(mockConsentExtension).when(listener).getParentExtension();

        // test
        listener.hear(event);

        // verify
        verify(mockConsentExtension, times(1)).handleConsentUpdate(event);
    }

    @Test
    public void testHear_WhenParentExtensionNull() {
        // setup
        Event event = new Event.Builder("Edge consent preference response event", EventType.EDGE.getName(),
                ConsentConstants.EventSource.CONSENT_PREFERENCE).build();
        doReturn(null).when(listener).getParentExtension();

        // test
        listener.hear(event);

        // verify
        verify(mockConsentExtension, times(0)).handleConsentUpdate(any(Event.class));
    }

    @Test
    public void testHear_WhenEventNull() {
        // setup
        doReturn(null).when(listener).getParentExtension();
        doReturn(mockConsentExtension).when(listener).getParentExtension();

        // test
        listener.hear(null);

        // verify
        verify(mockConsentExtension, times(0)).handleConsentUpdate(any(Event.class));
    }
}
