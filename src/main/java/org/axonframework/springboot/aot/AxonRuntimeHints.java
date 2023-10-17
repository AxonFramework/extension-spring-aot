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

import org.axonframework.eventhandling.GapAwareTrackingToken;
import org.axonframework.eventhandling.GlobalSequenceTrackingToken;
import org.axonframework.eventhandling.MergedTrackingToken;
import org.axonframework.eventhandling.MultiSourceTrackingToken;
import org.axonframework.eventhandling.ReplayToken;
import org.axonframework.eventhandling.tokenstore.ConfigToken;
import org.axonframework.messaging.responsetypes.InstanceResponseType;
import org.axonframework.messaging.responsetypes.MultipleInstancesResponseType;
import org.axonframework.messaging.responsetypes.OptionalResponseType;
import org.axonframework.modelling.saga.MetaDataAssociationResolver;
import org.axonframework.modelling.saga.PayloadAssociationResolver;
import org.axonframework.modelling.saga.repository.jpa.SagaEntry;
import org.axonframework.modelling.saga.repository.jpa.SerializedSaga;
import org.axonframework.serialization.SerializedMessage;
import org.axonframework.serialization.SerializedMetaData;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.Nullable;

import java.lang.reflect.Type;
import java.sql.Connection;

/**
 * This class is used to set runtime hints for axon applications. It sets reflection hints for some classes which needs
 * to be deserialized. It also gives some hints for commonly used files.
 *
 * @author Gerard Klijs
 * @since 4.8.0
 */
public class AxonRuntimeHints implements RuntimeHintsRegistrar {

    private final BindingReflectionHintsRegistrar registrar = new BindingReflectionHintsRegistrar();

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        ReflectionHints reflectionHints = hints.reflection();
        registerGrpcHints(reflectionHints);
        registrar.registerReflectionHints(reflectionHints, axonSerializableClasses());
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
                MemberCategory.PUBLIC_FIELDS);
    }

    private Type[] axonSerializableClasses() {
        return new Type[]{
                SagaEntry.class,
                SerializedSaga.class,
                SerializedMessage.class,
                SerializedMetaData.class,
                GlobalSequenceTrackingToken.class,
                GapAwareTrackingToken.class,
                MergedTrackingToken.class,
                MultiSourceTrackingToken.class,
                ReplayToken.class,
                ConfigToken.class,
                InstanceResponseType.class,
                OptionalResponseType.class,
                MultipleInstancesResponseType.class,
                PayloadAssociationResolver.class,
                MetaDataAssociationResolver.class
        };
    }
}
