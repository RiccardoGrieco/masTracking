import jason.asSemantics.Agent;
import jason.asSyntax.*;
import jason.environment.Environment;
import jason.environment.grid.Location;
import java.util.List;
import java.util.logging.Logger;

public final class HouseEnv extends Environment {

    //Model variable
    private HouseModel model;

    // common literals
    public static final Literal targetPositiony  = Literal.parseLiteral("target(id,x,y)");

    //Logger 
    static Logger logger = Logger.getLogger(HouseEnv.class.getName());


    @Override
    public void init(String[] args) {
        model=new HouseModel(16, args[1],args[2]);
        if (args[0].equals("gui")) {
           HouseView view=new HouseView(model);
           model.setView(view);
        }

        updatePercepts();
        
    }

    /** creates the agents percepts based on the HouseModel */
    void updatePercepts() {
        for (Target target : model.getTargets()) {
            Location position=target.getPosition();
            addPercept(Literal.parseLiteral("target("+
            String.valueOf(target.getId())+""+
            String.valueOf(position.x)+
            ","+String.valueOf(position.y)+")"));
        }
    }


    @Override
    public boolean executeAction(String ag, Structure action) {
       return true;
    }
}
