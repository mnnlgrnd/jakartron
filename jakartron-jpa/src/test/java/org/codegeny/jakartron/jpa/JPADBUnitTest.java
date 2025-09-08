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

import static jakarta.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL;

import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.codegeny.jakartron.dbunit.DBUnit;
import org.codegeny.jakartron.dbunit.DBUnitConnection;
import org.codegeny.jakartron.junit.ExtendWithJakartron;

@ExtendWithJakartron
@DataSourceDefinition(name = "mydb", className = "org.h2.jdbcx.JdbcDataSource", minPoolSize = 1, maxPoolSize = 2, url = "jdbc:h2:mem:mydb")
@DBUnitConnection(jndi = "mydb", properties = {
  @DBUnitConnection.Property(name = DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, value = "false"),
  @DBUnitConnection.Property(name = DatabaseConfig.PROPERTY_DATATYPE_FACTORY, value = "org.dbunit.ext.h2.H2DataTypeFactory"),
})
@PersistenceUnitDefinition(unitName = "tests", nonJtaDataSourceName = "mydb", transactionType = RESOURCE_LOCAL, managedClasses = President.class, properties = {
  @PersistenceUnitDefinition.Property(name = "jakarta.persistence.schema-generation.database.action", value = "create")
})
public class JPADBUnitTest {

    @PersistenceContext(unitName = "tests", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    @Test
    @DBUnit(initialDataSets = "presidents.xml")
    public void test() {
        entityManager.getTransaction().begin();
        Assertions.assertEquals(2, entityManager.createNamedQuery("countPresidents", Number.class).getSingleResult().intValue());
        entityManager.getTransaction().rollback();
    }
}
