import jason.asSyntax.*;
import jason.environment.Environment;
import java.util.logging.Logger;

public class HouseEnv extends Environment {

    // common literals
    public static final Literal of  = Literal.parseLiteral("open(fridge)");

    static Logger logger = Logger.getLogger(HouseEnv.class.getName());

    @Override
    public void init(String[] args) {
        if (args.length == 1 && args[0].equals("gui")) {
            //TODO ui
        }
    
        
    }

    /** creates the agents percepts based on the HouseModel */
    void updatePercepts() {

    }


    @Override
    public boolean executeAction(String ag, Structure action) {
       return true;
    }
}
