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

import jakarta.persistence.EntityManager;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.jpa.SimpleEntityManagerProvider;
import org.axonframework.config.ConfigurationResourceInjector;
import org.axonframework.modelling.saga.ResourceInjector;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the {@link SimpleEntityManagerProviderAutoConfiguration} providing a {@link SimpleEntityManagerProvider}.
 *
 * @author Gerard Klijs
 */
class ResourceInjectorAutoConfigurationTest {

    @Test
    void defaultContextResolverIsPresent() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestContext.class)
                .withPropertyValues("axon.axonserver.enabled=false")
                .run(context -> {
                    ResourceInjector resourceInjector = context.getBean(ResourceInjector.class);
                    assertNotNull(resourceInjector);
                    assertTrue(resourceInjector instanceof ConfigurationResourceInjector);
                });
    }


    @ContextConfiguration
    @EnableAutoConfiguration
    private static class TestContext {

    }
}
