package org.codegeny.jakartron;

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

import jakarta.annotation.Resource;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.codegeny.jakartron.junit.ExtendWithJakartron;

@ConfigureMyDataSource
@ExtendWithJakartron
public class JDBCTest {

    @Resource(lookup = "java:/jdbc/mydb")
    private DataSource dataSource;

    @Inject
    private TransactionManager transactionManager;

	@BeforeEach
	public void setUp() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("create table test ( name varchar2(50) )")) {
                statement.execute();
            }
            try (PreparedStatement statement = connection.prepareStatement("select count(*) from test")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    Assertions.assertTrue(resultSet.next());
                    Assertions.assertEquals(0, resultSet.getInt(1));
                }
            }
        }
	}

	@AfterEach
	public void tearDown() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement("drop table test if exists")) {
				statement.execute();
			}
		}
	}

	@Test
	public void test() throws Exception {
        transactionManager.begin();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("insert into test values ('hello')")) {
                statement.execute();
            }
            try (PreparedStatement statement = connection.prepareStatement("select count(*) from test")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    Assertions.assertTrue(resultSet.next());
                    Assertions.assertEquals(1, resultSet.getInt(1));
                }
            }
        } finally {
            transactionManager.rollback();
        }

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("select count(*) from test")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    Assertions.assertTrue(resultSet.next());
                    Assertions.assertEquals(0, resultSet.getInt(1));
                }
            }
        }
    }

	@Test
	public void testCommit() throws Exception {

		countRecords(0);
		insertRecord();
		countRecords(1);
	}

	@Transactional
	public void insertRecord() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement("insert into test values ('hello')")) {
				statement.execute();
			}
		}
	}

	@Transactional
	public void countRecords(int expected) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement statement = connection.prepareStatement("select count(*) from test")) {
				try (ResultSet resultSet = statement.executeQuery()) {
					Assertions.assertTrue(resultSet.next());
					Assertions.assertEquals(expected, resultSet.getInt(1));
				}
			}
		}
	}
}
