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

import java.util.HashMap;
import java.util.Map;

public enum ConsentValue {
    YES("y"),
    NO("n");

    private String consentString;

    /**
     * Returns {@link ConsentValue} value of the provided string.
     * <p>
     * Returns null if the provided string is not a valid {@link ConsentValue} enum value
     *
     * @return {@link ConsentValue} value for provided status string
     */
    ConsentValue(final String consentString) {
        this.consentString = consentString;
    }

    public String stringValue() {
        return consentString;
    }

    /**
     * Returns {@link ConsentValue} enum value for the provided string.
     * <p>
     * returns null if the provided string is not an valid enum string
     *
     * @param consentString string value of consent thats needs to be looked up
     * @return {@link ConsentValue} value for provided consent string
     */
    public static ConsentValue get(final String consentString) {
        return lookup.get(consentString);
    }

    // generate look up table on load time
    private static final Map<String, ConsentValue> lookup = new HashMap<>();

    static {
        for (ConsentValue env : ConsentValue.values()) {
            lookup.put(env.stringValue(), env);
        }
    }
}
