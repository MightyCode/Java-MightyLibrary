package MightyLibrary.mightylib.graphics.GUI;

import MightyLibrary.mightylib.graphics.renderer._2D.shape.RectangleRenderer;
import MightyLibrary.mightylib.graphics.text.ETextAlignment;
import MightyLibrary.mightylib.graphics.text.Text;
import MightyLibrary.mightylib.inputs.InputManager;
import MightyLibrary.mightylib.inputs.KeyboardManager;
import MightyLibrary.mightylib.main.SystemInfo;
import MightyLibrary.mightylib.util.Timer;
import MightyLibrary.mightylib.util.math.Color4f;
import MightyLibrary.mightylib.util.math.EDirection;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;

public class Terminal {
    private static final String BASE_RESULT_MESSAGE = "Write command:";
    private static final float CURSOR_FIRST_PERIOD_TIME = 1;
    private static final float CURSOR_PERIOD_TIME = 0.5f;

    private final TerminalActions actions;
    private final Vector2f referencePosition;
    private final Vector2f maxSize;

    private Text resultText;
    private Text commandText;

    private final Timer cursorTimer;
    private boolean cursorDisplayed;

    private int cursorPosition;
    private final RectangleRenderer cursor;

    private final StringBuilder commandTextCpy;

    private boolean shouldProcessCommand;

    private final ArrayList<String> previousCommands;
    private int previousCommandSelected;

    public Terminal(TerminalActions actions, Vector2f referencePosition, Vector2f maxSize){
        this.actions = actions;
        this.referencePosition = referencePosition;
        this.maxSize = maxSize;

        commandTextCpy = new StringBuilder("/");

        commandText = new Text();
        commandText.setFont("bahnschrift")
                .setAlignment(ETextAlignment.Left)
                .setReference(EDirection.LeftDown)
                .setPosition(new Vector2f(referencePosition))
                .setColor(new Color4f(0.8f, 0.8f, 0.8f, 1f))
                .setFontSize(30)
                .fitText(commandTextCpy.toString(), maxSize, true, "");

        resultText = new Text();
        commandText.copyTo(resultText)
                .setColor(new Color4f(1, 1, 1, 1f))
                .setText(BASE_RESULT_MESSAGE);

        updateResultPosition();

        cursorTimer = new Timer();
        cursorTimer.start(CURSOR_FIRST_PERIOD_TIME);
        cursorDisplayed = false;

        cursor = new RectangleRenderer("colorShape2D");
        cursor.setScale(new Vector3f(3, commandText.getFontSize(), 1.f));
        cursor.switchToColorMode(new Color4f(1, 1, 1, 0.8f));
        cursor.setPosition(new Vector2f(37, 705));

        updateCursorPosition(0);

        previousCommands = new ArrayList<>();
        previousCommandSelected = 0;
    }

    public void update(InputManager manager, SystemInfo info){
        boolean textUpdate = false;
        shouldProcessCommand = false;

        KeyboardManager keyboardManager = manager.getKeyboardManager();

        if (manager.inputPressed(actions.Paste)){
            addCharToPosition(cursorPosition + 1, info.getClipboardContent());
            textUpdate = true;
        } else if (keyboardManager.getMainKeyPressed() != -1 && keyboardManager.keyPressed(keyboardManager.getMainKeyPressed())) {
            char c = keyboardManager.getCorrespondingCharMainKeyPressed();

            if (c == ((char)-1)) {
                if (manager.inputPressed(actions.DeleteRight)){
                    removeCharAtPosition(cursorPosition + 1);
                    updateCursorPosition(cursorPosition + 1);
                } else if (manager.inputPressed(actions.GoWordAtRight) && cursorPosition < commandText.text().length() - 1) {
                    updateCursorPosition(navigateWordRight());
                } else if (manager.inputPressed(actions.GoWordAtLeft)  && cursorPosition > 0) {
                    updateCursorPosition(navigateWordLeft() - 1);
                } else if (manager.inputPressed(actions.MoveCursorRight) && cursorPosition < commandText.text().length() - 1) {
                    updateCursorPosition(cursorPosition + 1);
                } else if (manager.inputPressed(actions.MoveCursorLeft) && cursorPosition > 0) {
                    updateCursorPosition(cursorPosition - 1);
                } else if (manager.inputPressed(actions.SetPreviousCommand) && previousCommandSelected > 0) {
                    --previousCommandSelected;
                    replaceCommandText(previousCommands.get(previousCommandSelected));
                } else if (manager.inputPressed(actions.SetNextCommand) && previousCommandSelected < previousCommands.size()) {
                    // Accept -1
                    ++previousCommandSelected;
                    if (previousCommandSelected == previousCommands.size())
                        replaceCommandText("/");
                    else
                        replaceCommandText(previousCommands.get(previousCommandSelected));

                } else if (manager.inputPressed(actions.DeleteLeft) && commandText.text().length() > 1){
                    removeCharAtPosition(cursorPosition);
                    textUpdate = true;
                } else if (manager.inputPressed(actions.EnterCommand) && commandText.text().length() != 1){
                    shouldProcessCommand = true;
                }
            } else {
                addCharToPosition(cursorPosition + 1, String.valueOf(c));
                textUpdate = true;
            }
        }

        if (textUpdate){
            updateResultPosition();
        }

        cursorTimer.update();
        if (cursorTimer.isFinished()){
            cursorDisplayed = !cursorDisplayed;
            cursorTimer.start(CURSOR_PERIOD_TIME);
        }
    }

