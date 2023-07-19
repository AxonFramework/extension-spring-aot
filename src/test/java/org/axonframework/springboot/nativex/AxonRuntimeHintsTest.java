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

package org.axonframework.springboot.nativex;


import io.netty.channel.epoll.EpollChannelOption;
import org.axonframework.eventhandling.GlobalSequenceTrackingToken;
import org.axonframework.messaging.responsetypes.OptionalResponseType;
import org.junit.jupiter.api.*;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.ClassUtils;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Axon runtime hints
 *
 * @author Gerard Klijs
 */
class AxonRuntimeHintsTest {

    private RuntimeHints hints;

    @BeforeEach
    void setup() {
        this.hints = new RuntimeHints();
        SpringFactoriesLoader.forResourceLocation("META-INF/spring/aot.factories")
                             .load(RuntimeHintsRegistrar.class).forEach(registrar -> registrar
                                     .registerHints(this.hints, ClassUtils.getDefaultClassLoader()));
    }

    @Test
    void serializableClassesHaveReflectionOnConstructor() {
        //some default framework classes
        testForConstructor(GlobalSequenceTrackingToken.class);
        testForConstructor(OptionalResponseType.class);
    }

    @Test
    void grpcFieldIsAccessible() {
        assertTrue(RuntimeHintsPredicates.reflection().onField(EpollChannelOption.class, "TCP_USER_TIMEOUT")
                                         .test(this.hints));
    }

    @Test
    void resourcePatternsArePresent() {
        assertTrue(RuntimeHintsPredicates.resource().forResource("axonserver_download.txt").test(this.hints));
        assertTrue(RuntimeHintsPredicates.resource().forResource("SQLErrorCode.properties").test(this.hints));
    }

    @Test
    void proxiesAreSet() {
        assertTrue(RuntimeHintsPredicates.proxies().forInterfaces(
                                                 TypeReference.of(Connection.class),
                                                 TypeReference.of(
                                                         "org.axonframework.common.jdbc.UnitOfWorkAwareConnectionProviderWrapper$UoWAttachedConnection"))
                                         .test(this.hints));
    }

    private void testForConstructor(Class<?> clazz) {
        assertTrue(RuntimeHintsPredicates.reflection().onConstructor(clazz.getConstructors()[0])
                                         .test(this.hints), "Constructor not accessible on " + clazz.getSimpleName());
    }
}
