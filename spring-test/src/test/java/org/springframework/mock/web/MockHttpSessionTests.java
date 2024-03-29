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

package org.springframework.mock.web;

import org.junit.Test;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link MockHttpSession}.
 *
 * @author Sam Brannen
 * @author Vedran Pavic
 * @since 3.2
 */
public class MockHttpSessionTests {

    private MockHttpSession session = new MockHttpSession();


    @Test
    public void invalidateOnce() {
        assertThat(session.isInvalid()).isFalse();
        session.invalidate();
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    public void invalidateTwice() {
        session.invalidate();
        assertThatIllegalStateException().isThrownBy(
                session::invalidate);
    }

    @Test
    public void getCreationTimeOnInvalidatedSession() {
        session.invalidate();
        assertThatIllegalStateException().isThrownBy(
                session::getCreationTime);
    }

    @Test
    public void getLastAccessedTimeOnInvalidatedSession() {
        session.invalidate();
        assertThatIllegalStateException().isThrownBy(
                session::getLastAccessedTime);
    }

    @Test
    public void getAttributeOnInvalidatedSession() {
        session.invalidate();
        assertThatIllegalStateException().isThrownBy(() ->
                session.getAttribute("foo"));
    }

    @Test
    public void getAttributeNamesOnInvalidatedSession() {
        session.invalidate();
        assertThatIllegalStateException().isThrownBy(
                session::getAttributeNames);
    }

    @Test
    public void getValueOnInvalidatedSession() {
        session.invalidate();
        assertThatIllegalStateException().isThrownBy(() ->
                session.getValue("foo"));
    }

    @Test
    public void getValueNamesOnInvalidatedSession() {
        session.invalidate();
        assertThatIllegalStateException().isThrownBy(
                session::getValueNames);
    }

    @Test
    public void setAttributeOnInvalidatedSession() {
        session.invalidate();
        assertThatIllegalStateException().isThrownBy(() ->
                session.setAttribute("name", "value"));
    }

    @Test
    public void putValueOnInvalidatedSession() {
        session.invalidate();
        assertThatIllegalStateException().isThrownBy(() ->
                session.putValue("name", "value"));
    }

    @Test
    public void removeAttributeOnInvalidatedSession() {
        session.invalidate();
        assertThatIllegalStateException().isThrownBy(() ->
                session.removeAttribute("name"));
    }

    @Test
    public void removeValueOnInvalidatedSession() {
        session.invalidate();
        assertThatIllegalStateException().isThrownBy(() ->
                session.removeValue("name"));
    }

    @Test
    public void isNewOnInvalidatedSession() {
        session.invalidate();
        assertThatIllegalStateException().isThrownBy(
                session::isNew);
    }

    @Test
    public void bindingListenerBindListener() {
        String bindingListenerName = "bindingListener";
        CountingHttpSessionBindingListener bindingListener = new CountingHttpSessionBindingListener();

        session.setAttribute(bindingListenerName, bindingListener);

        assertThat(1).isEqualTo(bindingListener.getCounter());
    }

    @Test
    public void bindingListenerBindListenerThenUnbind() {
        String bindingListenerName = "bindingListener";
        CountingHttpSessionBindingListener bindingListener = new CountingHttpSessionBindingListener();

        session.setAttribute(bindingListenerName, bindingListener);
        session.removeAttribute(bindingListenerName);

        assertThat(0).isEqualTo(bindingListener.getCounter());
    }

    @Test
    public void bindingListenerBindSameListenerTwice() {
        String bindingListenerName = "bindingListener";
        CountingHttpSessionBindingListener bindingListener = new CountingHttpSessionBindingListener();

        session.setAttribute(bindingListenerName, bindingListener);
        session.setAttribute(bindingListenerName, bindingListener);

        assertThat(1).isEqualTo(bindingListener.getCounter());
    }

    @Test
    public void bindingListenerBindListenerOverwrite() {
        String bindingListenerName = "bindingListener";
        CountingHttpSessionBindingListener bindingListener1 = new CountingHttpSessionBindingListener();
        CountingHttpSessionBindingListener bindingListener2 = new CountingHttpSessionBindingListener();

        session.setAttribute(bindingListenerName, bindingListener1);
        session.setAttribute(bindingListenerName, bindingListener2);

        assertThat(0).isEqualTo(bindingListener1.getCounter());
        assertThat(1).isEqualTo(bindingListener2.getCounter());
    }

    private static class CountingHttpSessionBindingListener
            implements HttpSessionBindingListener {

        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public void valueBound(HttpSessionBindingEvent event) {
            this.counter.incrementAndGet();
        }

        @Override
        public void valueUnbound(HttpSessionBindingEvent event) {
            this.counter.decrementAndGet();
        }

        int getCounter() {
            return this.counter.get();
        }

    }

}
