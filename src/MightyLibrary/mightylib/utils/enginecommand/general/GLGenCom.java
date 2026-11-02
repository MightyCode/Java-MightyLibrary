package MightyLibrary.mightylib.utils.enginecommand.general;

import MightyLibrary.mightylib.graphics.GLResources;
import MightyLibrary.mightylib.graphics.utils.GenDeleteResources;
import MightyLibrary.mightylib.utils.enginecommand.BaseCommand;
import MightyLibrary.mightylib.utils.enginecommand.CommandResult;
import org.json.JSONObject;

public class GLGenCom extends BaseCommand {
    public static final String COMMAND_NAME = "gl";

    public GLGenCom() {
        super(COMMAND_NAME, GENERAL_COMMAND);
    }

    @Override
    public CommandResult execute(String[] args) {
        JSONObject data = new JSONObject();

        JSONObject glObjects = new JSONObject();
        for (GenDeleteResources.EGraphicResources type : GenDeleteResources.EGraphicResources.values())
            glObjects.put(type.name(), GenDeleteResources.Count(type));

        data.put("glObjects", glObjects);
        data.put("sharedElements", GLResources.getInstance().describe());

        return CommandResult.ok("OpenGL objects alive and shared elements with their number of references", data);
    }

    @Override
    public String help() {
        return super.help() +
                "\ngl : number of OpenGL objects alive by type, and elements shared by GLResources with their references." +
                "\nCompare it before and after a scene change to find leaks.";
    }
}
