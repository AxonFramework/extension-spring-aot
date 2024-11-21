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

import org.axonframework.common.Priority;
import org.axonframework.common.ReflectionUtils;
import org.axonframework.common.annotation.AnnotationUtils;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.annotation.AnnotatedHandlerInspector;
import org.axonframework.messaging.annotation.ClasspathParameterResolverFactory;
import org.axonframework.messaging.annotation.MessageHandlingMember;
import org.axonframework.messaging.annotation.MultiParameterResolverFactory;
import org.axonframework.messaging.annotation.ParameterResolver;
import org.axonframework.messaging.annotation.ParameterResolverFactory;
import org.axonframework.modelling.command.AggregateMember;
import org.axonframework.queryhandling.annotation.QueryHandlingMember;
import org.axonframework.spring.config.MessageHandlerLookup;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * BeanFactoryInitializationAotProcessor that registers message handler methods declared on beans for reflection. This
 * means that all methods annotated with {@code @MessageHandler} will be available for reflection.
 * <p/>
 * Additionally, the payload types for these methods are registered for reflective access, as well as the classes
 * containing the methods.
 *
 * @author Allard Buijze
 * @since 4.8.0
 */
public class MessageHandlerRuntimeHintsRegistrar implements BeanFactoryInitializationAotProcessor {

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        Set<Class<?>> messageHandlingClasses =
                MessageHandlerLookup.messageHandlerBeans(messageType(), beanFactory, true)
                                    .stream()
                                    .map(beanFactory::getType)
                                    .collect(Collectors.toSet());

        Set<Class<?>> detectedClasses = new HashSet<>();
        messageHandlingClasses.forEach(c -> registerAggregateMembers(c, detectedClasses));

        List<MessageHandlingMember<?>> messageHandlingMembers = detectedClasses
                .stream()
                .flatMap(beanType ->
                         {
                             AnnotatedHandlerInspector<?> inspector = AnnotatedHandlerInspector.inspectType(
                                     beanType,
                                     MultiParameterResolverFactory.ordered(
                                             ClasspathParameterResolverFactory.forClass(beanType),
                                             new LenientParameterResolver()
                                     ));
                             return Stream.concat(inspector.getAllHandlers().values().stream(),
                                                  inspector.getAllInterceptors().values().stream());
                         })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return new MessageHandlerContribution(detectedClasses, messageHandlingMembers);
    }

    private void registerAggregateMembers(Class<?> entityType, Set<Class<?>> reflectiveClasses) {
        if (!reflectiveClasses.add(entityType)) {
            return;
        }

        ReflectionUtils.fieldsOf(entityType).forEach(field -> {
            Optional<Map<String, Object>> annotationAttributes = AnnotationUtils.findAnnotationAttributes(field,
                                                                                                          AggregateMember.class);
            if (annotationAttributes.isPresent()) {
                Class<?> declaredType = (Class<?>) annotationAttributes.get().get("type");
                Class<?> forwardingMode = (Class<?>) annotationAttributes.get().get("eventForwardingMode");
                reflectiveClasses.add(forwardingMode);

                if (declaredType != Void.class) {
                    registerAggregateMembers(declaredType, reflectiveClasses);
                } else if (Map.class.isAssignableFrom(field.getType())) {
                    Optional<Class<?>> type = ReflectionUtils.resolveMemberGenericType(field, 1);
                    type.ifPresent(t -> registerAggregateMembers(t, reflectiveClasses));
                } else if (Collection.class.isAssignableFrom(field.getType())) {
                    Optional<Class<?>> type = ReflectionUtils.resolveMemberGenericType(field, 0);
                    type.ifPresent(t -> registerAggregateMembers(t, reflectiveClasses));
                } else {
                    registerAggregateMembers(field.getType(), reflectiveClasses);
                }
            }
        });
    }

    /**
     * Trick to return a Class of a generic type
     *
     * @param <T> The generic type - anything works, as long as it's a Message
     * @return Message.class, gift-wrapped in generics
     */
    @SuppressWarnings("unchecked")
    private <T> Class<T> messageType() {
        return (Class<T>) Message.class;
    }

    private static class MessageHandlerContribution implements BeanFactoryInitializationAotContribution {

        private final BindingReflectionHintsRegistrar registrar = new BindingReflectionHintsRegistrar();

        private final Set<Class<?>> messageHandlingClasses;

        private final List<MessageHandlingMember<?>> messageHandlingMembers;

        public MessageHandlerContribution(
                Set<Class<?>> messageHandlingClasses,
                List<MessageHandlingMember<?>> messageHandlingMembers) {
            this.messageHandlingClasses = messageHandlingClasses;
            this.messageHandlingMembers = messageHandlingMembers;
        }

        @Override
        public void applyTo(GenerationContext generationContext,
                            BeanFactoryInitializationCode beanFactoryInitializationCode) {
            ReflectionHints reflectionHints = generationContext.getRuntimeHints().reflection();
            messageHandlingClasses.forEach(c -> registrar.registerReflectionHints(reflectionHints, c));
            messageHandlingMembers.forEach(m -> {
                m.unwrap(Method.class).ifPresent(mm -> reflectionHints.registerMethod(mm, ExecutableMode.INVOKE));
                m.unwrap(Constructor.class).ifPresent(mm -> reflectionHints.registerConstructor(mm,
                                                                                                ExecutableMode.INVOKE));
                registrar.registerReflectionHints(reflectionHints, m.payloadType());
                if (m instanceof QueryHandlingMember<?> queryHandlingMember) {
                    registrar.registerReflectionHints(reflectionHints, queryHandlingMember.getResultType());
                }
            });
        }
    }

    @Priority(Priority.LAST)
    private static class LenientParameterResolver implements ParameterResolverFactory, ParameterResolver<Object> {

        @Override
        public ParameterResolver<Object> createInstance(Executable executable,
                                                        Parameter[] parameters,
                                                        int parameterIndex) {
            return this;
        }

        @Override
        public Object resolveParameterValue(Message message) {
            throw new UnsupportedOperationException(
                    "This parameter resolver is not mean for production use. Only for detecting handler methods.");
        }

        @Override
        public boolean matches(Message message) {
            return true;
        }
    }
}
