package org.codegeny.jakartron.junit;

/*-
 * #%L
 * jakartron-junit
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

import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstanceFactory;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;
import org.junit.platform.commons.util.ReflectionUtils;

import org.codegeny.jakartron.Jakartron;

public final class JakartronExtension implements TestInstanceFactory, BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver, TestInstancePreDestroyCallback {

    private static final Namespace NAMESPACE = Namespace.create(JakartronExtension.class);
    private static final Logger LOGGER = Logger.getLogger(JakartronExtension.class.getName());

    @Override
    public void afterAll(ExtensionContext context) {
		getBeanManager(context).ifPresent(beanManager -> beanManager.getEvent().select(TestEvent.Literal.of(TestPhase.AFTER_ALL)).fire(context));
        getContainer(context).ifPresent(SeContainer::close);
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        try {
			SeContainer container =
			  Jakartron.initialize(Stream.concat(Stream.of(extensionContext.getRequiredTestClass()), ReflectionUtils.findNestedClasses(extensionContext.getRequiredTestClass(), t -> true).stream()))
				.addExtensions(new TestExtension(extensionContext.getRequiredTestClass()))
				.addBeanClasses(hierarchy(extensionContext.getRequiredTestClass()))
				.initialize();
            getStore(extensionContext).put(SeContainer.class, container);
            getStore(extensionContext).put(AnnotatedType.class, container.getBeanManager().createAnnotatedType(extensionContext.getRequiredTestClass()));
			container.getBeanManager().getEvent().select(TestEvent.Literal.of(TestPhase.BEFORE_ALL)).fire(extensionContext);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, exception.getMessage(), exception);
            throw exception;
        }
    }

    private static Class<?>[] hierarchy(Class<?> testClass) {
        List<Class<?>> hierarchy = new ArrayList<>();
        while (testClass != null) {
            hierarchy.add(testClass);
            testClass = testClass.getEnclosingClass();
        }
        return hierarchy.toArray(new Class<?>[0]);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        getBeanManager(extensionContext)
		  .ifPresent(beanManager -> {
			  beanManager.getEvent().select(TestEvent.Literal.of(TestPhase.AFTER_EACH)).fire(extensionContext);
			  getStore(extensionContext).get(RequestContextController.class, RequestContextController.class).deactivate();
		  });
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        getBeanManager(extensionContext).ifPresent(beanManager -> {
            RequestContextController requestContextController = beanManager.createInstance().select(RequestContextController.class).get();
            getStore(extensionContext).put(RequestContextController.class, requestContextController);
            requestContextController.activate();
            AnnotatedType<?> annotatedType = getStore(extensionContext).get(AnnotatedType.class, AnnotatedType.class);
            annotatedType.getMethods().stream()
			  .filter(m -> m.getJavaMember().equals(extensionContext.getRequiredTestMethod()))
			  .findFirst()
			  .ifPresent(m -> getStore(extensionContext).put(AnnotatedMethod.class, m));
			beanManager.getEvent().select(TestEvent.Literal.of(TestPhase.BEFORE_EACH)).fire(extensionContext);
        });
    }

    @Override
    public Object createTestInstance(TestInstanceFactoryContext testInstanceFactoryContext, ExtensionContext extensionContext) {
        return getBeanManager(extensionContext).map(beanManager -> {
            CreationalContext<?> creationalContext = getStore(extensionContext).get(CreationalContext.class, CreationalContext.class);
            if (creationalContext == null) {
                creationalContext = beanManager.createCreationalContext(null);
                getStore(extensionContext).put(CreationalContext.class, creationalContext);
            }
            Bean<?> testBean = beanManager.resolve(beanManager.getBeans(extensionContext.getRequiredTestClass()));
            return beanManager.getReference(testBean, extensionContext.getRequiredTestClass(), creationalContext);
        }).orElse(null);
    }

    @Override
    public void preDestroyTestInstance(ExtensionContext extensionContext) {
        getBeanManager(extensionContext).ifPresent(beanManager -> beanManager.getExtension(TestExtension.class).reset());
        getStore(extensionContext).get(CreationalContext.class, CreationalContext.class).release();
    }

    public static Optional<BeanManager> getBeanManager(ExtensionContext extensionContext) {
        return getContainer(extensionContext).map(SeContainer::getBeanManager);
    }

    public static Optional<SeContainer> getContainer(ExtensionContext extensionContext) {
        return Optional.ofNullable(getStore(extensionContext).get(SeContainer.class, SeContainer.class));
    }

    public static ExtensionContext.Store getStore(ExtensionContext extensionContext) {
        return extensionContext.getStore(NAMESPACE);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return getBeanManager(extensionContext).map(beanManager -> {
            InjectionPoint injectionPoint = injectionPoint(parameterContext, extensionContext, beanManager);
            return beanManager.resolve(beanManager.getBeans(injectionPoint.getType(), injectionPoint.getQualifiers().toArray(new Annotation[0]))) != null;
        }).orElse(false);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return getBeanManager(extensionContext).map(beanManager -> {
            InjectionPoint injectionPoint = injectionPoint(parameterContext, extensionContext, beanManager);
            return beanManager.getInjectableReference(injectionPoint, getStore(extensionContext).get(CreationalContext.class, CreationalContext.class));
        }).orElse(null);
    }

    private static InjectionPoint injectionPoint(ParameterContext parameterContext, ExtensionContext extensionContext, BeanManager beanManager) {
        AnnotatedMethod<?> annotatedMethod = getStore(extensionContext).get(AnnotatedMethod.class, AnnotatedMethod.class);
        AnnotatedParameter<?> annotatedParameter = annotatedMethod.getParameters().get(parameterContext.getIndex());
        return beanManager.createInjectionPoint(annotatedParameter);
    }
}