    private void updateResultPosition(){
        resultText.setPosition(new Vector2f(commandText.leftUpPosition().x, commandText.leftUpPosition().y));
    }

    private void addCharToPosition(int index, String chr){
        if (index < 1 || index > commandText.text().length())
            return;

        commandTextCpy.insert(index, chr);
        commandText.fitText(commandTextCpy.toString(), maxSize, true, "");

        updateCursorPosition(cursorPosition + chr.length());
    }

    private void replaceCommandText(String newStr){
        commandTextCpy.replace(0, commandTextCpy.length(), newStr);
        commandText.setText(commandTextCpy.toString());

        updateCursorPosition(commandTextCpy.toString().length() - 1);
    }

    private void removeCharAtPosition(int index){
        if (index < 1 || index > commandText.text().length() - 1)
            return;

        commandTextCpy.deleteCharAt(index);
        commandText.fitText(commandTextCpy.toString(), maxSize, true, "");

        updateCursorPosition(cursorPosition - 1);
    }

    public int navigateWordLeft() {
        int position = cursorPosition;

        while (position > 0 && !Character.isLetterOrDigit(commandTextCpy.charAt(position - 1))) {
            position--;
        }

        while (position > 0 && Character.isLetterOrDigit(commandTextCpy.charAt(position - 1))) {
            position--;
        }

        return position;
    }

    public int navigateWordRight() {
        int length = commandTextCpy.length();
        int position = cursorPosition;

        while (position < length - 1 && Character.isLetterOrDigit(commandTextCpy.charAt(position + 1))) {
            position++;
        }

        while (position < length - 1 && !Character.isLetterOrDigit(commandTextCpy.charAt(position + 1))) {
            position++;
        }

        return position;
    }

    private void updateCursorPosition(int index){
        if (index < 0 || index > commandText.text().length() - 1)
            return;

        cursorPosition = index;
        Vector4f charPosition = commandText.getPositionOfChar(cursorPosition);
        cursor.setPosition(new Vector2f(
                (int)(charPosition.y) - commandText.getFontSize() * 0.05f,
                (int)(charPosition.z) + commandText.getFontSize() * 0.05f));

        cursorTimer.start(CURSOR_FIRST_PERIOD_TIME);
        cursorDisplayed = true;
    }

    public boolean shouldProcessCommand() {
        return shouldProcessCommand;
    }

    public String getCommandText(){
        return commandTextCpy.toString();
    }

    public Terminal saveCommand(){
        previousCommands.add(commandTextCpy.toString());

        return this;
    }

    public Terminal clearCommandText() {
        commandTextCpy.delete(0, commandTextCpy.length());
        commandTextCpy.append("/");
        commandText.fitText(commandTextCpy.toString(), maxSize, true, "");
        updateCursorPosition(0);

        previousCommandSelected = previousCommands.size();

        updateResultPosition();

        return this;
    }

    public Terminal clearResultText(){
        resultText.setText(BASE_RESULT_MESSAGE);
        commandText.fitText(commandTextCpy.toString(), maxSize, true, "");

        return this;
    }

    public Terminal addToResultText(String text){
        return addToResultText(text, "  ");
    }

    public Terminal addToResultText(String text, String breakLineStr){
        if (text == null)
            return this;

        if (text.charAt(text.length() - 1) != '\n')
            text += "\n";

        resultText.fitText(
                resultText.text().substring(0, resultText.text().length() - BASE_RESULT_MESSAGE.length()) + text + BASE_RESULT_MESSAGE,
                new Vector2f(maxSize.x, maxSize.y * 500),
                false, breakLineStr);

        return this;
    }

    public void display(){
        commandText.display();
        resultText.display();

        if (cursorDisplayed) {
            cursor.display();
        }
    }

    public void unload(){
        commandText.unload();
        resultText.unload();


        cursor.unload();
    }
}
