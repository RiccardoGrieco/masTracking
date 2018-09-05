package camera;
import jason.asSyntax.*;
import jason.asSemantics.*;
import java.util.ArrayList;
import java.util.List;
import java.lang.Exception;


public class firstAction extends DefaultInternalAction {

    public static final List<AgentModel> AGENTS_LIST = new ArrayList<>();
    public static HouseEnv env;

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        Agent agent = ts.getAg();
        synchronized (AGENTS_LIST){
           AgentModel agent2= (AgentModel)agent;
            AGENTS_LIST.add(agent2);
            agent2.setName("camera"+AGENTS_LIST.size());
            if(AGENTS_LIST.size()==16)
                env.setUpModel_View();
        }

        return true;
    }
}