/**
 * The test kit of the games: MightyTest starts an engine as the game does (same plugins), without GL (headless) or in a
 * hidden window, driven frame by frame with a manual time. JUnit 5: @MightyEngineTest and @MightySceneTest.
 * Classpath only, one engine at a time per JVM (the engine has a process-wide state, reset when it closes).
 */
@API(status = EXPERIMENTAL)
package io.github.mightycode.mightylib.test;

import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;
