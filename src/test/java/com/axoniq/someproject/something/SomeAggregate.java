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

import com.axoniq.someproject.api.AddChildToListCommand;
import com.axoniq.someproject.api.AddChildToMapCommand;
import com.axoniq.someproject.api.ChangeStatusCommand;
import com.axoniq.someproject.api.ChildAddedToListEvent;
import com.axoniq.someproject.api.ChildAddedToMapEvent;
import com.axoniq.someproject.api.SomeCommand;
import com.axoniq.someproject.api.SomeEvent;
import com.axoniq.someproject.api.StatusChangedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.Message;
import org.axonframework.messaging.interceptors.ExceptionHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateMember;
import org.axonframework.modelling.command.AggregateRoot;
import org.axonframework.modelling.command.CommandHandlerInterceptor;
import org.axonframework.modelling.command.ForwardMatchingInstances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@AggregateRoot(type = "some_aggregate")
public class SomeAggregate {

    @AggregateMember(eventForwardingMode = ForwardMatchingInstances.class)
    private final List<SomeAggregateChild> childList = new ArrayList<>();
    @AggregateMember
    private final Map<String, SomeAggregateChild> childMap = new HashMap<>();
    @AggregateIdentifier
    private String id;
    private String status;
    @AggregateMember
    private SingleAggregateChild child;

    @CommandHandler
    public SomeAggregate(SomeCommand command) {
        apply(new SomeEvent(command.id()));
    }

    public SomeAggregate() {
        // Required by Axon to construct an empty instance to initiate Event Sourcing.
    }

    @ExceptionHandler
    public void exceptionHandler(Exception error) throws Exception {
        throw error;
    }

    @CommandHandlerInterceptor
    public Object intercept(Message<?> message, InterceptorChain chain) throws Exception {
        return chain.proceed();
    }

    @CommandHandler
    public void handle(ChangeStatusCommand command) {
        if (Objects.equals(status, command.newStatus())) {
            throw new IllegalStateException("new state should be different than current state");
        }
        apply(new StatusChangedEvent(command.id(), command.newStatus()));
    }

    @CommandHandler
    public void handleAddToList(AddChildToListCommand command) {
        apply(new ChildAddedToListEvent(command.id(), command.property()));
    }

    @CommandHandler
    public void handleAddToMap(AddChildToMapCommand command) {
        apply(new ChildAddedToMapEvent(command.id(), command.key(), command.property()));
    }

    @EventSourcingHandler
    protected void onSomeEvent(SomeEvent event) {
        this.id = event.id();
    }

    @EventSourcingHandler
    protected void onStatusChangedEvent(StatusChangedEvent event) {
        this.status = event.newStatus();
    }

    @EventSourcingHandler
    protected void onAddedToList(ChildAddedToListEvent event) {
        this.childList.add(new SomeAggregateChild(event.id(), event.property()));
    }

    @EventSourcingHandler
    protected void onAddedToMap(ChildAddedToMapEvent event) {
        this.childMap.put(event.key(), new SomeAggregateChild(event.id(), event.property()));
    }
}
