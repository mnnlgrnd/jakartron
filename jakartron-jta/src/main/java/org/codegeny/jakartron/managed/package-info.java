/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * <p>
 * This package is a copy of <code>org.apache.commons:commons-dbcp2</code> package of the same name (managed).
 * <br>
 * Changes compared to original code are:
 * <ul>
 *     <li>JEE 10 compliant imports for <code>jakarta.transaction.*</code> instead of <code>javax.transaction.*</code></li>
 *     <li>{@link jakarta.annotation.sql.DataSourceDefinition DataSourceDefinition} as field of {@link org.codegeny.jakartron.managed.BasicManagedDataSource BasicManagedDataSource} to get password instead of using deprecated {@link org.apache.commons.dbcp2.BasicDataSource#getPassword}</li>
 * </ul>
 * <br>
 * TODO: should be removed as soon as a JEE 10 compliant version of commons-dbcp2 has been released
 */
package org.codegeny.jakartron.managed;
