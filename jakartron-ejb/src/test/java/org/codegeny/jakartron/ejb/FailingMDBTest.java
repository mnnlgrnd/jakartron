package org.codegeny.jakartron.ejb;

/*-
 * #%L
 * jakartron-ejb
 * %%
 * Copyright (C) 2018 - 2021 Codegeny
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import jakarta.annotation.Resource;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.MessageDrivenContext;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.codegeny.jakartron.junit.ExtendWithJakartron;

@ExtendWithJakartron
public class FailingMDBTest {

    private static final AtomicInteger RECEIVED = new AtomicInteger();
    private static final String QUEUE_NAME = "myQueue";

    @MessageDriven(activationConfig = {
	  @ActivationConfigProperty(propertyName = "destination", propertyValue = QUEUE_NAME),
	  @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "1")
    })
    public static class MyMDB implements MessageListener {

        @Resource
        private MessageDrivenContext context;

        @Override
        public void onMessage(Message message) {
            RECEIVED.incrementAndGet();
            context.setRollbackOnly();
        }
    }

    @Resource(lookup = QUEUE_NAME)
    private Queue queue;

    @Test
    public void testProxy(JMSContext context) {
        context.createProducer().send(queue, "ping");
        await().pollDelay(5, TimeUnit.SECONDS).untilAtomic(RECEIVED, equalTo(3)); // because redeliveryAttempts = 3
    }
}
