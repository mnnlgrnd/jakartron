package org.codegeny.jakartron.jdbc;

/*-
 * #%L
 * jakartron-jta
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

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.annotation.sql.DataSourceDefinitions;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.inject.Singleton;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DataSourceConnectionFactory;
import org.kohsuke.MetaInfServices;

import org.codegeny.jakartron.Annotations;
import org.codegeny.jakartron.jndi.JNDI;
import org.codegeny.jakartron.managed.BasicManagedDataSource;

@MetaInfServices
public final class XADataSourceIntegration implements Extension {

    private static final Logger LOGGER = Logger.getLogger(XADataSourceIntegration.class.getName());

    private final Set<DataSourceDefinition> dataSources = new HashSet<>();

    public void process(@Observes @WithAnnotations({DataSourceDefinition.class, DataSourceDefinitions.class}) ProcessAnnotatedType<?> event) {
        Annotations.findAnnotations(event.getAnnotatedType().getJavaClass(), DataSourceDefinition.class, Annotations.Expander.SUPER_CLASS, Annotations.Expander.META_ANNOTATIONS)
          .forEach(dataSources::add);
    }

    public void makeDataSourceInjectable(@Observes AfterBeanDiscovery event) {
        dataSources.forEach(dataSource -> event.<BasicDataSource>addBean()
          .types(Object.class, DataSource.class, XADataSource.class)
          .produceWith(instance -> createBasicDataSource(dataSource, instance))
          .destroyWith((instance, context) -> closeDataSource(instance))
          .qualifiers(JNDI.Literal.of(dataSource.name()))
          .scope(Singleton.class)
        );
    }

    private void closeDataSource(BasicDataSource dataSource) {
        try {
            dataSource.close();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, exception, () -> "Error while closing pool");
        }
    }

    private static <T> void addIfNotEmpty(Map<String, Object> map, String key, T value, T empty) {
        if (!Objects.equals(value, empty)) {
            map.put(key, value);
        }
    }

    private BasicDataSource createBasicDataSource(DataSourceDefinition definition, Instance<Object> instance) {

        Map<String, Object> map = new HashMap<>();
        addIfNotEmpty(map, "url", definition.url(), "");
        addIfNotEmpty(map, "name", definition.name(), "");
        addIfNotEmpty(map, "password", definition.password(), "");
        addIfNotEmpty(map, "databaseName", definition.databaseName(), "");
        addIfNotEmpty(map, "serverName", definition.serverName(), "");
        addIfNotEmpty(map, "portNumber", definition.portNumber(), -1);
        addIfNotEmpty(map, "loginTimeout", definition.loginTimeout(), 0);

        for (String property : definition.properties()) {
            int index = property.indexOf('=');
            if (index != -1) {
                map.put(property.substring(0, index), property.substring(index + 1));
            }
        }

        try {
            Class<?> klass = Thread.currentThread().getContextClassLoader().loadClass(definition.className());
            BasicDataSource pool;
            if (definition.transactional()) {
                if (!XADataSource.class.isAssignableFrom(klass)) {
                    throw new CreationException("DataSource is transactional but does not implement XADataSource");
                }
                XADataSource xaDataSource = klass.asSubclass(XADataSource.class).getDeclaredConstructor().newInstance();
                BeanUtils.copyProperties(xaDataSource, map);
                BasicManagedDataSource managedPool = new BasicManagedDataSource(definition);
                managedPool.setXaDataSourceInstance(xaDataSource);
                managedPool.setTransactionManager(instance.select(TransactionManager.class).get());
                managedPool.setTransactionSynchronizationRegistry(instance.select(TransactionSynchronizationRegistry.class).get());
                managedPool.setMinIdle(definition.minPoolSize());
                managedPool.setMaxIdle(definition.maxPoolSize());
                managedPool.setInitialSize(definition.initialPoolSize());
                pool = managedPool;
            } else {
                if (!DataSource.class.isAssignableFrom(klass)) {
                    throw new CreationException("The provided class does not implement DataSource");
                }
                DataSource dataSource = klass.asSubclass(DataSource.class).getDeclaredConstructor().newInstance();
                BeanUtils.copyProperties(dataSource, map);
                pool = new SimpleBasicDataSource(dataSource);
            }
            pool.setMinIdle(definition.minPoolSize());
            pool.setMaxIdle(definition.maxPoolSize());
            pool.setInitialSize(definition.initialPoolSize());
            return pool;
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new CreationException("Can't create a data source for " + definition, exception);
        }
    }

    private static final class SimpleBasicDataSource extends BasicDataSource {

        private final DataSource dataSource;

        SimpleBasicDataSource(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        protected ConnectionFactory createConnectionFactory() {
            return new DataSourceConnectionFactory(dataSource);
        }
    }
}
