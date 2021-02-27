package com.adobe.marketing.mobile.consent;

import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.marketing.mobile.consent.ConsentTestUtil.CreateConsentXDMMap;
import static com.adobe.marketing.mobile.consent.ConsentTestUtil.SAMPLE_METADATA_TIMESTAMP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConsentsTest {

    // ========================================================================================
    // Test Scenarios   : All possible XDMFormatted Map values
    // Test method      : Constructor, isEmpty
    // ========================================================================================
    @Test
    public void test_ConsentsCreation_With_ConsentDataMap() {
        // setup
        Map<String, Object> consentData = CreateConsentXDMMap("y", "n", "vi", SAMPLE_METADATA_TIMESTAMP);

        // test
        Consents consents = new Consents(consentData);

        // verify
        assertEquals("y", ConsentTestUtil.readCollectConsent(consents));
        assertEquals("n", ConsentTestUtil.readAdIdConsent(consents));
        assertEquals("vi", ConsentTestUtil.readPersonalizeConsent(consents));
        assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(consents));
        assertFalse(consents.isEmpty());
    }

    //
    @Test
    public void test_ConsentsCreation_With_CollectConsentOnly() {
        // setup
        Map<String, Object> consentData = ConsentTestUtil.CreateConsentXDMMap("n");

        // test
        Consents consents = new Consents(consentData);

        // verify
        assertEquals("n", ConsentTestUtil.readCollectConsent(consents));
        assertNull(ConsentTestUtil.readAdIdConsent(consents));
        assertNull(ConsentTestUtil.readPersonalizeConsent(consents));
        assertNull(ConsentTestUtil.readTimestamp(consents));
    }


    @Test
    public void test_ConsentsCreation_With_NoConsentDetailsInMap() {
        // setup
        Map<String, Object> consentData = ConsentTestUtil.CreateConsentXDMMap(null, null);

        // test
        Consents consents = new Consents(consentData);

        // verify
        assertTrue(consents.isEmpty());
    }

    @Test
    public void test_ConsentsCreation_With_EmptyConsentMap() {
        // test
        Consents consents = new Consents(new HashMap<String, Object>());

        // verify
        assertTrue(consents.isEmpty());
    }

    @Test
    public void test_ConsentsCreation_With_NullConsentMap() {
        // test
        Map<String, Object> xdmMap = null;
        Consents consents = new Consents(xdmMap);

        // verify
        assertTrue(consents.isEmpty());
    }

    @Test
    public void test_ConsentsCreation_With_InvalidMap() {
        // test
        Consents consents = new Consents(new HashMap<String, Object>() {
            {
                put("invalidKey", 30034);
            }
        });

        // verify
        assertTrue(consents.isEmpty());
    }

    @Test
    public void test_ConsentsCreation_With_InvalidConsentMap() {
        // test
        Consents consents = new Consents(new HashMap<String, Object>() {
            {
                put("consents", 30034);
            }
        });

        // verify
        assertTrue(consents.isEmpty());
    }


    // ========================================================================================
    // Test Scenarios   : All possible Consent object values
    // Test method      : Copy Constructor, isEmpty
    // ========================================================================================

    @Test
    public void test_CopyConstructor() {
        // setup
        Map<String, Object> consentData = ConsentTestUtil.CreateConsentXDMMap("y", "n");
        Consents originalConsent = new Consents(consentData);

        // test
        Consents copiedConsent = new Consents(originalConsent);

        assertEquals("y", ConsentTestUtil.readCollectConsent(copiedConsent));
        assertEquals("n", ConsentTestUtil.readAdIdConsent(copiedConsent));
    }

    @Test
    public void test_CopyConstructor_nullConsents() {
        // setup
        Consents originalConsent = null;

        // test
        Consents copiedConsent = new Consents(originalConsent);

        // verify
        assertTrue(copiedConsent.isEmpty());
    }

    // ========================================================================================
    // Test method : AsXDMMap
    // ========================================================================================
    @Test
    public void test_AsXDMMap() {
        // setup
        Map<String, Object> consentData = CreateConsentXDMMap("y", "n", "vi", SAMPLE_METADATA_TIMESTAMP);

        // test and verify
        Consents consents = new Consents(consentData);
        assertEquals(consentData, consents.asXDMMap());
    }

    @Test
    public void test_AsMap_whenEmptyConsents() {
        // setup
        Map<String, Object> consentData = ConsentTestUtil.CreateConsentXDMMap(null, null);
        Consents consents = new Consents(consentData);

        // test and verify
        assertNull(consents.asXDMMap());
    }

    // ========================================================================================
    // Test method : Merge
    // ========================================================================================
    @Test
    public void test_merge() {
        // setup
        Map<String, Object> xdmMap = null;
        Consents baseConsent = new Consents(xdmMap);

        // test
        Consents firstOverridingConsent = new Consents(CreateConsentXDMMap("y"));
        baseConsent.merge(firstOverridingConsent);

        // verify
        assertEquals("y", ConsentTestUtil.readCollectConsent(baseConsent));
        assertNull(ConsentTestUtil.readAdIdConsent(baseConsent));
        assertNull(ConsentTestUtil.readTimestamp(baseConsent));

        // test again
        Consents secondOverridingConsent = new Consents(CreateConsentXDMMap("n", "n", SAMPLE_METADATA_TIMESTAMP));
        baseConsent.merge(secondOverridingConsent);

        assertEquals("n", ConsentTestUtil.readCollectConsent(baseConsent));
        assertEquals("n", ConsentTestUtil.readAdIdConsent(baseConsent));
        assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(baseConsent));
    }

    @Test
    public void test_merge_NullConsent() {
        // setup
        Consents baseConsent = new Consents(CreateConsentXDMMap("n", null, SAMPLE_METADATA_TIMESTAMP));

        // test
        baseConsent.merge(null);

        // verify
        assertEquals("n", ConsentTestUtil.readCollectConsent(baseConsent));
        assertNull(ConsentTestUtil.readAdIdConsent(baseConsent));
        assertEquals(SAMPLE_METADATA_TIMESTAMP, ConsentTestUtil.readTimestamp(baseConsent));
    }

    // ========================================================================================
    // Test method : setTimestamp
    // ========================================================================================
    @Test
    public void test_setTimeStamp() {
        // setup
        Consents consents = new Consents(CreateConsentXDMMap("n"));

        // test
        long currentTimestamp = System.currentTimeMillis();
        String iso8601DateString = DateUtility.dateToISO8601String(new Date(currentTimestamp));
        consents.setTimestamp(currentTimestamp);

        // verify
        assertEquals(iso8601DateString, ConsentTestUtil.readTimestamp(consents));
    }

    @Test
    public void test_setTimeStamp_whenConsentsEmpty() {
        // setup
        Consents consents = new Consents(new HashMap<String, Object>());

        // test
        consents.setTimestamp(System.currentTimeMillis());

        // verify
        assertNull(ConsentTestUtil.readTimestamp(consents));
    }
}
