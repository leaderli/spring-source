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

package org.springframework.web.socket.handler;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Unit tests for WebSocketHttpHeaders.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketHttpHeadersTests {

    private WebSocketHttpHeaders headers;

    @Before
    public void setUp() {
        headers = new WebSocketHttpHeaders();
    }

    @Test
    public void parseWebSocketExtensions() {
        List<String> extensions = new ArrayList<>();
        extensions.add("x-foo-extension, x-bar-extension");
        extensions.add("x-test-extension");
        this.headers.put(WebSocketHttpHeaders.SEC_WEBSOCKET_EXTENSIONS, extensions);

        List<WebSocketExtension> parsedExtensions = this.headers.getSecWebSocketExtensions();
        assertThat(parsedExtensions).hasSize(3);
    }

}
