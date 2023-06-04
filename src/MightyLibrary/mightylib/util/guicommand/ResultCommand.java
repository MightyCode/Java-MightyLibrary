package MightyLibrary.mightylib.util.guicommand;

import java.util.ArrayList;

public class ResultCommand {
    public final class Action{
        private final String action;
        private final Object[] args;

        public Action (String action, Object[] args){
            this.action = action;
            this.args = args;
        }

        public String getAction() { return action; }

        public boolean argsNull() { return  args == null; }
        public int argsSize() { return args == null ? 0 : args.length; }
        public Object getArgs(int i) { return args[i]; }
    }

    private final ArrayList<Action> actions;

    public ResultCommand(){
        actions = new ArrayList<>();
    }

    public ResultCommand addAction(String action, Object[] args){
        actions.add(new Action(action, args));

        return this;
    }

    public Iterable<Action> getActions() {
        return actions;
    }
}
