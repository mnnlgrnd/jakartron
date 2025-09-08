package org.codegeny.jakartron.jpa;

import static jakarta.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.annotation.sql.DataSourceDefinition;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;

@Retention(RUNTIME)
@Inherited
@DataSourceDefinition(name = "mydb", className = "org.h2.jdbcx.JdbcDataSource", minPoolSize = 1, maxPoolSize = 2, url = "jdbc:h2:mem:mydb")
@PersistenceUnitDefinition(unitName = "tests", nonJtaDataSourceName = "mydb", transactionType = RESOURCE_LOCAL, managedClasses = President.class, properties = {
  @PersistenceUnitDefinition.Property(name = "jakarta.persistence.schema-generation.database.action", value = "create")
})
public @interface EnableTestPersistence {
}
