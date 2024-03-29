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

package org.springframework.core.io.buffer;

import io.netty.buffer.*;
import org.junit.Rule;
import org.junit.rules.Verifier;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.core.io.buffer.support.DataBufferTestUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for tests that read or write data buffers with a rule to check
 * that allocated buffers have been released.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public abstract class AbstractDataBufferAllocatingTestCase {

    @Rule
    public final Verifier leakDetector = new LeakDetector();
    @Parameterized.Parameter
    public DataBufferFactory bufferFactory;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] dataBufferFactories() {
        return new Object[][]{
                {new NettyDataBufferFactory(new UnpooledByteBufAllocator(true))},
                {new NettyDataBufferFactory(new UnpooledByteBufAllocator(false))},
                // disable caching for reliable leak detection, see https://github.com/netty/netty/issues/5275
                {new NettyDataBufferFactory(new PooledByteBufAllocator(true, 1, 1, 8192, 11, 0, 0, 0, true))},
                {new NettyDataBufferFactory(new PooledByteBufAllocator(false, 1, 1, 8192, 11, 0, 0, 0, true))},
                {new DefaultDataBufferFactory(true)},
                {new DefaultDataBufferFactory(false)}

        };
    }

    private static long getAllocations(List<PoolArenaMetric> metrics) {
        return metrics.stream().mapToLong(PoolArenaMetric::numActiveAllocations).sum();
    }

    protected DataBuffer createDataBuffer(int capacity) {
        return this.bufferFactory.allocateBuffer(capacity);
    }

    protected DataBuffer stringBuffer(String value) {
        return byteBuffer(value.getBytes(StandardCharsets.UTF_8));
    }

    protected Mono<DataBuffer> deferStringBuffer(String value) {
        return Mono.defer(() -> Mono.just(stringBuffer(value)));
    }

    protected DataBuffer byteBuffer(byte[] value) {
        DataBuffer buffer = this.bufferFactory.allocateBuffer(value.length);
        buffer.write(value);
        return buffer;
    }

    protected void release(DataBuffer... buffers) {
        Arrays.stream(buffers).forEach(DataBufferUtils::release);
    }

    protected Consumer<DataBuffer> stringConsumer(String expected) {
        return dataBuffer -> {
            String value =
                    DataBufferTestUtils.dumpString(dataBuffer, StandardCharsets.UTF_8);
            DataBufferUtils.release(dataBuffer);
            assertThat(value).isEqualTo(expected);
        };
    }

    /**
     * Wait until allocations are at 0, or the given duration elapses.
     */
    protected void waitForDataBufferRelease(Duration duration) throws InterruptedException {
        Instant start = Instant.now();
        while (true) {
            try {
                verifyAllocations();
                break;
            } catch (AssertionError ex) {
                if (Instant.now().isAfter(start.plus(duration))) {
                    throw ex;
                }
            }
            Thread.sleep(50);
        }
    }

    private void verifyAllocations() {
        if (this.bufferFactory instanceof NettyDataBufferFactory) {
            ByteBufAllocator allocator = ((NettyDataBufferFactory) this.bufferFactory).getByteBufAllocator();
            if (allocator instanceof PooledByteBufAllocator) {
                Instant start = Instant.now();
                while (true) {
                    PooledByteBufAllocatorMetric metric = ((PooledByteBufAllocator) allocator).metric();
                    long total = getAllocations(metric.directArenas()) + getAllocations(metric.heapArenas());
                    if (total == 0) {
                        return;
                    }
                    if (Instant.now().isBefore(start.plus(Duration.ofSeconds(5)))) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ex) {
                            // ignore
                        }
                        continue;
                    }
                    assertThat(total).as("ByteBuf Leak: " + total + " unreleased allocations").isEqualTo(0);
                }
            }
        }
    }

    protected class LeakDetector extends Verifier {

        @Override
        public void verify() {
            AbstractDataBufferAllocatingTestCase.this.verifyAllocations();
        }
    }

}
