/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai.chat;

import static org.wildfly.extension.ai.AIAttributeDefinitions.API_KEY;
import static org.wildfly.extension.ai.AIAttributeDefinitions.BASE_URL;
import static org.wildfly.extension.ai.AIAttributeDefinitions.CONNECT_TIMEOUT;
import static org.wildfly.extension.ai.AIAttributeDefinitions.LOG_REQUESTS;
import static org.wildfly.extension.ai.AIAttributeDefinitions.LOG_RESPONSES;
import static org.wildfly.extension.ai.AIAttributeDefinitions.MAX_TOKEN;
import static org.wildfly.extension.ai.AIAttributeDefinitions.MODEL_NAME;
import static org.wildfly.extension.ai.AIAttributeDefinitions.TEMPERATURE;
import static org.wildfly.extension.ai.Capabilities.CHAT_MODEL_PROVIDER_CAPABILITY;

import java.util.Collection;
import java.util.List;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.dmr.ModelType;

import static org.wildfly.extension.ai.AIAttributeDefinitions.RESPONSE_FORMAT;
import static org.wildfly.extension.ai.AIAttributeDefinitions.STREAMING;
import static org.wildfly.extension.ai.AIAttributeDefinitions.TOP_P;

import org.wildfly.extension.ai.injection.chat.WildFlyChatModelConfig;
import org.wildfly.service.capture.ValueExecutorRegistry;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

public class OpenAIChatLanguageModelProviderRegistrar implements ChildResourceDefinitionRegistrar {
    public static final SimpleAttributeDefinition FREQUENCY_PENALTY = new SimpleAttributeDefinitionBuilder("frequency-penalty", ModelType.DOUBLE, true)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition ORGANIZATION_ID = new SimpleAttributeDefinitionBuilder("organization-id", ModelType.STRING, true)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition PRESENCE_PENALTY = new SimpleAttributeDefinitionBuilder("presence-penalty", ModelType.DOUBLE, true)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition SEED = new SimpleAttributeDefinitionBuilder("seed", ModelType.INT, true)
            .setAllowExpression(true)
            .build();


    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(API_KEY, BASE_URL, CONNECT_TIMEOUT,
            FREQUENCY_PENALTY, LOG_REQUESTS, LOG_RESPONSES, MAX_TOKEN, MODEL_NAME, ORGANIZATION_ID, PRESENCE_PENALTY, 
            RESPONSE_FORMAT, SEED, STREAMING, TEMPERATURE, TOP_P);

    private final ResourceRegistration registration;
    private final ResourceDescriptor descriptor;
    static final String NAME = "openai-chat-model";
    public static final PathElement PATH = PathElement.pathElement(NAME);
    private final ValueExecutorRegistry<String, WildFlyChatModelConfig> registry = ValueExecutorRegistry.newInstance();

    public OpenAIChatLanguageModelProviderRegistrar(ParentResourceDescriptionResolver parentResolver) {
        this.registration = ResourceRegistration.of(PATH);
        this.descriptor = ResourceDescriptor.builder(parentResolver.createChildResolver(PATH))
                .addCapability(CHAT_MODEL_PROVIDER_CAPABILITY)
                .addAttributes(ATTRIBUTES)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(new OpenAIChatModelProviderServiceConfigurator(registry)))
                .build();
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDefinition definition = ResourceDefinition.builder(this.registration, this.descriptor.getResourceDescriptionResolver()).build();
        ManagementResourceRegistration resourceRegistration = parent.registerSubModel(definition);
        ChatModelConnectionCheckerOperationHandler.register(resourceRegistration, descriptor, registry);
        resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required("dev.langchain4j.openai"));
        ManagementResourceRegistrar.of(this.descriptor).register(resourceRegistration);
        return resourceRegistration;
    }
}
