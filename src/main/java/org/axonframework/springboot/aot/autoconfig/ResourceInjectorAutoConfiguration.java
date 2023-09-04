package org.axonframework.springboot.aot.autoconfig;

import org.axonframework.config.Configuration;
import org.axonframework.config.ConfigurationResourceInjector;
import org.axonframework.modelling.saga.ResourceInjector;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(beforeName = "org.axonframework.springboot.autoconfig.InfraConfiguration")
@ConditionalOnClass({ResourceInjector.class, Configuration.class})
public class ResourceInjectorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ResourceInjector resourceInjector(Configuration axonConfig) {
        return new ConfigurationResourceInjector(axonConfig);
    }
}
