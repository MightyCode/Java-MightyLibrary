package io.github.mightycode.mightylib.utils.enginecommand.general;

import io.github.mightycode.mightylib.main.Views;

import io.github.mightycode.mightylib.graphics.GLResources;
import io.github.mightycode.mightylib.graphics.utils.GenDeleteResources;
import io.github.mightycode.mightylib.utils.enginecommand.BaseCommand;
import io.github.mightycode.mightylib.utils.enginecommand.CommandResult;
import org.json.JSONObject;

public class GLGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "gl";

    public GLGenCom() {
        super(COMMAND_NAME);
    }

    @Override
    public CommandResult execute(String[] args) {
        JSONObject data = new JSONObject();

        JSONObject glObjects = new JSONObject();
        for (GenDeleteResources.EGraphicResources type : GenDeleteResources.EGraphicResources.values())
            glObjects.put(type.name(), GenDeleteResources.Count(type));

        data.put("glObjects", glObjects);
        JSONObject shared = Views.engine().glResources().describe();

        if (args.length > 1 && args[1].equals("summary")) {
            data.put("sharedElements", summarize(shared));
            return CommandResult.ok("OpenGL objects alive, shared elements and references by type", data);
        }

        data.put("sharedElements", shared);

        return CommandResult.ok("OpenGL objects alive and shared elements with their number of references", data);
    }

    // By type: "elements/references" instead of every element
    private static JSONObject summarize(JSONObject shared) {
        JSONObject summary = new JSONObject();

        for (String type : shared.keySet()) {
            JSONObject elements = shared.optJSONObject(type);
            if (elements == null) {
                summary.put(type, shared.get(type));
                continue;
            }

            int references = 0;
            for (String name : elements.keySet())
                references += elements.optInt(name);

            summary.put(type, elements.length() + "/" + references);
        }

        return summary;
    }

    @Override
    public String help() {
        return super.help() +
                "\ngl : number of OpenGL objects alive by type, and elements shared by GLResources with their references." +
                "\nCompare it before and after a scene change to find leaks." +
                "\ngl summary : same, shared elements given by type as elements/references";
    }
}
