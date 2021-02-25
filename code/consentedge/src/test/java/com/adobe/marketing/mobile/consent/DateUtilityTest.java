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

import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static junit.framework.TestCase.assertEquals;

public class DateUtilityTest {

    @Test
    public void dateToISO8601String_onValidTimestamp_returnsFormattedString() {
        Calendar cal = new Calendar.Builder()
                .set(Calendar.YEAR, 2019)
                .set(Calendar.MONTH, Calendar.SEPTEMBER)
                .set(Calendar.DAY_OF_MONTH, 23)
                .set(Calendar.HOUR, 11)
                .set(Calendar.MINUTE, 15)
                .set(Calendar.SECOND, 45)
                .setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"))
                .build();

        String serializedDate = DateUtility.dateToISO8601String(cal.getTime());
        // Expected time in UTC which is +7 hours from America/Los_Angeles during Daylight Savings
        assertEquals("2019-09-23T18:15:45Z", serializedDate);
    }

    @Test
    public void dateToISO8601String_onNull_returnsEmptyString() {
        String serializedDate = DateUtility.dateToISO8601String(null);
        assertEquals("", serializedDate);
    }
}
