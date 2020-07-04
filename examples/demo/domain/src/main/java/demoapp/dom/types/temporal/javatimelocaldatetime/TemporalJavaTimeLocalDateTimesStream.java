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
package demoapp.dom.types.temporal.javatimelocaldatetime;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import demoapp.dom.types.Samples;

@Service
public class TemporalJavaTimeLocalDateTimesStream implements Samples<LocalDateTime> {

    @Override
    public Stream<LocalDateTime> stream() {
        return Stream.of(1, 2, 3)
                .map(x -> java.time.LocalDateTime.of(2020,x,x,x,x));
    }

}
