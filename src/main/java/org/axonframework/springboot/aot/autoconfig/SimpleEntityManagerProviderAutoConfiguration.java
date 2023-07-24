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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * Configuration to provide a {@link SimpleEntityManagerProvider} instead of the
 * {@code ContainerManagedEntityManagerProvider} which doesn't work when compiled ahead of time.
 *
 * @author Gerard Klijs
 * @since 4.8.0
 */
@AutoConfiguration
@AutoConfigureBefore(name = "org.axonframework.springboot.autoconfig.JpaAutoConfiguration")
@ConditionalOnBean(EntityManager.class)
public class SimpleEntityManagerProviderAutoConfiguration {

    @Bean
    public EntityManagerProvider entityManagerProvider(EntityManager entityManager) {
        return new SimpleEntityManagerProvider(entityManager);
    }
}
