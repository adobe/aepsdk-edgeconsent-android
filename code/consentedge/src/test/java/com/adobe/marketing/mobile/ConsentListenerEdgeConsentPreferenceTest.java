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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ConsentListenerEdgeConsentPreferenceTest {

    @Mock
    private ConsentExtension mockConsentExtension;

    private ConsentListenerEdgeConsentPreference listener;

    @Before
    public void setup() {
        mockConsentExtension = Mockito.mock(ConsentExtension.class);
        MobileCore.start(null);
        listener = spy(new ConsentListenerEdgeConsentPreference(null, ConsentTestConstants.EventType.EDGE, ConsentTestConstants.EventSource.CONSENT_PREFERENCE));
    }

    @Test
    public void testHear() {
        // setup
        Event event = new Event.Builder("Edge consent preference response event", ConsentTestConstants.EventType.EDGE,
                ConsentTestConstants.EventSource.CONSENT_PREFERENCE).build();
        doReturn(mockConsentExtension).when(listener).getParentExtension();

        // test
        listener.hear(event);

        // verify
        verify(mockConsentExtension, times(1)).handleEdgeConsentPreference(event);
    }

    @Test
    public void testHear_WhenParentExtensionNull() {
        // setup
        Event event = new Event.Builder("Edge consent preference response event", ConsentTestConstants.EventType.EDGE,
                ConsentTestConstants.EventSource.CONSENT_PREFERENCE).build();
        doReturn(null).when(listener).getParentExtension();

        // test
        listener.hear(event);

        // verify
        verify(mockConsentExtension, times(0)).handleEdgeConsentPreference(any(Event.class));
    }

    @Test
    public void testHear_WhenEventNull() {
        // setup
        doReturn(null).when(listener).getParentExtension();
        doReturn(mockConsentExtension).when(listener).getParentExtension();

        // test
        listener.hear(null);

        // verify
        verify(mockConsentExtension, times(0)).handleEdgeConsentPreference(any(Event.class));
    }

}
