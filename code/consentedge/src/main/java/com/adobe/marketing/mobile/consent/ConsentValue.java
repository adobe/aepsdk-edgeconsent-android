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

    private String consentStatusString;

    /**
     * Returns {@link ConsentValue} value of the provided string.
     * <p>
     * Returns null if the provided string is not a valid {@link ConsentValue} enum value
     *
     * @return {@link ConsentValue} value for provided status string
     */
    ConsentValue(final String consentStatusString) {
        this.consentStatusString = consentStatusString;
    }

    public String stringValue() {
        return consentStatusString;
    }

    /**
     * Returns {@link ConsentValue} enum value for the provided string.
     * <p>
     * Defaults to NO if the provided string is not a valid {@link ConsentValue} enum value
     *
     * @return {@link ConsentValue} value for provided status string
     */
    public static ConsentValue get(final String statusString) {
        ConsentValue enumValue = lookup.get(statusString);
        return enumValue;
    }

    // generate look up table on load time
    private static final Map<String, ConsentValue> lookup = new HashMap<>();

    static {
        for (ConsentValue env : ConsentValue.values()) {
            lookup.put(env.stringValue(), env);
        }
    }
}
