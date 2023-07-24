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

package org.axonframework.springboot.aot.autoconfig;

import com.axoniq.someproject.api.SomeCommand;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests default target resolver
 *
 * @author Gerard Klijs
 */
class DefaultTargetResolverTest {

    private static final String SOME_CONTEXT = "some_context";

    @Test
    void returnGivenValue() {
        DefaultTargetContextResolver<CommandMessage<?>> resolver = new DefaultTargetContextResolver<>(SOME_CONTEXT);
        String result = resolver.resolveContext(new GenericCommandMessage<>(new SomeCommand("1")));
        assertEquals(SOME_CONTEXT, result);
    }
}
