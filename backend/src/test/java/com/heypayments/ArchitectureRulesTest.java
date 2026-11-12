package com.heypayments;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * ArchUnit structural and architectural rule enforcement. Enforces domain purity, forbidden field
 * injection, and layer isolation per AGENTS.md §5.
 */
@AnalyzeClasses(
    packages = "com.heypayments",
    importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureRulesTest {

  @ArchTest
  static final ArchRule noFieldInjection =
      noFields()
          .should()
          .beAnnotatedWith(Autowired.class)
          .as("Field injection with @Autowired is strictly prohibited; use constructor injection");

  @ArchTest
  static final ArchRule domainMustNotDependOnSpring =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework..")
          .as("Domain layer must be pure and not depend on Spring framework");

  @ArchTest
  static final ArchRule domainMustNotDependOnPersistence =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("jakarta.persistence..")
          .as("Domain layer must not depend on JPA / persistence annotations");

  @ArchTest
  static final ArchRule domainMustNotDependOnWebOrInfra =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..web..", "..infra..")
          .as("Domain layer must not depend on web or infra layers");

  @ArchTest
  static final ArchRule controllerMethodsMustBeSecured =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .areAnnotatedWith(RestController.class)
          .and()
          .arePublic()
          .should()
          .beAnnotatedWith(PreAuthorize.class)
          .as(
              "Every public endpoint method in a @RestController must be explicitly secured with"
                  + " @PreAuthorize");
}
