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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.codegeny.jakartron.junit.ExtendWithJakartron;

@ExtendWithJakartron
@EnableTestPersistence
public class JPADBWithPersistenceUnitDefinitionOnAnnotationTest {

    @PersistenceContext(unitName = "tests", type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    @Test
    public void test() {
        entityManager.getTransaction().begin();
        entityManager.persist(new President("G. Washington"));
        entityManager.persist(new President("A. Lincoln"));
        entityManager.getTransaction().commit();

        entityManager.clear();

        entityManager.getTransaction().begin();
        Assertions.assertEquals(2, entityManager.createNamedQuery("countPresidents", Number.class).getSingleResult().intValue());
        entityManager.getTransaction().rollback();
    }
}
