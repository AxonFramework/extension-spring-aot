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
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.common.AxonConfigurationException;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.deadline.annotation.DeadlineHandler;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.GapAwareTrackingToken;
import org.axonframework.eventhandling.GlobalSequenceTrackingToken;
import org.axonframework.eventhandling.MergedTrackingToken;
import org.axonframework.eventhandling.MultiSourceTrackingToken;
import org.axonframework.eventhandling.ReplayToken;
import org.axonframework.eventhandling.tokenstore.ConfigToken;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.messaging.responsetypes.InstanceResponseType;
import org.axonframework.messaging.responsetypes.MultipleInstancesResponseType;
import org.axonframework.messaging.responsetypes.OptionalResponseType;
import org.axonframework.modelling.saga.SagaEventHandler;
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
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is used to set runtime hints for axon applications. It finds the root package, and starts from there to
 * search vor axon classes, and add reflection hints. From those classes it tries to find all the api classes, which
 * need to have there constructors and methods available for serialisation. It does a few additional things which are
 * needed to make it work.
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
        registerReflectionHints(axonClasses, reflectionHints);
        Set<Class<?>> apiClasses = new HashSet<>(axonSerializableClasses());
        addClassesUsedInAggregateConstructors(apiClasses, axonClasses);
        addClassesUsedInHandlers(apiClasses, axonClasses);
        apiClasses.forEach(c -> registerForSerialisation(reflectionHints, c));
        hints.resources().registerPattern("axonserver_download.txt");
        hints.resources().registerPattern("SQLErrorCode.properties");
        hints.proxies().registerJdkProxy(Connection.class);
        hints.proxies().registerJdkProxy(TypeReference.of(
                "org.axonframework.common.jdbc.UnitOfWorkAwareConnectionProviderWrapper$UoWAttachedConnection"));
    }

    private void registerGrpcHints(ReflectionHints hints) {
        hints.registerType(EpollChannelOption.class, MemberCategory.DECLARED_FIELDS);
    }

    private List<Class<?>> getAxonClasses(ClassLoader classLoader) {
        return getClasses(classLoader, getRootPackage())
                .stream()
                .filter(this::isAxonClass)
                .toList();
    }

    private void registerReflectionHints(List<Class<?>> axonClasses, ReflectionHints hints) {
        hints
                .registerTypes(axonClasses.stream().map(TypeReference::of).toList(),
                               TypeHint.builtWith(MemberCategory.PUBLIC_CLASSES,
                                                  MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                                                  MemberCategory.INVOKE_PUBLIC_METHODS,
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
                   .flatMap(c -> Arrays.stream(c.getConstructors()))
                   .forEach(c -> {
                       if (c.isAnnotationPresent(CommandHandler.class) && c.getParameterTypes().length > 0) {
                           apiClasses.add(c.getParameterTypes()[0]);
                       }
                   });
    }

    private void addClassesUsedInHandlers(Set<Class<?>> apiClasses, List<Class<?>> axonClasses) {
        axonClasses.stream()
                   .flatMap(c -> Arrays.stream(c.getMethods()))
                   .filter(this::isAxonMethod)
                   .forEach(m -> {
                       if (m.getParameterTypes().length > 0) {
                           apiClasses.add(m.getParameterTypes()[0]);
                       }
                       if (m.isAnnotationPresent(QueryHandler.class)) {
                           Class<?> returnType = m.getReturnType();
                           if (!Collection.class.isAssignableFrom(returnType)) {
                               apiClasses.add(returnType);
                           }
                       }
                   });
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
        return clazz.isAnnotationPresent(Aggregate.class) ||
                clazz.isAnnotationPresent(ProcessingGroup.class) ||
                clazz.isAnnotationPresent(Saga.class) ||
                Arrays.stream(clazz.getMethods()).anyMatch(this::isAxonMethod);
    }

    private boolean isAxonMethod(Method method) {
        return method.isAnnotationPresent(EventHandler.class) ||
                method.isAnnotationPresent(QueryHandler.class) ||
                method.isAnnotationPresent(CommandHandler.class) ||
                method.isAnnotationPresent(EventSourcingHandler.class) ||
                method.isAnnotationPresent(SagaEventHandler.class) ||
                method.isAnnotationPresent(DeadlineHandler.class);
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
