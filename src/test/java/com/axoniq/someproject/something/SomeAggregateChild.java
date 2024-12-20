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

package com.axoniq.someproject.something;

import com.axoniq.someproject.SomeBean;
import com.axoniq.someproject.api.SomeChildCommand;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.modelling.command.EntityId;

public record SomeAggregateChild(
        @EntityId String id,
        String property
) {

    @CommandHandler
    public void handle(SomeChildCommand command, SomeBean someBean) {
        //left empty to not overcomplicate things
    }
}
