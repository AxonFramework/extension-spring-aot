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
import org.axonframework.messaging.Message;

/**
 * Context resolver that doesn't rely on lambda's, so it works also when compiling to native.
 *
 * @param <T> the type of {@link Message} to resolve the context for
 * @author Gerard Klijs
 * @since 4.8.0
 */
public class DefaultTargetContextResolver<T extends Message<?>> implements TargetContextResolver<T> {

    private final String context;

    /**
     * Creates a new instance, which will always return the given value.
     *
     * @param context the {@link String} to return
     */
    public DefaultTargetContextResolver(String context) {
        this.context = context;
    }

    @Override
    public String resolveContext(T message) {
        return context;
    }
}
