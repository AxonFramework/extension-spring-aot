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

import org.axonframework.config.Configuration;
import org.axonframework.config.ConfigurationResourceInjector;
import org.axonframework.modelling.saga.ResourceInjector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration class that configures the ConfigurationResourceInjector rather than the framework-provided
 * {@link org.axonframework.spring.saga.SpringResourceInjector}. In native-compiled applications, the logic used by the
 * SpringResourceInjector isn't available.
 *
 * @author Allard Buijze
 */
@AutoConfiguration(beforeName = "org.axonframework.springboot.autoconfig.InfraConfiguration")
@ConditionalOnClass({ResourceInjector.class, Configuration.class})
public class ResourceInjectorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResourceInjector resourceInjector(Configuration axonConfig) {
        return new ConfigurationResourceInjector(axonConfig);
    }
}
