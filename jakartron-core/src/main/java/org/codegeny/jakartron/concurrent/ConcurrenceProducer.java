package org.codegeny.jakartron.concurrent;

/*-
 * #%L
 * jakartron-core
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

import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManageableThread;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import jakarta.enterprise.concurrent.ManagedThreadFactory;
import jakarta.enterprise.concurrent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.codegeny.jakartron.Internal;
import org.codegeny.jakartron.jndi.JNDI;

@Dependent
public class ConcurrenceProducer {

    public static final String MANAGED_EXECUTOR_SERVICE_JNDI_NAME = "java:comp/concurrent/ThreadPool";
    public static final String MANAGED_THREAD_FACTORY_JNDI_NAME = "java:comp/concurrent/ThreadFactory";

    @Inject
    @Internal
    private Logger logger;

    @Produces
    @JNDI(MANAGED_THREAD_FACTORY_JNDI_NAME)
    @ApplicationScoped
    public ManagedThreadFactory createManagedThreadFactory() {
        // FIXME
        return new ManagedThreadFactory() {
            @Override
            public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
                return new ForkJoinWorkerThread(pool) {

                };
            }

            @Override
            public Thread newThread(Runnable r) {
                return new ManageableThreadImpl(r);
            }
        };
    }

    @Produces
    @JNDI(MANAGED_EXECUTOR_SERVICE_JNDI_NAME)
    @ApplicationScoped
    public ManagedScheduledExecutorService createManagedScheduledExecutorService(@JNDI(MANAGED_THREAD_FACTORY_JNDI_NAME) ManagedThreadFactory managedThreadFactory) {
        return new ManagedScheduledExecutorServiceImpl(managedThreadFactory);
    }

    public void destroyManagedExecutorService(@Disposes @JNDI(MANAGED_EXECUTOR_SERVICE_JNDI_NAME) ManagedExecutorService managedExecutorService) throws InterruptedException {
        managedExecutorService.shutdown();
        if (!managedExecutorService.awaitTermination(10, TimeUnit.SECONDS)) {
            logger.warning("Could not shut down ManagedExecutorService properly");
        }
    }

    private static final class ManagedScheduledExecutorServiceImpl extends ScheduledThreadPoolExecutor implements ManagedScheduledExecutorService {

        ManagedScheduledExecutorServiceImpl(ThreadFactory threadFactory) {
            super(5, threadFactory);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, Trigger trigger) {
            return schedule(() -> {
                command.run();
                return null;
            }, trigger);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, Trigger trigger) {
            throw new UnsupportedOperationException();
        }

        // FIXME
        @Override
        public <U> CompletableFuture<U> completedFuture(U u) {
            return null;
        }

        @Override
        public <U> CompletionStage<U> completedStage(U u) {
            return null;
        }

        @Override
        public <T> CompletableFuture<T> copy(CompletableFuture<T> completableFuture) {
            return null;
        }

        @Override
        public <T> CompletionStage<T> copy(CompletionStage<T> completionStage) {
            return null;
        }

        @Override
        public <U> CompletableFuture<U> failedFuture(Throwable throwable) {
            return null;
        }

        @Override
        public <U> CompletionStage<U> failedStage(Throwable throwable) {
            return null;
        }

        @Override
        public ContextService getContextService() {
            return null;
        }

        @Override
        public <U> CompletableFuture<U> newIncompleteFuture() {
            return null;
        }

        @Override
        public CompletableFuture<Void> runAsync(Runnable runnable) {
            return null;
        }

        @Override
        public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
            return null;
        }
    }

    private static final class ManageableThreadImpl extends Thread implements ManageableThread {

        ManageableThreadImpl(Runnable target) {
            super(target);
        }

        @Override
        public boolean isShutdown() {
            return false;
        }
    }
}
