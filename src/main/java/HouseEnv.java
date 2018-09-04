import jason.asSemantics.Agent;
import jason.asSyntax.*;
import jason.environment.Environment;
import jason.environment.grid.Location;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import java.awt.Rectangle;

public final class HouseEnv extends Environment {

    public static class ForgetfulSet<T> extends HashSet<T> {
        @Override
        public synchronized boolean add(T obj) {
            if(super.add(obj)) {
                new Thread() {
                    @Override public void run() {
                        try {
                            Thread.sleep(1500);
                        }
                        catch(Exception e) {}
                        remove(obj);
                    }
                };
                return true;
            }
            return false;
        }

        @Override
        public synchronized boolean remove(Object obj){
        return super.remove(obj);
        }

        @Override
        public synchronized Iterator<T> iterator(){
            return super.iterator();
        }

        @Override
        public synchronized boolean contains(Object obj){
            return super.contains(obj);
        }
    }

    private static final int AGENT_RADIUS = 5;

    //Model variable
    private HouseModel model;

    private ForgetfulSet<Target> targetsRecentlyNotified;

    private ForgetfulSet<AgentModel> losingTargetsRecentlyNotified;

    // common literals
    public static final Literal targetPositiony  = Literal.parseLiteral("target(id,x,y)");

    //Logger 
    static Logger logger = Logger.getLogger(HouseEnv.class.getName());


    @Override
    public void init(String[] args) {

        targetsRecentlyNotified = new ForgetfulSet<Target>();
        losingTargetsRecentlyNotified = new ForgetfulSet<AgentModel>();
        

        //TODO togliere gli init fasulli da camera.asl, tracker, etc...
        initCameraAgentsPositions();

        initCameraAgentsViewZones();

        setCameraAgentsNoNeighbors();

        List<AgentModel> agents = initCameraAgentsPositionsModel();

        initCameraAgentsViewZonesModel(agents);

        model = new HouseModel(agents, args[1], args[2]);
        if (args[0].equals("gui")) {
           HouseView view = new HouseView(model);
           model.setView(view);
        }

        System.out.println("World initialized.");

        updatePercepts();
    }

    /**
     * 
     */
    List<AgentModel> initCameraAgentsPositionsModel(){
        List<AgentModel> agents = new LinkedList<>();

        // Room 1 (up-sx)
        agents.add(new AgentModel.CameraAgent("camera1",1,1));
        agents.add(new AgentModel.CameraAgent("camera2",9,1));
        agents.add(new AgentModel.CameraAgent("camera3",1,9));
        agents.add(new AgentModel.CameraAgent("camera4",9,9));

        // Room 2 (up-dx)
        agents.add(new AgentModel.CameraAgent("camera5",11,1));
        agents.add(new AgentModel.CameraAgent("camera6",19,1));
        agents.add(new AgentModel.CameraAgent("camera7",11,9));
        agents.add(new AgentModel.CameraAgent("camera8",19,9));

        // Room 3 (down-sx)
        agents.add(new AgentModel.CameraAgent("camera9",1,11));
        agents.add(new AgentModel.CameraAgent("camera10",9,11));
        agents.add(new AgentModel.CameraAgent("camera11",1,19));
        agents.add(new AgentModel.CameraAgent("camera12",9,19));

        // Room 4 (down-dx)
        agents.add(new AgentModel.CameraAgent("camera13",11,11));
        agents.add(new AgentModel.CameraAgent("camera14",19,11));
        agents.add(new AgentModel.CameraAgent("camera15",11,19));
        agents.add(new AgentModel.CameraAgent("camera16",19,19));

        return agents;
    }

    /**
     * 
     */
    private void initCameraAgentsViewZonesModel(List<AgentModel> agents){
        for(AgentModel agent : agents)
            agent.setRadius(AGENT_RADIUS);
    }

