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

import org.junit.Assert;
import org.junit.Test;

public class ConsentValueTest {

    @Test
    public void test_get() {
        // test and verify
        Assert.assertEquals(ConsentValue.YES, ConsentValue.get("y"));
        Assert.assertEquals(ConsentValue.NO, ConsentValue.get("n"));
        Assert.assertNull(ConsentValue.get("invalid"));
        Assert.assertNull(ConsentValue.get(null));
        Assert.assertNull(ConsentValue.get(""));
    }

    @Test
    public void test_vwt() {
        // test and verify
        Assert.assertEquals("y", ConsentValue.YES.stringValue());
        Assert.assertEquals("n", ConsentValue.NO.stringValue());
    }
}
