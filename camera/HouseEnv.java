package camera;
import jason.asSemantics.Agent;
import jason.asSyntax.*;
import jason.bb.BeliefBase;
import jason.bb.DefaultBeliefBase;
import jason.environment.Environment;
import jason.environment.grid.Location;
import jason.stdlib.foreach;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import camera.Target;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


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

    //Agents Descriptions
    private JSONArray agentsJSON;

    private String[] initArgs;


    @Override
    public void init(String[] args) {

       firstAction.env=this;

       initArgs=args;

        targetsRecentlyNotified = new ForgetfulSet<Target>();
        losingTargetsRecentlyNotified = new ForgetfulSet<AgentModel>();
        
        new Thread(){
            @Override
            public void run(){
                while(true){
                    synchronized(Target.BLOCK_LIST){
                        while(Target.BLOCK_LIST.isEmpty()){
                            try{
                                Target.BLOCK_LIST.wait();
                            }catch(InterruptedException ie){}
                            
                        }
                        try {
                         Thread.sleep(1000);
                        } catch (Exception e) {
                            //TODO: handle exception
                        }
                        updatePercepts();
                    }
                }
            }
        }.start();
        

        //Loading Json
        parseAgents("agents.json");

        //INIT JASON AGENTS

        initCameraAgentsPositions();

        initCameraAgentsViewZones();

        setCameraAgentsNoNeighbors();        

        addPercept(Literal.parseLiteral("numberOfAgents(16)"));

     //   updatePercepts();
    }

    public void setUpModel_View(){
        //INIT MODEL AGENTS

        List<AgentModel> agents = initCameraAgentsPositionsModel();

        initCameraAgentsShadowZone(agents);

        initCameraAgentsViewZonesModel(agents); //TODO

        model = new HouseModel(agents, initArgs[1], initArgs[2]);


        if (initArgs[0].equals("gui")) {
           HouseView view = new HouseView(model);
           model.setView(view);
        }

        updatePercepts();
    }

    private void parseAgents(String file){
        String result=null;
        try {
            InputStream inputStream = new FileInputStream("resources/"+file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            result = sb.toString();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

       agentsJSON =new JSONObject(result).getJSONArray("agents");
    }

    public void initCameraAgentsShadowZone(List<AgentModel> agents){
        
        for (int i = 0; i < agentsJSON.length(); i++) {
            JSONObject obj= agentsJSON.getJSONObject(i);
            AgentModel camera=null;
            for (AgentModel agent  : agents) {
                if(agent.getName().equalsIgnoreCase(obj.getString("name"))){
                    camera=agent;
                    break;
                }
            }
            List<Point> points=new ArrayList<>();
            
            JSONArray pointArray= obj.getJSONArray("shadowZones");
            for (int j = 0; j < pointArray.length(); j++) {
                JSONObject pointObj=pointArray.getJSONObject(j);
                points.add(new Point(pointObj.getInt("x"), pointObj.getInt("y")));
            }

            camera.setShadowZones(points);
        }

    }



    /**
     * 
     */
    List<AgentModel> initCameraAgentsPositionsModel(){

        for (int i = 0; i < agentsJSON.length(); i++) {
            JSONObject agentJSON = agentsJSON.getJSONObject(i);
            JSONObject pos = agentJSON.getJSONObject("position");
            String name = agentJSON.getString("name");
            int x=pos.getInt("x");
            int y=pos.getInt("y");
            for(AgentModel agent : firstAction.AGENTS_LIST) {
                if(agent.getName().equalsIgnoreCase(name)) 
                    agent.setLocation(new Location(x, y));
            }

        }

        return  firstAction.AGENTS_LIST;
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
            boolean moved = Target.BLOCK_LIST.contains(target);
            targetPos = target.getPosition();
            tracker = model.getInverseAgentsTrackingMap().get(target);    // agent who is tracking target

            if(tracker != null) {
                //TODO remove
                if(target.getIdAgent()==null){ 
                    System.out.println("Agente "+tracker.getName()+" ha il target per magia.");
                    continue;
                }
                
                System.out.println("Il target " + target.getId() + "e' tracciato");
                // ** TRACKING **
                // update tracking percept in agent's BB
                boolean inShadowZones = tracker.isInShadowZones(targetPos);
                if(camViewZone.contains(targetPos.x, targetPos.y) || inShadowZones){
                    addPercept(tracker.getName(), 
                        Literal.parseLiteral("tracking(" + target.getIdAgent() + ", " + 
                        target.getProgressiveNumber() + ", " + target.getPosition().x + ", " + 
                        target.getPosition().y + ")"));
                    
                    if(inShadowZones)
                        addPercept(tracker.getName(), 
                                    Literal.parseLiteral(String.format("losingTarget(%s,%d,%d,%d)",
                                                         target.getIdAgent(), target.getProgressiveNumber(),
                                                         targetPos.x, targetPos.y)));
                    
                }
                else{
                    removePercept(tracker.getName(), 
                        Literal.parseLiteral("tracking(" + target.getIdAgent() + ", " + 
                        target.getProgressiveNumber() + ", _, _)"));
                }
            }


            //if(targetsRecentlyNotified.contains(target)) continue;

            for(AgentModel agent : model.getCameraAgents()) {
                camViewZone = agent.getViewZone();

                if(tracker == null || !tracker.equals(agent)) {
                    // ** TARGET **
                    // A camera agent percepts a target if and only if the target is in
                    // the camera view zone and the camera is not tracking it yet.
                    if((camViewZone.contains(targetPos.x, targetPos.y) || agent.isInShadowZones(targetPos))&& 
                            ! model.isAlreadyTracking(agent, target)){
                        System.out.println("L'agente " + agent.getName() + " ha trovato il target " + target.getId());
                        addPercept(agent.getName(), 
                            Literal.parseLiteral("target(" + targetPos.x + ", " + targetPos.y + ")"));
                    }
                }
            }

            //targetsRecentlyNotified.add(target);
        }

        /*for(AgentModel cam : model.getCameraAgents()) {

            if(losingTargetsRecentlyNotified.contains(cam)) continue;

            camViewZone = cam.getViewZone();

            // ** LOSING TARGET **
            if(isLosingItsTarget(cam)) {
                System.out.println("Sono entrato nell'if isLosingItsTarget: agente " + cam.getName());
                Target target = model.getAgentsTrackingMap().get(cam);

                addPercept(cam.getName(), 
                    Literal.parseLiteral("losingTarget(" + 
                        target.getIdAgent() + ", " + target.getIdAgent() + ", " + 
                        target.getPosition().x + ", " + target.getPosition().y + ")"));
            }

            losingTargetsRecentlyNotified.add(cam);
        }*/

        Target.BLOCK_LIST.clear();
    }

    /**
     * Update, in model, info about targets:
     * - update the tracking maps
     * - update the target's id-pair
     */
    private void updateModel() {
        List<Target> freeTargets = new ArrayList<>(model.getTargets());
        Iterator<Literal> itLiteral = null;

        // clear the tracking-map cause there may be lost targets
        Map<AgentModel, Target> agentToTracked = model.getAgentsTrackingMap();
        Map<Target, AgentModel> trackedToAgent = model.getInverseAgentsTrackingMap();
        agentToTracked.clear();
        trackedToAgent.clear();

        synchronized(Target.BLOCK_LIST){
            for(Target target : model.getTargets()){
                Location loc = null;
                if(Target.BLOCK_LIST.contains(target)){
                    loc = target.getOldPosition();
                    if(loc==null) continue;
                }
                else{
                    loc = target.getPosition();
                }

                boolean found = false;
                for(AgentModel ag : model.getCameraAgents()){
                    itLiteral = ag.getBB().getCandidateBeliefs(new PredicateIndicator("tracking",4));
                    if(itLiteral==null) continue;
                    if(itLiteral.hasNext()){
                        Literal lit = itLiteral.next();
                        int x = Integer.valueOf(lit.getTerm(2).toString());
                        int y = Integer.valueOf(lit.getTerm(3).toString());
                        if(loc.x==x && loc.y==y){
                            String agentId = lit.getTerm(0).toString();
                            int progressiveId = Integer.valueOf(lit.getTerm(1).toString());
                            target.setIdAgent(agentId);
                            target.setProgressiveNumber(progressiveId);
                            agentToTracked.put(ag, target);
                            trackedToAgent.put(target, ag);
                            found = true;
                            break;
                        }
                    }
                }
                if(!found){
                    target.setIdAgent(null);
                    target.setProgressiveNumber(-1);
                }
            }


        }

        // manage tracking changes
        /*for(AgentModel agent : model.getCameraAgents()) {
            
            BeliefBase bb = agent.getBB();
            PredicateIndicator pi = new PredicateIndicator("tracking", 4);
            itLiteral = bb.getCandidateBeliefs(pi);
            if(itLiteral==null) continue;
            
            if(itLiteral.hasNext()) {
                Literal lit = itLiteral.next();
                String name = lit.getTermsArray()[0].toString();
                for (int i = 0; i < lit.getTermsArray().length; i++) {
                    System.out.println("Agent: "+agent.getName()+" Terms"+i+"----"+lit.getTermsArray()[i]);
                }
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
                
                itLiteral = agent.getBB().getCandidateBeliefs(new PredicateIndicator("tracking", 4));
                if(itLiteral==null) continue;
                
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
            for(Agent agent : model.getCameraAgents()){
                agent.getBB().remove(Literal.parseLiteral(String.format("tracking(%s,%d,_,_)", target.getIdAgent(), target.getProgressiveNumber())));
            }
            target.setIdAgent(null);
            target.setProgressiveNumber(-1);
        }*/
    }

    private Map<AgentModel, Long> losing = new HashMap<>();
    private static final long DELTA_TIME_LOSING = 500; //TODO set it properly!

    /**
     * Serve to manage the literal 'loosingTraget'.
     * An agent is loosing it's target if he is on the agent's view zone edge
     * and the tracking lasts for 4 seconds.
     * @return true if and only if 'agent' is loosing it's target.
     */
    private boolean isLosingItsTarget(AgentModel agent) {
        // tracking check
        Target tracked = model.getAgentsTrackingMap().get(agent);
        //System.out.println("Agente " + agent.getName() + " VZ " + agent.getViewZone());
        //System.out.println("Agente " + agent.getName() + " VZ " + agent.getViewZone().getBounds());
        if(tracked == null) {
            return false;
        }

        // target at border check
        Location trackedLocation = tracked.getPosition();
        Rectangle viewZone = agent.getViewZone();
        Point trackedPosition = new Point(trackedLocation.x, trackedLocation.y);

        for(Point point : agent.getshadowZones()){
            if(point.equals(trackedPosition)) {
                // delta time check
                Long lastTimeLosing = losing.get(agent);
                long currentTimeMillis = System.currentTimeMillis();
                if(lastTimeLosing!=null && currentTimeMillis-lastTimeLosing<DELTA_TIME_LOSING) return false;

                losing.put(agent, currentTimeMillis);
                System.out.println("Sto per ritornare true! isLosingTarget");
                return true;
            }
        }

        return false;
    }


    @Override
    public boolean executeAction(String ag, Structure action) {
        AgentModel agent = null;
        Target target = null;
        Location targetLoc = null;
        boolean result = false;

        System.out.println("Esegue executeAction");
        System.out.println("ag: "  + ag + " action: " + action.toString());

        if(action.getFunctor().equals("track")) {
            for(AgentModel agS : model.getCameraAgents()) {
                if(agS.getName().equals(ag)) agent = agS; 
            }

            targetLoc = new Location(Integer.valueOf(action.getTerms().get(2).toString()), 
                Integer.valueOf(action.getTerms().get(3).toString()));

            for(Target tar : model.getTargets()) {
                if(targetLoc.equals(tar.getPosition())) target = tar;
            }

            model.getAgentsTrackingMap().put(agent, target);
            model.getInverseAgentsTrackingMap().put(target, agent);

            System.out.println(String.format("aggiunto tracking di (%s,%s) a %s", action.getTerms().get(0).toString(),
            action.getTerms().get(1).toString(), agent.getName()));

            addPercept(agent.getName(), 
                Literal.parseLiteral("tracking(" + 
                action.getTerms().get(0).toString() + ", "+
                action.getTerms().get(1).toString() + ", "+
                action.getTerms().get(2).toString() + ", "+
                action.getTerms().get(3).toString() +
                ")"));
            result = true;
        }
        else if(action.getFunctor().equals("observeTarget")) {
            result = true;
        }

        if(result) updatePercepts();
        
        return result;
    }

    /**
     * Init the camera agents' positions. They are fixed.
     * PRECONDITIONS: the world grid must be 21x21 with the wall at the edges
     * and the camera agents must be 16.
     */
    private void initCameraAgentsPositions() {

        for (int i = 0; i < agentsJSON.length(); i++) {
            JSONObject obj= agentsJSON.getJSONObject(i);
            JSONObject pos=obj.getJSONObject("position");
            Integer x=pos.getInt("x"), y=pos.getInt("y");
            addPercept(obj.getString("name"),Literal.parseLiteral("myPosition("+x+", "+y+")"));
            }
    }

    /**
     * Init the camera agents' view zones. They are fixed.
     * The view zones are rectangles, expressed by two points (down-sx, up-dx).
     * PRECONDITIONS: the world grid must be 21x21 with the wall at the edges.
     * and the camera agents must be 16.
     */
    private void initCameraAgentsViewZones() {

        for (int i = 0; i < agentsJSON.length(); i++) {
            JSONObject obj= agentsJSON.getJSONObject(i);
            JSONArray cArray=obj.getJSONArray("canSee");
            JSONObject pos1=cArray.getJSONObject(0) , pos2=cArray.getJSONObject(1);
            Integer x1=pos1.getInt("x1"), y1=pos1.getInt("y1"), x2=pos2.getInt("x2"), y2=pos2.getInt("y2");
            addPercept(obj.getString("name"),Literal.parseLiteral("canSee("+x1+", "+y1+", "+x2+", "+y2+")"));
            }


        // Room 1 (up-sx)
  /*  addPercept("camera1", Literal.parseLiteral("canSee(1, 5, 5, 1)"));
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
        addPercept("camera16", Literal.parseLiteral("canSee(15, 19, 19, 15)"));*/
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

        for (int i = 0; i < agentsJSON.length(); i++) {
            JSONObject obj= agentsJSON.getJSONObject(i);
            int x=obj.getInt("noNeighbors");
            addPercept(obj.getString("name"),Literal.parseLiteral("noNeighbors("+x+")"));
        }
    }
}
