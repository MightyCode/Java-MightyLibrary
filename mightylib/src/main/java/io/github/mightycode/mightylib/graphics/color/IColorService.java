package io.github.mightycode.mightylib.graphics.color;

import io.github.mightycode.mightylib.services.IEngineService;
import org.apiguardian.api.API;

import static org.apiguardian.api.API.Status.EXPERIMENTAL;

/**
 * The colors of the engine (service mightylib:colors, Engine.colors): the color space of the draws by default.
 * A view (IView.colorSpace) and a renderer (Renderer.setColorSpace) override it. ColorService by default,
 * replaceable by registry.services().replace. Without GL.
 */
@API(status = EXPERIMENTAL)
public interface IColorService extends IEngineService {
    // LINEAR or GAMMA, never INHERIT; read at each draw
    ERenderColorSpace colorSpace();

    /**
     * Changes the default of the draws; takes effect at the next draw, nothing is reloaded. Main thread.
     *
     * @throws IllegalArgumentException if the mode is INHERIT or null
     */
    void setColorSpace(ERenderColorSpace colorSpace);

    // The mode of a draw from the levels below the engine (null or INHERIT: the level above)
    default ERenderColorSpace resolve(ERenderColorSpace renderer, ERenderColorSpace view) {
        return ERenderColorSpace.Resolve(renderer, view, colorSpace());
    }
}
