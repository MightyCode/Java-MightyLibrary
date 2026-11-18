package io.github.mightycode.mightylib.test;

import io.github.mightycode.mightylib.main.EngineConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

/**
 * JUnit 5, tests with GL (tag "gl"): each test method with a SceneRunner parameter gets the scenes of an engine with a
 * hidden window (MightyTest.withWindow), kept between the tests of the same configuration. Before each test the engine
 * is on the empty scene; after it, back to the empty scene, the GL objects, shared elements, referenced resources and
 * sound sources must be the ones before (the test fails naming them). Without screen or OpenGL 3.3 core, the test is
 * skipped with the reason.
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Tag("gl")
@ExtendWith(MightySceneExtension.class)
public @interface MightySceneTest {
    // The configuration of the game: a class with a constructor without parameter
    Class<? extends Supplier<EngineConfig>> config();

    // Folders of resources above the ones of the game, relative to the working directory of the tests
    String[] sources() default {};

    // False: no leak check after each test
    boolean checkLeaks() default true;
}
