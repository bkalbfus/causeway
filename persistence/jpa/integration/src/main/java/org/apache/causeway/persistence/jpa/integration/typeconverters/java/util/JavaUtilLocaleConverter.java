/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.causeway.persistence.jpa.integration.typeconverters.java.util;

import java.util.Locale;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * @since 3.3 {@index}
 */
@Converter(autoApply = true)
public class JavaUtilLocaleConverter
implements AttributeConverter<Locale, String> {

    @Override
    public String convertToDatabaseColumn(final Locale locale) {
        return locale != null
                ? locale.toLanguageTag()
                : null;
    }

    @Override
    public Locale convertToEntityAttribute(final String datastoreValue) {
        return datastoreValue != null
                ? Locale.forLanguageTag(datastoreValue)
                : null;
    }

}
