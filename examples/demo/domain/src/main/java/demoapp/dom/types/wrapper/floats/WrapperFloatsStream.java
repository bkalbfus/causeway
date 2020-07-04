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
package demoapp.dom.types.wrapper.floats;

import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import demoapp.dom.types.Samples;

@Service
public class WrapperFloatsStream implements Samples<Float> {

    @Override
    public Stream<Float> stream() {
        return Stream.of(1.1f, 2.2f, 3.3f, 4.0f, -9.9f, -8.8f);
    }

}