    /** 
     * Update agents' percepts based on the HouseModel.
     */
    void updatePercepts() {
        Location    targetPos = null;
        AgentModel tracker = null;

        Rectangle camViewZone = null;

        //clearAllPercepts();

        // non va bene: il letterale 'target' lo usiamo solo quando un agente si accorge di un target.
        /*for (Target target : model.getTargets()) {
            Location position=target.getPosition();
            addPercept(Literal.parseLiteral("target("+
            String.valueOf(target.getId())+""+
            String.valueOf(position.x)+
            ","+String.valueOf(position.y)+")"));
        }*/

        //updateTargets();
        updateModel();

        for(Target target : model.getTargets()) {
            targetPos = target.getPosition();
            tracker = model.getInverseAgentsTrackingMap().get(target);    // agent who is tracking target

            if(tracker != null) {
                // ** TRACKING **
                // update tracking percept in agent's BB
                addPercept(tracker.getName(), 
                    Literal.parseLiteral("tracking(" + target.getIdAgent() + ", " + 
                    target.getProgressiveNumber() + ", " + target.getPosition().x + ", " + 
                    target.getPosition().y + ")"));
            }

            if(targetsRecentlyNotified.contains(target)) continue;

            for(AgentModel agent : model.getCameraAgents()) {
                camViewZone = agent.getViewZone();

                if(tracker == null || !tracker.equals(agent)) {
                    // ** TARGET **
                    // A camera agent percepts a target if and only if the target is in
                    // the camera view zone and the camera is not tracking it yet.
                    if(camViewZone.contains(targetPos.x, targetPos.y) && 
                            ! model.isAlreadyTracking(agent, target))
                        addPercept(agent.getName(), 
                            Literal.parseLiteral("target(" + targetPos.x + ", " + targetPos.y + ")"));
                }
            }

            targetsRecentlyNotified.add(target);
        }
        
        for(AgentModel cam : model.getCameraAgents()) {

            if(losingTargetsRecentlyNotified.contains(cam)) continue;

            camViewZone = cam.getViewZone();

            // ** LOOSING TARGET **
            if(isLosingItsTarget(cam)) {
                Target target = model.getAgentsTrackingMap().get(cam);

                addPercept(cam.getName(), 
                    Literal.parseLiteral("losingTarget(" + 
                        target.getIdAgent() + ", " + target.getIdAgent() + ", " + 
                        target.getPosition().x + ", " + target.getPosition().y + ")"));
            }

            losingTargetsRecentlyNotified.add(cam);
        }
    }

    /**
     * Update, in model, info about targets:
     * - update the tracking maps
     * - update the target's id-pair
     */
    private void updateModel() {
        List<Target> freeTargets = new ArrayList<>(model.getTargets());
        Iterator<Literal> itLiteral = null;

        // clear the tracking-map cause there may be losted targets
        model.getAgentsTrackingMap().clear();
        model.getInverseAgentsTrackingMap().clear();

        // manage tracking changes
        for(AgentModel agent : model.getCameraAgents()) {
            try {
                itLiteral = agent.getBB().getCandidateBeliefs(new PredicateIndicator("tracking", 4));
            }
            catch(NullPointerException e) {
                continue;   // predicate not found: go to next agent.
            }

            if(itLiteral.hasNext()) {
                Literal lit = itLiteral.next();
                String name = lit.getTermsArray()[0].toString();
                int progressive = Integer.valueOf(lit.getTermsArray()[1].toString());

                for(Target target : model.getTargets()) {
                    if(name.equals(target.getIdAgent()) && target.getProgressiveNumber() == progressive) {
                        model.getAgentsTrackingMap().put(agent, target);
                        model.getInverseAgentsTrackingMap().put(target, agent);
                        freeTargets.remove(target);
                    }
                }
            }
        }

        for(Target target : freeTargets) {
            boolean tracked = false;

            for(AgentModel agent : model.getCameraAgents()) {
                try {
                    itLiteral = agent.getBB().getCandidateBeliefs(new PredicateIndicator("tracking", 4));
                }
                catch(NullPointerException e) {
                    continue;   // predicate not found: go to next agent.
                }
                // if agent is tracking someone
                if(itLiteral.hasNext()) {
                    Literal lit = itLiteral.next();

                    // take tracking position
                    int x = Integer.valueOf(lit.getTermsArray()[2].toString());
                    int y = Integer.valueOf(lit.getTermsArray()[3].toString());

                    // agent is tracking someone else 
                    if(target.getPosition().x != x || target.getPosition().y != y) continue;

                    // target is properly the target tracked by agent
                    model.getAgentsTrackingMap().put(agent, target);
                    model.getInverseAgentsTrackingMap().put(target, agent);
                    freeTargets.remove(target);
                    tracked = true;

                    // if target has no id-pair
                    if(target.getIdAgent() == null) {
                        String idAgent = lit.getTermsArray()[0].toString();
                        int progressiveN = Integer.valueOf(lit.getTermsArray()[1].toString());
                        
                        // it is the very first time for target: we must set it's id-pair 
                        if(target.getPosition().x == x && target.getPosition().y == y) {
                            target.setIdAgent(idAgent);
                            target.setProgressiveNumber(progressiveN);
                        }
                    }
                }
            }

            // target is not tracked: we must reset it's id-pair
            if(tracked == false) target.setIdAgent(null);
        }

        for(Target target : freeTargets) {
            target.setIdAgent(null);
            target.setProgressiveNumber(-1);
        }
    }

