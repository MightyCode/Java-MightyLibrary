package MightyLibrary.mightylib.util.guicommand;

public interface ICommand{
    ResultCommand process(String[] args);
    ResultCommand returnHelp();
}