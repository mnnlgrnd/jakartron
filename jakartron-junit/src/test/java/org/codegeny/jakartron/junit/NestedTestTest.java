package org.codegeny.jakartron.junit;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.codegeny.jakartron.DisableDiscovery;

@ExtendWithJakartron
@DisableDiscovery
public class NestedTestTest {

    @Test
    public void simpleTest() {
    }

    @Nested
    public class NestedTest {

        @Test
        public void nested() {
        }
    }
}
