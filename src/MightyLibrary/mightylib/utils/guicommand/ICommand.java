package MightyLibrary.mightylib.utils.guicommand;

public interface ICommand{
    ResultCommand process(String[] args);
    ResultCommand returnHelp();
}