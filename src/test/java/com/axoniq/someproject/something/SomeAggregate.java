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

import com.axoniq.someproject.api.ChangeStatusCommand;
import com.axoniq.someproject.api.SomeCommand;
import com.axoniq.someproject.api.SomeEvent;
import com.axoniq.someproject.api.StatusChangedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.Objects;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
public class SomeAggregate {

    @AggregateIdentifier
    private String id;
    private String status;

    @CommandHandler
    public SomeAggregate(SomeCommand command) {
        apply(new SomeEvent(command.id()));
    }

    @CommandHandler
    public void handle(ChangeStatusCommand command) {
        if (Objects.equals(status, command.newStatus())) {
            throw new IllegalStateException("new state should be different than current state");
        }
        apply(new StatusChangedEvent(command.id(), command.newStatus()));
    }

    @EventSourcingHandler
    protected void onSomeEvent(SomeEvent event) {
        this.id = event.id();
    }

    @EventSourcingHandler
    protected void onStatusChangedEvent(StatusChangedEvent event) {
        this.status = event.newStatus();
    }


    public SomeAggregate() {
        // Required by Axon to construct an empty instance to initiate Event Sourcing.
    }
}