    private Map<AgentModel, Long> losing = new HashMap<>();
    private static final long DELTA_TIME_LOSING = 4000; //TODO set it properly!

    /**
     * Serve to manage the literal 'loosingTraget'.
     * An agent is loosing it's target if he is on the agent's view zone edge
     * and the tracking lasts for 4 seconds.
     * @return true if and only if 'agent' is loosing it's target.
     */
    private boolean isLosingItsTarget(AgentModel agent) {
        // tracking check
        Target tracked = model.getAgentsTrackingMap().get(agent);
        if(tracked==null)
            return false;

        // target at border check
        Location trackedLocation = tracked.getPosition();
        Rectangle viewZone = agent.getViewZone();
        if(trackedLocation.x!=viewZone.x || trackedLocation.x!=viewZone.x+viewZone.width ||
            trackedLocation.y!=viewZone.y || trackedLocation.y!=viewZone.y+viewZone.height)
            return false;

        // delta time check
        Long lastTimeLosing = losing.get(agent);
        long currentTimeMillis = System.currentTimeMillis();
        if(lastTimeLosing!=null && currentTimeMillis-lastTimeLosing<DELTA_TIME_LOSING) return false;

        losing.put(agent, currentTimeMillis);

        return true;
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
        addPercept("camera1", Literal.parseLiteral("canSee(1, 5, 5, 1)"));
        addPercept("camera2", Literal.parseLiteral("canSee(5, 5, 10, 0)"));
        addPercept("camera3", Literal.parseLiteral("canSee(0, 10, 5, 5)"));
        addPercept("camera4", Literal.parseLiteral("canSee(5, 10, 10, 5)"));

        // Room 2 (up-dx)
        addPercept("camera5", Literal.parseLiteral("canSee(10, 5, 15, 1)"));
        addPercept("camera6", Literal.parseLiteral("canSee(15, 5, 19, 1)"));
        addPercept("camera7", Literal.parseLiteral("canSee(10, 10, 15, 5)"));
        addPercept("camera8", Literal.parseLiteral("canSee(15, 10, 19, 5)"));

        // Room 3 (down-sx)
        addPercept("camera9", Literal.parseLiteral("canSee(1, 15, 5, 10)"));
        addPercept("camera10", Literal.parseLiteral("canSee(5, 15, 10, 10)"));
        addPercept("camera11", Literal.parseLiteral("canSee(1, 19, 5, 15)"));
        addPercept("camera12", Literal.parseLiteral("canSee(5, 19, 10, 15)"));

        // Room 4 (down-dx)
        addPercept("camera13", Literal.parseLiteral("canSee(10, 15, 15, 10)"));
        addPercept("camera14", Literal.parseLiteral("canSee(15, 15, 20, 10)"));
        addPercept("camera15", Literal.parseLiteral("canSee(10, 20, 15, 15)"));
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
