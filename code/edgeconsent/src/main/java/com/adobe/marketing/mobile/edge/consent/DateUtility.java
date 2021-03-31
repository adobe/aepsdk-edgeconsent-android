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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class DateUtility {

	private DateUtility() {
	}

	private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	/**
	 * Formats a {@code Date} to an ISO 8601 date-time string in UTC as defined in
	 * <a href="https://tools.ietf.org/html/rfc3339#section-5.6">RFC 3339, section 5.6</a>
	 * For example, 2017-09-26T15:52:25Z
	 *
	 * @param timestamp a timestamp
	 * @return {@code timestamp} formatted to a string in the format of 'yyyy-MM-dd'T'HH:mm:ss'Z'',
	 * or an empty string if {@code timestamp} is null
	 */
	static String dateToISO8601String(final Date timestamp) {
		if (timestamp == null) {
			return "";
		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		return simpleDateFormat.format(timestamp);
	}
}
