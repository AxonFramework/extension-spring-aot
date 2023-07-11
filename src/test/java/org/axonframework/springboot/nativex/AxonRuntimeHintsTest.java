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


import com.axoniq.someproject.api.ChangeStatusCommand;
import com.axoniq.someproject.api.SomeCommand;
import com.axoniq.someproject.api.SomeEvent;
import com.axoniq.someproject.api.SomeProjectionEvent;
import com.axoniq.someproject.api.SomeQuery;
import com.axoniq.someproject.api.SomeResult;
import com.axoniq.someproject.api.StatusChangedEvent;
import com.axoniq.someproject.something.SomeAggregate;
import com.axoniq.someproject.something.SomeProjectionWithGroupAnnotation;
import com.axoniq.someproject.something.SomeProjectionWithoutGroupAnnotation;
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

import java.lang.reflect.Method;
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
        //the ones find via the classloader
        testForConstructorAndAllMethods(ChangeStatusCommand.class);
        testForConstructorAndAllMethods(SomeCommand.class);
        testForConstructorAndAllMethods(SomeEvent.class);
        testForConstructorAndAllMethods(SomeProjectionEvent.class);
        testForConstructorAndAllMethods(SomeQuery.class);
        testForConstructorAndAllMethods(SomeResult.class);
        testForConstructorAndAllMethods(StatusChangedEvent.class);
        //some default framework classes
        testForConstructor(GlobalSequenceTrackingToken.class);
        testForConstructorAndAllMethods(OptionalResponseType.class);
    }

    @Test
    void handlerMethodsHaveReflectiveHints() {
        testReflectionMethod(SomeAggregate.class, "handle");
        testReflectionMethod(SomeAggregate.class, "onSomeEvent");
        testReflectionMethod(SomeAggregate.class, "onStatusChangedEvent");
        testReflectionMethod(SomeProjectionWithGroupAnnotation.class, "handle");
        testReflectionMethod(SomeProjectionWithGroupAnnotation.class, "on");
        testReflectionMethod(SomeProjectionWithoutGroupAnnotation.class, "handle");
        testReflectionMethod(SomeProjectionWithoutGroupAnnotation.class, "on");
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
        assertTrue(RuntimeHintsPredicates.proxies().forInterfaces(Connection.class).test(this.hints));
        assertTrue(RuntimeHintsPredicates.proxies().forInterfaces(TypeReference.of(
                                                 "org.axonframework.common.jdbc.UnitOfWorkAwareConnectionProviderWrapper$UoWAttachedConnection"))
                                         .test(this.hints));
    }

    private void testReflectionMethod(Class<?> clazz, String methodName) {
        assertTrue(RuntimeHintsPredicates.reflection().onMethod(clazz, methodName).test(this.hints),
                   "No reflection on method " + methodName + " in class " + clazz.getName());
    }

    private void testForConstructorAndAllMethods(Class<?> clazz) {
        testForConstructor(clazz);
        for (Method method : clazz.getDeclaredMethods()) {
            assertTrue(RuntimeHintsPredicates.reflection().onMethod(clazz, method.getName())
                                             .test(this.hints),
                       "Method " + method + " not accessible on " + clazz.getSimpleName());
        }
    }

    private void testForConstructor(Class<?> clazz) {
        assertTrue(RuntimeHintsPredicates.reflection().onConstructor(clazz.getConstructors()[0])
                                         .test(this.hints), "Constructor not accessible on " + clazz.getSimpleName());
    }
}
