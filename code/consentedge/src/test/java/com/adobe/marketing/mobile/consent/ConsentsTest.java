package com.adobe.marketing.mobile.consent;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.adobe.marketing.mobile.consent.ConsentTestUtil.CreateConsentDataMap;
import static com.adobe.marketing.mobile.consent.ConsentTestUtil.SAMPLE_METADATA_TIMESTAMP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConsentsTest {


    @Test
    public void test_ConsentsGetterAndSetter() {
        // setup
        Consents consents = new Consents();

        // verify
        assertNull(consents.getCollectConsent());
        assertNull(consents.getAdIdConsent());
        assertNull(consents.getMetadata());
        assertTrue(consents.isEmpty());

        // test and verify collectConsent Getter/Setter
        consents.setCollectConsent(ConsentValue.NO);
        assertEquals(ConsentValue.NO, consents.getCollectConsent());

        // test and verify adIdConsent Getter/Setter
        consents.setAdIdConsent(ConsentValue.YES);
        assertEquals(ConsentValue.YES, consents.getAdIdConsent());
        assertFalse(consents.isEmpty());
    }

    @Test
    public void test_CopyConstructor() {
        // setup
        Map<String, Object> consentData = CreateConsentDataMap("y", "n");
        Consents originalConsent = new Consents(consentData, SAMPLE_METADATA_TIMESTAMP);

        // test
        Consents copiedConsent = new Consents(originalConsent);

        assertEquals(copiedConsent.getCollectConsent(), originalConsent.getCollectConsent());
        assertEquals(copiedConsent.getAdIdConsent(), originalConsent.getAdIdConsent());
        assertEquals(copiedConsent.getMetadata(), originalConsent.getMetadata());
    }


    @Test
    public void test_ConsentsCreation_With_ConsentDataMap() {
        // setup
        Map<String, Object> consentData = CreateConsentDataMap("y", "n");

        // test
        Consents consents = new Consents(consentData);

        // verify
        assertEquals(ConsentValue.YES, consents.getCollectConsent());
        assertEquals(ConsentValue.NO, consents.getAdIdConsent());
        assertFalse(consents.isEmpty());
    }

    @Test
    public void test_ConsentsCreation_With_AdIdOnlyMap() {
        // setup
        Map<String, Object> consentData = CreateConsentDataMap(null, "n");

        // test
        Consents consents = new Consents(consentData);

        // verify
        assertNull(consents.getCollectConsent());
        assertEquals(ConsentValue.NO, consents.getAdIdConsent());
        assertFalse(consents.isEmpty());
    }

    @Test
    public void test_ConsentsCreation_With_CollectConsentOnlyMap() {
        // setup
        Map<String, Object> consentData = CreateConsentDataMap("y", null);

        // test
        Consents consents = new Consents(consentData);

        // verify
        assertEquals(ConsentValue.YES, consents.getCollectConsent());
        assertNull(consents.getAdIdConsent());
        assertFalse(consents.isEmpty());
    }

    @Test
    public void test_ConsentsCreation_With_NoConsentDetailsInMap() {
        // setup
        Map<String, Object> consentData = CreateConsentDataMap(null, null);

        // test
        Consents consents = new Consents(consentData);

        // verify
        assertNull(consents.getAdIdConsent());
        assertNull(consents.getCollectConsent());
        assertTrue(consents.isEmpty());
    }

    @Test
    public void test_ConsentsCreation_With_InvalidValueForAdIDAndCollectConsent() {
        // setup
        Map<String, Object> consentData = CreateConsentDataMap("invalid", "");

        // test
        Consents consents = new Consents(consentData);

        // verify
        assertNull(consents.getAdIdConsent());
        assertNull(consents.getCollectConsent());
        assertTrue(consents.isEmpty());
    }

    @Test
    public void test_ConsentsCreation_With_EmptyConsentMap() {
        // test
        Consents consents = new Consents(new HashMap<String, Object>());

        // verify
        assertNull(consents.getAdIdConsent());
        assertNull(consents.getCollectConsent());
        assertTrue(consents.isEmpty());
    }

    @Test
    public void test_ConsentsCreation_With_NullConsentMap() {
        // test
        Consents consents = new Consents(new HashMap<String, Object>());

        // verify
        assertNull(consents.getAdIdConsent());
        assertNull(consents.getCollectConsent());
        assertTrue(consents.isEmpty());
    }

    @Test
    public void test_ConsentsCreation_With_InvalidConsentMap() {
        // test
        Consents consents = new Consents(new HashMap<String, Object>() {
            {
                put("invalidKey", 30034);
            }
        });

        // verify
        assertNull(consents.getAdIdConsent());
        assertNull(consents.getCollectConsent());
        assertTrue(consents.isEmpty());
    }

    @Test
    public void test_AsMap() {
        // setup
        Map<String, Object> consentData = CreateConsentDataMap("y", "n");

        // test and verify
        Consents consents = new Consents(consentData);
        assertEquals(consentData, consents.asMap());
    }

    @Test
    public void test_AsMap_whenEmptyConsents() {
        // setup
        Consents consents = new Consents();

        // test and verify
        assertNull(consents.asMap());
    }

    @Test
    public void test_merge() {
        // setup
        Consents baseConsent = new Consents();
        baseConsent.setAdIdConsent(ConsentValue.YES);
        baseConsent.setCollectConsent(ConsentValue.NO);

        // test
        Consents firstOverridingConsent = new Consents(CreateConsentDataMap("y", null));
        baseConsent.merge(firstOverridingConsent);

        // verify
        assertEquals(ConsentValue.YES, baseConsent.getCollectConsent());
        assertEquals(ConsentValue.YES, baseConsent.getAdIdConsent());
        assertNull(baseConsent.getMetadata());

        // test again
        Consents secondOverridingConsent = new Consents(CreateConsentDataMap("n", "n"), SAMPLE_METADATA_TIMESTAMP);
        baseConsent.merge(secondOverridingConsent);

        assertEquals(ConsentValue.NO, baseConsent.getCollectConsent());
        assertEquals(ConsentValue.NO, baseConsent.getAdIdConsent());
        assertEquals(SAMPLE_METADATA_TIMESTAMP, baseConsent.getMetadata().getTime());
    }

    @Test
    public void test_merge_NullConsent() {
        // setup
        Consents baseConsent = new Consents();
        baseConsent.setCollectConsent(ConsentValue.NO);
        baseConsent.setAdIdConsent(ConsentValue.YES);

        // test
        baseConsent.merge(null);

        // verify
        assertEquals(ConsentValue.NO, baseConsent.getCollectConsent());
        assertEquals(ConsentValue.YES, baseConsent.getAdIdConsent());
        assertNull(baseConsent.getMetadata());
    }

}
