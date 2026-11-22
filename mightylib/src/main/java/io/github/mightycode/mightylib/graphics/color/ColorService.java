package io.github.mightycode.mightylib.graphics.color;

import io.github.mightycode.mightylib.main.MainThread;
import io.github.mightycode.mightylib.resources.EngineResources;
import io.github.mightycode.mightylib.resources.Resources;
import io.github.mightycode.mightylib.resources.config.ConfigData;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

/**
 * The color service of the engine: the default color space of the draws, given at the start
 * (EngineConfig.setColorSpace, -Dmightylib.colorSpace), the final pass of the engine (EngineResources.POST_PROCESSING)
 * and its settings, read from the configuration DISPLAY_CONFIG (defaults of the sources, then the player's layer) and
 * written to the player's layer by setPicture. A change of the file (watched) is applied at the next update. No GL.
 */
public final class ColorService implements IColorService {
    // Reference of the service on its configuration
    private static final String OWNER = "service:colors";

    // Null: no configuration (the settings stay in memory)
    private final Resources resources;
    private ConfigData config;
    private int appliedVersion = -1;

    // Written on the main thread, read when the views draw
    private volatile ERenderColorSpace colorSpace;
    private volatile String finalPassShader = EngineResources.POST_PROCESSING;
    private volatile PictureSettings picture = PictureSettings.DEFAULT;

    // Without configuration: the settings of the picture are not read nor saved
    public ColorService(ERenderColorSpace colorSpace) {
        this(colorSpace, null);
    }

    public ColorService(ERenderColorSpace colorSpace, Resources resources) {
        this.colorSpace = ERenderColorSpace.CheckEngine(colorSpace);
        this.resources = resources;
    }

    // The configuration is taken once the data of the resources exist, read again when its version changes
    @Override
    public void update() {
        if (config == null && resources != null && resources.isInitialized())
            config = resources.addOwnedResource(ConfigData.class, new ConfigData(DISPLAY_CONFIG), OWNER);

        if (config != null && config.isLoaded() && config.getVersion() != appliedVersion) {
            appliedVersion = config.getVersion();
            picture = Read(config, false);
        }
    }

    // The settings of the configuration; a value out of bounds is ignored with a line (1 instead)
    static PictureSettings Read(ConfigData config, boolean defaultsOnly) {
        JSONObject values = defaultsOnly ? config.defaultValues() : config.values();
        return PictureSettings.of(Value(config, values, GAMMA_KEY), Value(config, values, BRIGHTNESS_KEY),
                Value(config, values, CONTRAST_KEY));
    }

    private static float Value(ConfigData config, JSONObject values, String key) {
        float value = values.optFloat(key, 1f);
        if (PictureSettings.IsValid(value))
            return value;

        System.err.println("--Config " + config.getDataName() + ": " + key + " " + value + " out of ["
                + PictureSettings.MIN + ", " + PictureSettings.MAX + "], 1 is used");
        return 1f;
    }

    @Override
    public ERenderColorSpace colorSpace() {
        return colorSpace;
    }

    @Override
    public void setColorSpace(ERenderColorSpace colorSpace) {
        ERenderColorSpace checked = ERenderColorSpace.CheckEngine(colorSpace);
        checkMainThread();
        this.colorSpace = checked;
    }

    @Override
    public String finalPassShader() {
        return finalPassShader;
    }

    @Override
    public void setFinalPassShader(String shaderName) {
        if (shaderName == null || shaderName.isEmpty())
            throw new IllegalArgumentException("The final pass needs the name of a shader");
        checkMainThread();
        finalPassShader = shaderName;
    }

    @Override
    public PictureSettings picture() {
        return picture;
    }

    // Only the values that differ from the settings of the game go to the player's layer
    @Override
    public void setPicture(PictureSettings picture) {
        if (picture == null)
            throw new IllegalArgumentException("The settings of the picture can't be null");
        checkMainThread();
        this.picture = picture;

        if (config == null || !config.isLoaded())
            return;

        PictureSettings game = Read(config, true);
        save(GAMMA_KEY, picture.gamma(), game.gamma());
        save(BRIGHTNESS_KEY, picture.brightness(), game.brightness());
        save(CONTRAST_KEY, picture.contrast(), game.contrast());
    }

    private void save(String key, float value, float gameValue) {
        if (value == gameValue) {
            remove(key);
        } else if (config.getFloat(key, Float.NaN) != value) {
            set(key, value);
        }
    }

    // The writes log their own failure: nothing waits for them
    private void set(String key, float value) {
        CompletableFuture<Void> ignored = config.set(key, value);
    }

    private void remove(String key) {
        CompletableFuture<Void> ignored = config.remove(key);
    }

    @Override
    public void resetPicture() {
        checkMainThread();
        if (config == null || !config.isLoaded()) {
            picture = PictureSettings.DEFAULT;
            return;
        }

        remove(GAMMA_KEY);
        remove(BRIGHTNESS_KEY);
        remove(CONTRAST_KEY);
        picture = Read(config, false);
    }

    // The configuration, null before the data of the resources exist or without resources
    public ConfigData config() {
        return config;
    }

    @Override
    public void unload() {
        if (config != null) {
            resources.releaseOwnedResource(config, OWNER);
            config = null;
        }
    }

    private static void checkMainThread() {
        if (!MainThread.isMainThread())
            throw new IllegalStateException("The colors of the engine change on the main thread");
    }
}
