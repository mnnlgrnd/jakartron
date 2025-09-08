package org.codegeny.jakartron.jsf;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.omnifaces.cdi.InjectionTargetWrapper;
import org.omnifaces.cdi.eager.EagerExtension;
import org.omnifaces.cdi.param.ParamExtension;
import org.omnifaces.cdi.viewscope.ViewScopeExtension;

import org.codegeny.jakartron.AdditionalClasses;
import org.codegeny.jakartron.AdditionalPackages;

@Retention(RetentionPolicy.RUNTIME)
@AdditionalClasses({ViewScopeExtension.class, EagerExtension.class, ParamExtension.class})
@AdditionalPackages(value = InjectionTargetWrapper.class, recursive = true)
public @interface EnableOmnifaces {
}
