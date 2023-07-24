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

package org.axonframework.springboot.aot;

import com.axoniq.someproject.api.ChangeStatusCommand;
import com.axoniq.someproject.api.SingleChildCommand;
import com.axoniq.someproject.api.SomeChildCommand;
import com.axoniq.someproject.api.SomeCommand;
import com.axoniq.someproject.api.SomeEvent;
import com.axoniq.someproject.api.SomeProjectionEvent;
import com.axoniq.someproject.api.SomeQuery;
import com.axoniq.someproject.api.SomeResult;
import com.axoniq.someproject.api.StatusChangedEvent;
import com.axoniq.someproject.something.SingleAggregateChild;
import com.axoniq.someproject.something.SomeAggregate;
import com.axoniq.someproject.something.SomeAggregateChild;
import com.axoniq.someproject.something.SomeProjectionWithGroupAnnotation;
import com.axoniq.someproject.something.SomeProjectionWithoutGroupAnnotation;
import org.junit.jupiter.api.*;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

class MessageHandlerRuntimeHintsRegistrarTest {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
    private final TestGenerationContext generationContext = new TestGenerationContext();
    private final GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext(beanFactory);

    @BeforeEach
    void processAheadOfTime() {
        addClassToBeanFactory(SomeAggregate.class);
        addClassToBeanFactory(SingleAggregateChild.class);
        addClassToBeanFactory(SomeAggregateChild.class);
        addClassToBeanFactory(SomeProjectionWithGroupAnnotation.class);
        addClassToBeanFactory(SomeProjectionWithoutGroupAnnotation.class);
        new ApplicationContextAotGenerator().processAheadOfTime(this.applicationContext, this.generationContext);
        this.generationContext.writeGeneratedContent();
    }

    @Test
    void objectsUsedInHandlersHaveReflectiveAccessToConstructor() {
        testForConstructor(ChangeStatusCommand.class);
        testForConstructor(SomeCommand.class);
        testForConstructor(SomeEvent.class);
        testForConstructor(SomeProjectionEvent.class);
        testForConstructor(SomeQuery.class);
        testForConstructor(SomeResult.class);
        testForConstructor(StatusChangedEvent.class);
        //the ones from the aggregate childs
        testForConstructor(SingleChildCommand.class);
        testForConstructor(SomeChildCommand.class);
    }

    @Test
    void handlerMethodsHaveReflectiveHints() {
        testForConstructor(SomeAggregate.class);
        testReflectionMethod(SomeAggregate.class, "handle");
        testReflectionMethod(SomeAggregate.class, "onSomeEvent");
        testReflectionMethod(SomeAggregate.class, "onStatusChangedEvent");
        testReflectionMethod(SomeProjectionWithGroupAnnotation.class, "handle");
        testReflectionMethod(SomeProjectionWithGroupAnnotation.class, "on");
        testReflectionMethod(SomeProjectionWithoutGroupAnnotation.class, "handle");
        testReflectionMethod(SomeProjectionWithoutGroupAnnotation.class, "on");
        testReflectionMethod(SingleAggregateChild.class, "handle");
        testReflectionMethod(SomeAggregateChild.class, "handle");
    }

    private void addClassToBeanFactory(Class<?> clazz) {
        BeanDefinition definition = new RootBeanDefinition(clazz);
        beanFactory.registerBeanDefinition(clazz.getName(), definition);
    }

    private void testForConstructor(Class<?> clazz) {
        assertTrue(RuntimeHintsPredicates.reflection().onConstructor(clazz.getConstructors()[0])
                                         .test(this.generationContext.getRuntimeHints()),
                   "Constructor not accessible on " + clazz.getSimpleName());
    }

    private void testReflectionMethod(Class<?> clazz, String methodName) {
        assertTrue(RuntimeHintsPredicates.reflection().onMethod(clazz, methodName)
                                         .test(this.generationContext.getRuntimeHints()),
                   "No reflection on method " + methodName + " in class " + clazz.getName());
    }
}
