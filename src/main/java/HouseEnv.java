import jason.asSyntax.*;
import jason.environment.Environment;
import java.util.logging.Logger;

public class HouseEnv extends Environment {

    //Model variable
    private HouseModel model;

    // common literals
    public static final Literal of  = Literal.parseLiteral("open(fridge)");

    static Logger logger = Logger.getLogger(HouseEnv.class.getName());

    @Override
    public void init(String[] args) {
        model=new HouseModel(16);
        if (args.length == 1 && args[0].equals("gui")) {
           HouseView view=new HouseView(model);
           model.setView(view);
        }

        updatePercepts();
        
    }

    /** creates the agents percepts based on the HouseModel */
    void updatePercepts() {

    }


    @Override
    public boolean executeAction(String ag, Structure action) {
       return true;
    }
}
