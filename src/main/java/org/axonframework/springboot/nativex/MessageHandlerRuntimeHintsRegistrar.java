package org.axonframework.springboot.nativex;

import org.axonframework.messaging.Message;
import org.axonframework.messaging.annotation.AnnotatedHandlerInspector;
import org.axonframework.messaging.annotation.MessageHandlingMember;
import org.axonframework.spring.config.MessageHandlerLookup;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * BeanFactoryInitializationAotProcessor that registers message handler methods declared on beans for reflection. This
 * means that all methods annotated with {@code @MessageHandler} will be available for reflection.
 * <p/>
 * Additionally, the payload types for these methods are registered for reflective access.
 *
 * @author Allard Buijze
 * @since 4.8.0
 */
public class MessageHandlerRuntimeHintsRegistrar implements BeanFactoryInitializationAotProcessor {
    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        @SuppressWarnings("unchecked")
        List<MessageHandlingMember<?>> messageHandlingMembers =
                MessageHandlerLookup.messageHandlerBeans(messageType(), beanFactory, true).stream()
                                    .map(beanFactory::getType)
                                    .distinct()
                                    .flatMap(beanType -> AnnotatedHandlerInspector.inspectType(beanType).getAllHandlers().values().stream())
                                    .flatMap(Collection::stream)
                                    .collect(Collectors.<MessageHandlingMember<?>>toList());

        return new PayloadTypesContribution(messageHandlingMembers);
    }

    /**
     * Trick to return a Class of a generic type
     *
     * @param <T> The generic type - anything works, as long as it's a Message
     *
     * @return Message.class, gift-wrapped in generics
     */
    @SuppressWarnings("unchecked")
    private <T> Class<T> messageType() {
        return (Class<T>) Message.class;
    }

    private static class PayloadTypesContribution implements BeanFactoryInitializationAotContribution {

        private final BindingReflectionHintsRegistrar registrar = new BindingReflectionHintsRegistrar();

        private final List<MessageHandlingMember<?>> messageHandlingMembers;

        public PayloadTypesContribution(List<MessageHandlingMember<?>> messageHandlingMembers) {
            this.messageHandlingMembers = messageHandlingMembers;
        }

        @Override
        public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
            messageHandlingMembers.forEach(m -> {
                m.unwrap(Method.class).ifPresent(mm -> generationContext.getRuntimeHints().reflection().registerMethod(mm, ExecutableMode.INVOKE));
                m.unwrap(Constructor.class).ifPresent(mm -> generationContext.getRuntimeHints().reflection().registerConstructor(mm, ExecutableMode.INVOKE));
                registrar.registerReflectionHints(generationContext.getRuntimeHints().reflection(), m.payloadType());
            });

        }
    }
}
