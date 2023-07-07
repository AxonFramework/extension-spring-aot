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

import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.axonframework.axonserver.connector.TargetContextResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Configuration to help enabling native compilation. For example by preventing the use of lambda's.
 *
 * @author Gerard Klijs
 * @since 4.8.0
 */
@AutoConfiguration
@AutoConfigureBefore(name = "org.axonframework.springboot.autoconfig.AxonServerAutoConfiguration")
@ConditionalOnClass(AxonServerConfiguration.class)
public class NativeAutoConfiguration {

    @ConditionalOnMissingBean
    @Bean
    public TargetContextResolver<?> targetContextResolver() {
        return new DefaultTargetContextResolver<>(null);
    }
}
