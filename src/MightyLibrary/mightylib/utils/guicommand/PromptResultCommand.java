package MightyLibrary.mightylib.utils.guicommand;

public class PromptResultCommand extends ResultCommand {
    public static final String PROMPT_RESULT = "ShowResultPrompt";
    public PromptResultCommand(String text){
        super();

        addAction(PROMPT_RESULT, new Object[]{text});
    }

    public PromptResultCommand(String text, String breakLineDelimiter){
        super();

        addAction(PROMPT_RESULT, new Object[]{text, breakLineDelimiter});
    }
}