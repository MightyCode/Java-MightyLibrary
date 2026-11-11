package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;
import io.github.mightycode.mightylib.sounds.SoundManager;
import io.github.mightycode.mightylib.sounds.SoundSource;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import io.github.mightycode.mightylib.utils.enginecommand.ECommandLevel;
import io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode;
import io.github.mightycode.mightylib.utils.math.KeyTreeNode;
import org.joml.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;

import static io.github.mightycode.mightylib.utils.enginecommand.syntax.CommandNode.literal;

public class SoundsGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "sounds";

    public SoundsGenCom() {
        super(ListGeneralCommand.Id(COMMAND_NAME), ECommandLevel.DEV,
                "The sound sources (state, position, gains) and the listener, or the gain tree");
    }

    @Override
    protected void syntax(CommandNode root) {
        root.executes("Compare the owned sources before and after a scene change to find the forgotten ones",
                        context -> sources())
                .then(literal("gains", "The nodes of the gain tree: own value and value with their ancestors")
                        .executes(context -> gains()));
    }

    private static CommandResult sources() {
        SoundManager manager = Views.engine().soundManager();

        JSONArray sources = new JSONArray();
        StringBuilder message = new StringBuilder();
        int owned = 0;
        for (SoundSource source : manager.getSources()) {
            if (source.isOwned())
                ++owned;

            String state = source.state();
            Vector3f position = source.getPosition();

            JSONObject json = new JSONObject();
            json.put("sound", String.valueOf(source.getSoundName()));
            json.put("state", state);
            json.put("owned", source.isOwned());
            json.put("loop", source.isLooping());
            json.put("relative", source.isRelative());
            json.put("fallback", source.playsFallback());
            json.put("position", vector(position));
            json.put("gain", round(source.getSelfGain()));
            json.put("finalGain", round(source.getFinalGain()));
            json.put("gainNode", source.getGainNodeName());
            sources.put(json);

            message.append("\n").append(source.getSoundName()).append(" ").append(state)
                    .append(source.isOwned() ? ", owned" : "").append(source.isLooping() ? ", loop" : "")
                    .append(source.playsFallback() ? ", fallback" : "")
                    .append(" at ").append(text(position))
                    .append(", gain ").append(format(source.getSelfGain()))
                    .append(" -> ").append(format(source.getFinalGain()))
                    .append(" (").append(source.getGainNodeName()).append(")");
        }

        Vector3f listener = manager.getListener().get3DPosition();

        JSONObject data = new JSONObject();
        data.put("count", sources.length());
        data.put("owned", owned);
        data.put("listener", vector(listener));
        data.put("sources", sources);

        return CommandResult.ok(sources.length() + " sound source(s), " + owned + " owned, listener at "
                + text(listener) + message, data);
    }

    private static CommandResult gains() {
        SoundManager manager = Views.engine().soundManager();

        JSONArray nodes = new JSONArray();
        StringBuilder message = new StringBuilder();
        for (KeyTreeNode<String, Float> node : manager.getGainNodes()) {
            Float value = node.getValue();
            float total = manager.getNodeGain(node.getKey());

            JSONObject json = new JSONObject();
            json.put("node", node.getKey());
            json.put("parent", node.parent() == null ? JSONObject.NULL : node.parent().getKey());
            json.put("value", value == null ? JSONObject.NULL : round(value));
            json.put("total", round(total));
            nodes.put(json);

            message.append("\n").append(node.getKey())
                    .append(node.parent() == null ? "" : " (in " + node.parent().getKey() + ")")
                    .append(": ").append(value == null ? "-" : format(value))
                    .append(" -> ").append(format(total));
        }

        JSONObject data = new JSONObject();
        data.put("count", nodes.length());
        data.put("nodes", nodes);
        return CommandResult.ok(nodes.length() + " gain node(s)" + message, data);
    }

    private static JSONObject vector(Vector3f vector) {
        JSONObject json = new JSONObject();
        json.put("x", round(vector.x));
        json.put("y", round(vector.y));
        json.put("z", round(vector.z));
        return json;
    }

    private static String text(Vector3f vector) {
        return "(" + format(vector.x) + ", " + format(vector.y) + ", " + format(vector.z) + ")";
    }

    private static double round(float value) {
        return Math.round(value * 1000) / 1000.0;
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
