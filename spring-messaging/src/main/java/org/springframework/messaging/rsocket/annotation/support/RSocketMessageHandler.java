/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.rsocket.annotation.support;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.frame.FrameType;
import org.springframework.beans.BeanUtils;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.reactive.MessageMappingMessageHandler;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
import org.springframework.messaging.rsocket.ClientRSocketFactoryConfigurer;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.util.*;
import reactor.core.publisher.Mono;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Extension of {@link MessageMappingMessageHandler} for use in RSocket as a
 * responder that handles requests with {@link ConnectMapping @ConnectMapping}
 * and {@link MessageMapping @MessageMapping} methods.
 * <p>For RSocket servers use {@link #serverResponder()} to obtain a
 * {@link SocketAcceptor} to register with
 * {@link io.rsocket.RSocketFactory.ServerRSocketFactory ServerRSocketFactory}.
 * <p>For RSocket clients use {@link #clientResponder()} to obtain an adapter
 * to register with
 * {@link io.rsocket.RSocketFactory.ClientRSocketFactory ClientRSocketFactory},
 * or use the static shortcut
 * {@link #clientResponder(RSocketStrategies, Object...)} to obtain a configurer
 * for {@link RSocketRequester.Builder#rsocketFactory}.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RSocketMessageHandler extends MessageMappingMessageHandler {

    private final List<Encoder<?>> encoders = new ArrayList<>();

    private RSocketStrategies strategies = RSocketStrategies.create();

    @Nullable
    private MimeType defaultDataMimeType;

    private MimeType defaultMetadataMimeType = MetadataExtractor.COMPOSITE_METADATA;


    public RSocketMessageHandler() {
        setRSocketStrategies(this.strategies);
    }

    /**
     * Static factory method for a configurer of a client side responder with
     * annotated handler methods. This is intended to be passed into
     * {@link RSocketRequester.Builder#rsocketFactory(ClientRSocketFactoryConfigurer)}.
     * <p>In effect a shortcut to create and initialize
     * {@code RSocketMessageHandler} with the given strategies and handlers,
     * and use {@link #clientResponder()} to obtain the responder.
     * For more advanced scenarios, e.g. discovering handlers through a custom
     * stereotype annotation, consider declaring {@code RSocketMessageHandler}
     * as a bean, and then obtain the responder from it.
     *
     * @param strategies        the strategies to set on the created
     *                          {@code RSocketMessageHandler}
     * @param candidateHandlers a list of Objects and/or Classes with annotated
     *                          handler methods; used to call {@link #setHandlers(List)} with
     *                          on the created {@code RSocketMessageHandler}
     * @return a configurer that may be passed into
     * {@link RSocketRequester.Builder#rsocketFactory(ClientRSocketFactoryConfigurer)}
     */
    public static ClientRSocketFactoryConfigurer clientResponder(
            RSocketStrategies strategies, Object... candidateHandlers) {

        Assert.notEmpty(candidateHandlers, "No handlers");
        List<Object> handlers = new ArrayList<>(candidateHandlers.length);
        for (Object obj : candidateHandlers) {
            handlers.add(obj instanceof Class ? BeanUtils.instantiateClass((Class<?>) obj) : obj);
        }

        return rsocketFactory -> {
            RSocketMessageHandler handler = new RSocketMessageHandler();
            handler.setHandlers(handlers);
            handler.setRSocketStrategies(strategies);
            handler.afterPropertiesSet();
            rsocketFactory.acceptor(handler.clientResponder());
        };
    }

    /**
     * Return the configured {@link #setEncoders(List) encoders}.
     */
    public List<? extends Encoder<?>> getEncoders() {
        return this.encoders;
    }

    /**
     * Configure the encoders to use for encoding handler method return values.
     * <p>When {@link #setRSocketStrategies(RSocketStrategies) rsocketStrategies}
     * is set, this property is re-initialized with the encoders in it, and
     * likewise when this property is set the {@code RSocketStrategies} are
     * mutated to change the encoders in it.
     * <p>By default this is set to the
     * {@link RSocketStrategies.Builder#encoder(Encoder[]) defaults} from
     * {@code RSocketStrategies}.
     */
    public void setEncoders(List<? extends Encoder<?>> encoders) {
        this.encoders.clear();
        this.encoders.addAll(encoders);
        this.strategies = this.strategies.mutate()
                .encoders(list -> {
                    list.clear();
                    list.addAll(encoders);
                })
                .build();
    }

    /**
     * {@inheritDoc}
     * <p>When {@link #setRSocketStrategies(RSocketStrategies) rsocketStrategies}
     * is set, this property is re-initialized with the decoders in it, and
     * likewise when this property is set the {@code RSocketStrategies} are
     * mutated to change the decoders in them.
     * <p>By default this is set to the
     * {@link RSocketStrategies.Builder#decoder(Decoder[]) defaults} from
     * {@code RSocketStrategies}.
     */
    @Override
    public void setDecoders(List<? extends Decoder<?>> decoders) {
        super.setDecoders(decoders);
        this.strategies = this.strategies.mutate()
                .decoders(list -> {
                    list.clear();
                    list.addAll(decoders);
                })
                .build();
    }

    /**
     * {@inheritDoc}
     * <p>When {@link #setRSocketStrategies(RSocketStrategies) rsocketStrategies}
     * is set, this property is re-initialized with the route matcher in it, and
     * likewise when this property is set the {@code RSocketStrategies} are
     * mutated to change the matcher in it.
     * <p>By default this is set to the
     * {@link RSocketStrategies.Builder#routeMatcher(RouteMatcher) defaults}
     * from {@code RSocketStrategies}.
     */
    @Override
    public void setRouteMatcher(@Nullable RouteMatcher routeMatcher) {
        super.setRouteMatcher(routeMatcher);
        this.strategies = this.strategies.mutate().routeMatcher(routeMatcher).build();
    }

    /**
     * Configure the registry for adapting various reactive types.
     * <p>When {@link #setRSocketStrategies(RSocketStrategies) rsocketStrategies}
     * is set, this property is re-initialized with the registry in it, and
     * likewise when this property is set the {@code RSocketStrategies} are
     * mutated to change the registry in it.
     * <p>By default this is set to the
     * {@link RSocketStrategies.Builder#reactiveAdapterStrategy(ReactiveAdapterRegistry) defaults}
     * from {@code RSocketStrategies}.
     */
    @Override
    public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
        super.setReactiveAdapterRegistry(registry);
        this.strategies = this.strategies.mutate().reactiveAdapterStrategy(registry).build();
    }

    /**
     * Return the configured {@link #setMetadataExtractor MetadataExtractor}.
     */
    public MetadataExtractor getMetadataExtractor() {
        return this.strategies.metadataExtractor();
    }

    /**
     * Configure a {@link MetadataExtractor} to extract the route along with
     * other metadata.
     * <p>When {@link #setRSocketStrategies(RSocketStrategies) rsocketStrategies}
     * is set, this property is re-initialized with the extractor in it, and
     * likewise when this property is set the {@code RSocketStrategies} are
     * mutated to change the extractor in it.
     * <p>By default this is set to the
     * {@link RSocketStrategies.Builder#metadataExtractor(MetadataExtractor)} defaults}
     * from {@code RSocketStrategies}.
     *
     * @param extractor the extractor to use
     */
    public void setMetadataExtractor(MetadataExtractor extractor) {
        this.strategies = this.strategies.mutate().metadataExtractor(extractor).build();
    }

    /**
     * Return the {@link #setRSocketStrategies configured} {@code RSocketStrategies}.
     */
    public RSocketStrategies getRSocketStrategies() {
        return this.strategies;
    }

    /**
     * Configure this handler through an {@link RSocketStrategies} instance which
     * can be re-used to initialize a client-side {@link RSocketRequester}.
     * <p>When this property is set, in turn it sets the following:
     * <ul>
     * <li>{@link #setDecoders(List)}
     * <li>{@link #setEncoders(List)}
     * <li>{@link #setRouteMatcher(RouteMatcher)}
     * <li>{@link #setMetadataExtractor(MetadataExtractor)}
     * <li>{@link #setReactiveAdapterRegistry(ReactiveAdapterRegistry)}
     * </ul>
     * <p>By default this is set to {@link RSocketStrategies#create()} which in
     * turn sets default settings for all related properties.
     */
    public void setRSocketStrategies(RSocketStrategies rsocketStrategies) {
        this.strategies = rsocketStrategies;
        this.encoders.clear();
        this.encoders.addAll(this.strategies.encoders());
        super.setDecoders(this.strategies.decoders());
        super.setRouteMatcher(this.strategies.routeMatcher());
        super.setReactiveAdapterRegistry(this.strategies.reactiveAdapterRegistry());
    }

    /**
     * Return the configured
     * {@link #setDefaultDataMimeType defaultDataMimeType}, or {@code null}.
     */
    @Nullable
    public MimeType getDefaultDataMimeType() {
        return this.defaultDataMimeType;
    }

    /**
     * Configure the default content type to use for data payloads if the
     * {@code SETUP} frame did not specify one.
     * <p>By default this is not set.
     *
     * @param mimeType the MimeType to use
     */
    public void setDefaultDataMimeType(@Nullable MimeType mimeType) {
        this.defaultDataMimeType = mimeType;
    }

    /**
     * Return the configured
     * {@link #setDefaultMetadataMimeType defaultMetadataMimeType}.
     */
    public MimeType getDefaultMetadataMimeType() {
        return this.defaultMetadataMimeType;
    }

    /**
     * Configure the default {@code MimeType} for payload data if the
     * {@code SETUP} frame did not specify one.
     * <p>By default this is set to {@code "message/x.rsocket.composite-metadata.v0"}
     *
     * @param mimeType the MimeType to use
     */
    public void setDefaultMetadataMimeType(MimeType mimeType) {
        Assert.notNull(mimeType, "'metadataMimeType' is required");
        this.defaultMetadataMimeType = mimeType;
    }

    @Override
    public void afterPropertiesSet() {

        // Add argument resolver before parent initializes argument resolution
        getArgumentResolverConfigurer().addCustomResolver(new RSocketRequesterMethodArgumentResolver());

        super.afterPropertiesSet();
    }

    @Override
    protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
        List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();
        handlers.add(new RSocketPayloadReturnValueHandler(this.encoders, getReactiveAdapterRegistry()));
        handlers.addAll(getReturnValueHandlerConfigurer().getCustomHandlers());
        return handlers;
    }

    @Override
    @Nullable
    protected CompositeMessageCondition getCondition(AnnotatedElement element) {
        MessageMapping ann1 = AnnotatedElementUtils.findMergedAnnotation(element, MessageMapping.class);
        if (ann1 != null && ann1.value().length > 0) {
            String[] patterns = processDestinations(ann1.value());
            return new CompositeMessageCondition(
                    RSocketFrameTypeMessageCondition.REQUEST_CONDITION,
                    new DestinationPatternsMessageCondition(patterns, obtainRouteMatcher()));
        }
        ConnectMapping ann2 = AnnotatedElementUtils.findMergedAnnotation(element, ConnectMapping.class);
        if (ann2 != null) {
            String[] patterns = processDestinations(ann2.value());
            return new CompositeMessageCondition(
                    RSocketFrameTypeMessageCondition.CONNECT_CONDITION,
                    new DestinationPatternsMessageCondition(patterns, obtainRouteMatcher()));
        }
        return null;
    }

    @Override
    protected void handleNoMatch(@Nullable RouteMatcher.Route destination, Message<?> message) {
        FrameType frameType = RSocketFrameTypeMessageCondition.getFrameType(message);
        if (frameType == FrameType.SETUP || frameType == FrameType.METADATA_PUSH) {
            return;  // optional handling
        }
        if (frameType == FrameType.REQUEST_FNF) {
            // Can't propagate error to client, so just log
            logger.warn("No handler for fireAndForget to '" + destination + "'");
            return;
        }
        throw new MessageDeliveryException("No handler for destination '" + destination + "'");
    }

    /**
     * Return an adapter for a server side
     * {@link io.rsocket.RSocketFactory.ServerRSocketFactory#acceptor(SocketAcceptor)
     * acceptor} that delegate to this {@link RSocketMessageHandler} for
     * handling.
     * <p>The initial {@link ConnectionSetupPayload} can be handled with a
     * {@link ConnectMapping @ConnectionMapping} method which can be asynchronous
     * and return {@code Mono<Void>} with an error signal preventing the
     * connection. Such a method can also start requests to the client but that
     * must be done decoupled from handling and from the current thread.
     * <p>Subsequent requests on the connection can be handled with
     * {@link MessageMapping MessageMapping} methods.
     */
    public SocketAcceptor serverResponder() {
        return (setupPayload, sendingRSocket) -> {
            MessagingRSocket responder;
            try {
                responder = createResponder(setupPayload, sendingRSocket);
            } catch (Throwable ex) {
                return Mono.error(ex);
            }
            return responder.handleConnectionSetupPayload(setupPayload).then(Mono.just(responder));
        };
    }

    /**
     * Return an adapter for a client side responder that can be used to set
     * {@link io.rsocket.RSocketFactory.ClientRSocketFactory#acceptor(Function)}.
     * The responder delegates requests to this {@code RSocketMessageHandler}
     * for handling via {@code @MessageMapping} methods.
     * <p>The initial {@link ConnectionSetupPayload} can be accessed through a
     * {@link ConnectMapping @ConnectionMapping} method, but such a method is
     * only a callback just before the connection is made and cannot "accept"
     * or prevent the connection. Such a method can also start requests to the
     * server but must do so decoupled from handling and the current thread.
     */
    public BiFunction<ConnectionSetupPayload, RSocket, RSocket> clientResponder() {
        return (setupPayload, sendingRSocket) -> {
            MessagingRSocket responder = createResponder(setupPayload, sendingRSocket);
            responder.handleConnectionSetupPayload(setupPayload).subscribe();
            return responder;
        };
    }

    private MessagingRSocket createResponder(ConnectionSetupPayload setupPayload, RSocket rsocket) {
        String str = setupPayload.dataMimeType();
        MimeType dataMimeType = StringUtils.hasText(str) ? MimeTypeUtils.parseMimeType(str) : this.defaultDataMimeType;
        Assert.notNull(dataMimeType, "No `dataMimeType` in ConnectionSetupPayload and no default value");
        Assert.isTrue(isDataMimeTypeSupported(dataMimeType), "Data MimeType '" + dataMimeType + "' not supported");

        str = setupPayload.metadataMimeType();
        MimeType metaMimeType = StringUtils.hasText(str) ? MimeTypeUtils.parseMimeType(str) : this.defaultMetadataMimeType;
        Assert.notNull(metaMimeType, "No `metadataMimeType` in ConnectionSetupPayload and no default value");

        RSocketRequester requester = RSocketRequester.wrap(rsocket, dataMimeType, metaMimeType, this.strategies);
        return new MessagingRSocket(dataMimeType, metaMimeType, getMetadataExtractor(),
                requester, this, obtainRouteMatcher(), this.strategies);
    }

    private boolean isDataMimeTypeSupported(MimeType dataMimeType) {
        for (Encoder<?> encoder : getEncoders()) {
            for (MimeType encodable : encoder.getEncodableMimeTypes()) {
                if (encodable.isCompatibleWith(dataMimeType)) {
                    return true;
                }
            }
        }
        return false;
    }
}
