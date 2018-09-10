package camera;

import jason.asSyntax.*;
import jason.asSemantics.*;
import java.util.ArrayList;
import java.util.List;
import java.lang.Exception;

//Internal action that make possible bind a Jason agent to java class AgentModel
public class firstAction extends DefaultInternalAction {

    public static final List<AgentModel> AGENTS_LIST = new ArrayList<>();

    public static HouseEnv env;

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        
        //Obtain Jason java agent instance
        Agent agent = ts.getAg();

        synchronized (AGENTS_LIST){
            AgentModel agent2= (AgentModel)agent;

            //Add to model list
            AGENTS_LIST.add(agent2);

            //camera.firstAction(my_name).
            agent2.setName(args[0].toString());

            if(AGENTS_LIST.size()==16)
                //Finish environment set-up
                env.setUpModel_View();
        }
        return true;
    }
}