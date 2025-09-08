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

import jakarta.ejb.Lock;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.util.Nonbinding;

import org.kohsuke.MetaInfServices;

@MetaInfServices
public final class LockExtension implements Extension {

    public void configure(@Observes BeforeBeanDiscovery event) {
        event.addAnnotatedType(LockInterceptor.class, LockInterceptor.class.getName());
        event.configureInterceptorBinding(Lock.class).methods().forEach(m -> m.add(Nonbinding.Literal.INSTANCE));
    }
}
