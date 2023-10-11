package MightyLibrary.mightylib.utils.enginecommand;

public abstract class CommandMethods {
    public static int indexFirstWord(String command){
        int i = -1;
        boolean stop = true;
        char current;

        if (command.length() > 0){
            stop = false;
            i = 0;
        }

        while (i < command.length() && !stop){
            current  = command.charAt(i);
            if (current == ' '){
                stop = true;
            } else if ((int)current >= 32 && (int)current <= 126){
                ++i;
            } else {
                stop = true;
                i = -1;
            }
        }
        return i;
    }
}
