package internalActions;

import jason.asSyntax.*;
import jason.asSemantics.*;
import java.util.ArrayList;
import java.util.List;
import java.lang.Exception;


public class firstAction extends DefaultInternalAction {

    public static final List<Agent> AGENTS_LIST = new ArrayList<>();

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        Agent agent = ts.getAg();
        
        AGENTS_LIST.add((AgentModel)agent);
        System.out.println("eseguo firstAction");
        return null;
    }
}