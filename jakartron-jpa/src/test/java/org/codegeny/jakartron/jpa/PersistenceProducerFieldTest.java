package org.codegeny.jakartron.jpa;

/*-
 * #%L
 * jakartron-jpa
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
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.codegeny.jakartron.junit.ExtendWithJakartron;

@ExtendWithJakartron
@DataSourceDefinition(name = "mydb", className = "org.h2.jdbcx.JdbcDataSource", url = "jdbc:h2:mem:mydb")
@PersistenceUnitDefinition(unitName = "tests", nonJtaDataSourceName = "mydb", managedClasses = President.class, properties = {
  @PersistenceUnitDefinition.Property(name = "jakarta.persistence.schema-generation.database.action", value = "create")
})
public class PersistenceProducerFieldTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    public @interface MyDB {
    }

    public static class MyDBContainerProducer {

        @PersistenceContext(unitName = "tests")
        @Produces
        @MyDB
        EntityManager entityManager;
    }

    @Inject
    @MyDB
    private EntityManager entityManager;

    @Test
    @Transactional
    public void test() {
        Assertions.assertNull(entityManager.find(President.class, 1L));
        entityManager.persist(new President("G. Washington"));
        entityManager.flush();
        entityManager.clear();
        Assertions.assertNotNull(entityManager.find(President.class, 1L));
    }
}
