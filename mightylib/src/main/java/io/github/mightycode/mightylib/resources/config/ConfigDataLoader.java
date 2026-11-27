package io.github.mightycode.mightylib.resources.config;

import io.github.mightycode.mightylib.resources.DataType;
import io.github.mightycode.mightylib.resources.FileMethods;
import io.github.mightycode.mightylib.resources.ResourceException;
import io.github.mightycode.mightylib.resources.ResourceLoader;
import io.github.mightycode.mightylib.resources.source.IResourceSource;
import io.github.mightycode.mightylib.resources.source.ResourceSources;
import io.github.mightycode.mightylib.resources.source.SourceEntry;
import io.github.mightycode.mightylib.utils.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The configurations: every configs/**.json of the sources, named after their path in the folder without the
 * extension ("configs/mightylib/display.json" is "mightylib/display"). Each source that has the file is a layer.
 */
public final class ConfigDataLoader extends ResourceLoader {
    public static final String FOLDER = "configs";
    private static final String EXTENSION = ".json";

    @Override
    public Class<? extends DataType> getType() {
        return ConfigData.class;
    }

    @Override
    public String getResourceNameType() {
        return "ConfigData";
    }

    @Override
    public void create(Map<String, DataType> data) {
        explore(data, FOLDER);
    }

    private void explore(Map<String, DataType> data, String folder) {
        for (SourceEntry entry : resources().sources().list(folder)) {
            if (entry.isFolder()) {
                explore(data, entry.path());
                continue;
            }

            String path = entry.path();
            if (!path.endsWith(EXTENSION))
                continue;

            String name = path.substring(FOLDER.length() + 1, path.length() - EXTENSION.length());
            data.putIfAbsent(name, new ConfigData(name));
        }
    }

    @Override
    public void fileDetected(Map<String, DataType> data, String currentPath, String name) {}

    @Override
    public String filterFile(String path) {
        return null;
    }

    @Override
    public void initWithFile(DataType dataType) {
        if (!(dataType instanceof ConfigData))
            return;

        ConfigData config = (ConfigData) dataType;
        ResourceSources sources = resources().sources();
        String path = config.path();

        JSONObject defaults = new JSONObject();
        JSONObject player = new JSONObject();
        List<String> layers = new ArrayList<>();
        List<String> invalid = new ArrayList<>();
        boolean invalidPlayer = false;

        for (IResourceSource source : sources.layers(path)) {
            if (source.size(path) < 0)
                continue;

            boolean isPlayer = ResourceSources.PLAYER_OWNER.equals(sources.owner(source));
            JSONObject layer;
            try {
                layer = read(source, path);
            } catch (ResourceException e) {
                // The other layers keep their values
                Logger.Warn(Logger.CATEGORY_CONFIG, "Config " + config.getDataName() + ": " + e.getMessage() + ", layer ignored");
                invalid.add(path + " (" + source.name() + ")");
                invalidPlayer |= isPlayer;
                continue;
            }

            layers.add(source.name());
            if (isPlayer)
                ConfigData.MergeInto(player, layer);
            else
                ConfigData.MergeInto(defaults, layer);
        }

        config.setRead(defaults, player, layers, invalid, invalidPlayer, playerFile(sources, path));
    }

    private static JSONObject read(IResourceSource source, String path) {
        String where = path + " (" + source.name() + ")";
        try (InputStream input = source.open(path)) {
            return new JSONObject(FileMethods.readLines(input));
        } catch (JSONException e) {
            throw new ResourceException("invalid JSON in " + where + ": " + e.getMessage());
        } catch (IOException e) {
            throw new ResourceException("can't read the file " + where + ": " + e.getMessage());
        }
    }

    // The file of the player's layer, existing or not; null without layer of the player
    private static Path playerFile(ResourceSources sources, String path) {
        for (IResourceSource source : sources.sources()) {
            if (ResourceSources.PLAYER_OWNER.equals(sources.owner(source)))
                return source.localFile(path).orElse(null);
        }

        return null;
    }
}
