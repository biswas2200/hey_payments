package com.heypayments;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTests {

    private final ApplicationModules modules = ApplicationModules.of(BackendApplication.class);

    @Test
    @DisplayName("should satisfy Spring Modulith architectural boundaries")
    void verifyModularity() {
        modules.verify();
    }
}
