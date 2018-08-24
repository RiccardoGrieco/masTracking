import jason.asSemantics.Agent;
import jason.asSyntax.*;
import jason.environment.Environment;
import jason.environment.grid.Location;

import java.util.List;
import java.util.logging.Logger;

import java.awt.Rectangle;

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

        //TODO togliere gli init fasulli da camera.asl, tracker, etc...
        initCameraAgentsPositions();

        initCameraAgentsViewZones();

        setCameraAgentsNoNeighbors();
        
        updatePercepts();
        
    }

    /** 
     * Update agents' percepts based on the HouseModel.
     */
    void updatePercepts() {
        Location targetPos = null;
        Rectangle camViewZone = null;

        // non va bene: il letterale 'target' lo usiamo solo quando un agente si accorge di un target.
        for (Target target : model.getTargets()) {
            Location position=target.getPosition();
            addPercept(Literal.parseLiteral("target("+
            String.valueOf(target.getId())+""+
            String.valueOf(position.x)+
            ","+String.valueOf(position.y)+")"));
        }

        //TODO trova un modo per implementare quest'idea
        for(Agent cam : model.getCameraAgents()) {
            // camViewZone = cam.getViewZone();

            for(Target target : model.getTargets()) {
                targetPos = target.getPosition();
                
                // if(camViewZone.contains(targetPos.x, targetPos.y))
                // addPercept(cam.getName(), target(targetPos.x, targetPos.y));
            }
        }
    }


    @Override
    public boolean executeAction(String ag, Structure action) {
       return true;
    }

    /**
     * Init the camera agents' positions. They are fixed.
     * PRECONDITIONS: the world grid must be 21x21 with the wall at the edges
     * and the camera agents must be 16.
     */
    private void initCameraAgentsPositions() {
        // Room 1 (up-sx)
        addPercept("camera1", Literal.parseLiteral("myPosition(1, 1)"));
        addPercept("camera2", Literal.parseLiteral("myPosition(9, 1)"));
        addPercept("camera3", Literal.parseLiteral("myPosition(1, 9)"));
        addPercept("camera4", Literal.parseLiteral("myPosition(9, 9)"));

        // Room 2 (up-dx)
        addPercept("camera5", Literal.parseLiteral("myPosition(11, 1)"));
        addPercept("camera6", Literal.parseLiteral("myPosition(19, 1)"));
        addPercept("camera7", Literal.parseLiteral("myPosition(11, 9)"));
        addPercept("camera8", Literal.parseLiteral("myPosition(19, 9)"));

        // Room 3 (down-sx)
        addPercept("camera9", Literal.parseLiteral("myPosition(1, 11)"));
        addPercept("camera10", Literal.parseLiteral("myPosition(9, 11)"));
        addPercept("camera11", Literal.parseLiteral("myPosition(1, 19)"));
        addPercept("camera12", Literal.parseLiteral("myPosition(9, 19)"));

        // Room 4 (down-dx)
        addPercept("camera13", Literal.parseLiteral("myPosition(11, 11)"));
        addPercept("camera14", Literal.parseLiteral("myPosition(19, 11)"));
        addPercept("camera15", Literal.parseLiteral("myPosition(11, 19)"));
        addPercept("camera16", Literal.parseLiteral("myPosition(19, 19)"));
    }

    /**
     * Init the camera agents' view zones. They are fixed.
     * The view zones are rectangles, expressed by two points (down-sx, up-dx).
     * PRECONDITIONS: the world grid must be 21x21 with the wall at the edges.
     * and the camera agents must be 16.
     */
    private void initCameraAgentsViewZones() {
        // Room 1 (up-sx)
        addPercept("camera1", Literal.parseLiteral("canSee(5, 1, 1, 5)"));
        addPercept("camera2", Literal.parseLiteral("canSee(5, 5, 9, 1)"));
        addPercept("camera3", Literal.parseLiteral("canSee(1, 9, 5, 5)"));
        addPercept("camera4", Literal.parseLiteral("canSee(5, 9, 9, 5)"));

        // Room 2 (up-dx)
        addPercept("camera5", Literal.parseLiteral("canSee(5, 11, 1, 15)"));
        addPercept("camera6", Literal.parseLiteral("canSee(5, 15, 19, 1)"));
        addPercept("camera7", Literal.parseLiteral("canSee(11, 9, 5, 15)"));
        addPercept("camera8", Literal.parseLiteral("canSee(11, 15, 5, 19)"));

        // Room 3 (down-sx)
        addPercept("camera9", Literal.parseLiteral("canSee(1, 15, 5, 11)"));
        addPercept("camera10", Literal.parseLiteral("canSee(5, 15, 9, 11)"));
        addPercept("camera11", Literal.parseLiteral("canSee(1, 19, 5, 15)"));
        addPercept("camera12", Literal.parseLiteral("canSee(5, 19, 9, 15)"));

        // Room 4 (down-dx)
        addPercept("camera13", Literal.parseLiteral("canSee(15, 11, 11, 15)"));
        addPercept("camera14", Literal.parseLiteral("canSee(15, 15, 19, 11)"));
        addPercept("camera15", Literal.parseLiteral("canSee(11, 19, 15, 15)"));
        addPercept("camera16", Literal.parseLiteral("canSee(15, 19, 19, 15)"));
    }

    /**
     * Set the camera agent's number of neighbors.
     * Neighbors are:
     * - agents that share the room (mobile robot too),
     * - agents divided by a wall that share a room entry.
     * PRECONDITIONS: the world grid must be 21x21 with the wall at the edges.
     * and the camera agents must be 16.
     */
    private void setCameraAgentsNoNeighbors() {
        // Room 1 (up-sx)
        addPercept("camera1", Literal.parseLiteral("noNeighbors(4)"));
        addPercept("camera2", Literal.parseLiteral("noNeighbors(5)"));
        addPercept("camera3", Literal.parseLiteral("noNeighbors(5)"));
        addPercept("camera4", Literal.parseLiteral("noNeighbors(6)"));

        // Room 2 (up-dx)
        addPercept("camera5", Literal.parseLiteral("noNeighbors(5)"));
        addPercept("camera6", Literal.parseLiteral("noNeighbors(4)"));
        addPercept("camera7", Literal.parseLiteral("noNeighbors(6)"));
        addPercept("camera8", Literal.parseLiteral("noNeighbors(5)"));

        // Room 3 (down-sx)
        addPercept("camera9", Literal.parseLiteral("noNeighbors(5)"));
        addPercept("camera10", Literal.parseLiteral("noNeighbors(6)"));
        addPercept("camera11", Literal.parseLiteral("noNeighbors(4)"));
        addPercept("camera12", Literal.parseLiteral("noNeighbors(5)"));

        // Room 4 (down-dx)
        addPercept("camera13", Literal.parseLiteral("noNeighbors(6)"));
        addPercept("camera14", Literal.parseLiteral("noNeighbors(5)"));
        addPercept("camera15", Literal.parseLiteral("noNeighbors(5)"));
        addPercept("camera16", Literal.parseLiteral("noNeighbors(4)"));
    }
}
