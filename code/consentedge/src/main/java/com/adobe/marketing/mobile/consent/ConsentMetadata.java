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

public class ConsentMetadata {
    private String time;

    /**
     * @return {@link String} representing a valid ISO8601 time indicating the time when the consents were last updated.
     */
    public String getTime() {
        return time;
    }

    /**
     * Constructor for ConsentMeta
     * Initialized with the date when the consents updated.
     *
     * @param date must be a valid ISO8601 date String
     */
    public ConsentMetadata(final String date) {
        this.time = date;
    }
}
