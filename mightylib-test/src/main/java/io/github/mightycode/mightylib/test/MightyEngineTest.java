package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.main.EngineConfig;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

/**
 * JUnit 5: each test method that has a TestEngine parameter gets a started headless engine (MightyTest.headless),
 * built from a new configuration of the given supplier, closed after the test.
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(MightyEngineExtension.class)
public @interface MightyEngineTest {
    // The configuration of the game: a class with a constructor without parameter, called for each test
    Class<? extends Supplier<EngineConfig>> config();

    // Folders of resources above the ones of the game, relative to the working directory of the tests
    String[] sources() default {};
}
