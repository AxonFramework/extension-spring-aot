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

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.common.annotation.AnnotationUtils;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.GapAwareTrackingToken;
import org.axonframework.eventhandling.GlobalSequenceTrackingToken;
import org.axonframework.eventhandling.MergedTrackingToken;
import org.axonframework.eventhandling.MultiSourceTrackingToken;
import org.axonframework.eventhandling.ReplayToken;
import org.axonframework.eventhandling.tokenstore.ConfigToken;
import org.axonframework.messaging.annotation.MessageHandler;
import org.axonframework.messaging.responsetypes.InstanceResponseType;
import org.axonframework.messaging.responsetypes.MultipleInstancesResponseType;
import org.axonframework.messaging.responsetypes.OptionalResponseType;
import org.axonframework.modelling.command.AggregateMember;
import org.axonframework.modelling.saga.repository.jpa.SagaEntry;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.spring.stereotype.Aggregate;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is used to set runtime hints for axon applications. It finds the root package, and starts from there to
 * search for Axon Framework classes, and add reflection hints. From those classes it tries to find all the API classes, which
 * need to have there constructors and methods available for serialization.
 *
 * @author Gerard Klijs
 * @since 4.8.0
 */
public class AxonRuntimeHints implements RuntimeHintsRegistrar {

    private static final String PACKAGE_SEPARATOR = ".";
    private static final String FOLDER_SEPARATOR = "/";

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        ReflectionHints reflectionHints = hints.reflection();
        registerGrpcHints(reflectionHints);
        List<Class<?>> axonClasses = getAxonClasses(classLoader);
        List<Class<?>> aggregateMembers = getAggregateMembers(axonClasses);
        axonClasses = Stream.concat(axonClasses.stream(), aggregateMembers.stream()).toList();
        registerReflectionHints(axonClasses, reflectionHints);
        Set<Class<?>> apiClasses = new HashSet<>(axonSerializableClasses());
        addClassesUsedInAggregateConstructors(apiClasses, axonClasses);
        addClassesUsedInHandlers(apiClasses, axonClasses);
        apiClasses.forEach(c -> registerForSerialisation(reflectionHints, c));
        hints.resources().registerPattern("axonserver_download.txt");
        hints.resources().registerPattern("SQLErrorCode.properties");
        hints.proxies().registerJdkProxy(
                TypeReference.of(Connection.class),
                TypeReference.of(
                        "org.axonframework.common.jdbc.UnitOfWorkAwareConnectionProviderWrapper$UoWAttachedConnection"));
    }

    private void registerGrpcHints(ReflectionHints hints) {
        hints.registerType(
                TypeReference.of("io.netty.channel.epoll.EpollChannelOption"),
                MemberCategory.DECLARED_FIELDS);
    }

    private List<Class<?>> getAxonClasses(ClassLoader classLoader) {
        return getClasses(classLoader, getRootPackage())
                .stream()
                .filter(this::isAxonClass)
                .toList();
    }

    private List<Class<?>> getAggregateMembers(List<Class<?>> axonClasses) {
        List<Class<?>> aggregateMembers = new ArrayList<>();
        axonClasses.forEach(c -> {
            if (AnnotationUtils.isAnnotationPresent(c, Aggregate.class)) {
                Arrays.stream(c.getDeclaredFields()).forEach(
                        field -> getAggregateMember(field).ifPresent(aggregateMembers::add)
                );
            }
        });
        return aggregateMembers;
    }

    private Optional<Class<?>> getAggregateMember(Field field) {
        if (AnnotationUtils.isAnnotationPresent(field, AggregateMember.class)) {
            return extractType(field);
        }
        return Optional.empty();
    }

    private Optional<Class<?>> extractType(Field field) {
        Class<?> type = field.getType();
        if (Collection.class.isAssignableFrom(type)) {
            return getCollectionTypeFromType(field.getGenericType());
        } else if (Map.class.isAssignableFrom(type)) {
            return getMapTypeFromType(field.getGenericType());
        } else {
            return Optional.of(type);
        }
    }

    private void registerReflectionHints(List<Class<?>> axonClasses, ReflectionHints hints) {
        hints
                .registerTypes(axonClasses.stream().map(TypeReference::of).toList(),
                               TypeHint.builtWith(MemberCategory.PUBLIC_CLASSES,
                                                  MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                                  MemberCategory.INVOKE_DECLARED_METHODS,
                                                  MemberCategory.INTROSPECT_DECLARED_METHODS,
                                                  MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS,
                                                  MemberCategory.DECLARED_FIELDS));
    }

    private void registerForSerialisation(ReflectionHints hints, Class<?> c) {
        hints.registerType(c,
                           MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                           MemberCategory.INVOKE_PUBLIC_METHODS);
    }

    private List<Class<?>> axonSerializableClasses() {
        return List.of(SagaEntry.class,
                       GlobalSequenceTrackingToken.class,
                       GapAwareTrackingToken.class,
                       MergedTrackingToken.class,
                       MultiSourceTrackingToken.class,
                       ReplayToken.class,
                       ConfigToken.class,
                       InstanceResponseType.class,
                       OptionalResponseType.class,
                       MultipleInstancesResponseType.class);
    }

    private void addClassesUsedInAggregateConstructors(Set<Class<?>> apiClasses, List<Class<?>> axonClasses) {
        axonClasses.stream()
                   .flatMap(c -> Arrays.stream(c.getDeclaredConstructors()))
                   .forEach(c -> {
                       if (AnnotationUtils.isAnnotationPresent(c, CommandHandler.class)
                               && c.getParameterTypes().length > 0) {
                           apiClasses.add(c.getParameterTypes()[0]);
                       }
                   });
    }

    private void addClassesUsedInHandlers(Set<Class<?>> apiClasses, List<Class<?>> axonClasses) {
        axonClasses.stream()
                   .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                   .filter(this::isAxonHandlerMethod)
                   .forEach(m -> {
                       if (m.getParameterTypes().length > 0) {
                           apiClasses.add(m.getParameterTypes()[0]);
                       }
                       if (AnnotationUtils.isAnnotationPresent(m, QueryHandler.class)) {
                           Class<?> returnClass = m.getReturnType();
                           if (!Collection.class.isAssignableFrom(returnClass)) {
                               apiClasses.add(returnClass);
                           } else {
                               getCollectionTypeFromType(m.getGenericReturnType()).ifPresent(apiClasses::add);
                           }
                       }
                   });
    }

    private Optional<Class<?>> getCollectionTypeFromType(Type type) {
        return getTypeArgument(type, 0);
    }

    private Optional<Class<?>> getMapTypeFromType(Type type) {
        return getTypeArgument(type, 1);
    }

    private Optional<Class<?>> getTypeArgument(Type type, int index) {
        if (type instanceof ParameterizedType paramType) {
            Type[] argTypes = paramType.getActualTypeArguments();
            if (argTypes.length > index && argTypes[index] instanceof Class<?> clazz) {
                return Optional.of(clazz);
            }
        }
        return Optional.empty();
    }

    private Set<Class<?>> getClasses(ClassLoader classLoader, String packageName) {
        InputStream stream = classLoader.getResourceAsStream(
                packageName.replaceAll("[" + PACKAGE_SEPARATOR + "]", FOLDER_SEPARATOR)
        );
        assert stream != null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        List<String> lines = reader.lines().toList();

        Stream<Class<?>> current = lines.stream()
                                        .filter(this::isClassFile)
                                        .map(line -> getClass(line, packageName));

        Stream<Class<?>> nested = lines.stream()
                                       .filter(this::isPackageFolder)
                                       .map(String::trim)
                                       .map(child -> setChildPackageName(packageName, child))
                                       .map(pName -> getClasses(classLoader, pName))
                                       .flatMap(Set::stream);

        return Stream.concat(current, nested).collect(Collectors.toSet());
    }

    private Class<?> getClass(String className, String packageName) {
        try {
            return Class.forName(
                    packageName + PACKAGE_SEPARATOR + className.substring(0, className.lastIndexOf(PACKAGE_SEPARATOR)));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private boolean isClassFile(String path) {
        return path.endsWith(".class");
    }

    private boolean isPackageFolder(String path) {
        return !isClassFile(path);
    }

    private String setChildPackageName(String parent, String child) {
        return parent + PACKAGE_SEPARATOR + child;
    }

    private boolean isAxonClass(Class<?> clazz) {
        return AnnotationUtils.isAnnotationPresent(clazz, Aggregate.class) ||
                AnnotationUtils.isAnnotationPresent(clazz, ProcessingGroup.class) ||
                AnnotationUtils.isAnnotationPresent(clazz, Saga.class) ||
                Arrays.stream(clazz.getMethods()).anyMatch(this::isAxonHandlerMethod);
    }

    private boolean isAxonHandlerMethod(Method method) {
        return AnnotationUtils.isAnnotationPresent(method, MessageHandler.class);
    }

    private String getRootPackage() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(SpringBootApplication.class));
        for (BeanDefinition bd : scanner.findCandidateComponents("")) {
            String beanClassName = bd.getBeanClassName();
            if (Objects.isNull(beanClassName)) {
                throw new AxonConfigurationException(
                        "Could not find get bean class name of SpringBootApplication annotated class.");
            } else {
                return beanClassName.split("\\.(?=[^.]*$)")[0];
            }
        }
        throw new AxonConfigurationException("Could not find @SpringBootApplication annotated class.");
    }
}
