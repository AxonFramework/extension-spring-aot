/*
 * Copyright (c) 2010-2023. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.axoniq.someproject.something;

import com.axoniq.someproject.api.SomeProjectionEvent;
import com.axoniq.someproject.api.SomeQuery;
import com.axoniq.someproject.api.SomeResult;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;

import java.util.ArrayList;
import java.util.List;

public class SomeProjectionWithoutGroupAnnotation {

    private final List<String> ids = new ArrayList<>();

    @EventHandler
    public void on(SomeProjectionEvent event) {
        ids.add(event.id());
    }

    @QueryHandler
    public SomeResult handle(SomeQuery query) {
        return new SomeResult(ids);
    }
}
