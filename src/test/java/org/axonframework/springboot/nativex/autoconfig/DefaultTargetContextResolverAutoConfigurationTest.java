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

package org.axonframework.springboot.nativex.autoconfig;

import org.axonframework.axonserver.connector.TargetContextResolver;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the {@link DefaultTargetContextResolverAutoConfiguration} providing a {@link DefaultTargetContextResolver}.
 *
 * @author Gerard Klijs
 */
class DefaultTargetContextResolverAutoConfigurationTest {

    @Test
    void defaultContextResolverIsPresent() {
        new ApplicationContextRunner()
                .withUserConfiguration(TestContext.class)
                .run(context -> {
                    TargetContextResolver<?> resolver = context.getBean(TargetContextResolver.class);
                    assertNotNull(resolver);
                    assertTrue(resolver instanceof DefaultTargetContextResolver);
                });
    }


    @ContextConfiguration
    @EnableAutoConfiguration
    private static class TestContext {

    }
}
