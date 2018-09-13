package camera;

import jason.asSemantics.Agent;
import jason.asSyntax.*;
import jason.bb.BeliefBase;
import jason.bb.DefaultBeliefBase;
import jason.environment.Environment;
import jason.environment.grid.Location;
import jason.stdlib.abolish;
import jason.stdlib.foreach;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import camera.AgentModel;
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


//MAS Environment class
public final class HouseEnv extends Environment {

    //Custom set that loses refs after 1.5sec
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

    //Agent view zone radius
    private static final int AGENT_RADIUS = 5;

    //Model variable
    private HouseModel model;

    private ForgetfulSet<Target> targetsRecentlyNotified;

    private ForgetfulSet<AgentModel> losingTargetsRecentlyNotified;

    //Agents Descriptions
    private JSONArray agentsJSON;

    //Save param used in mas2j
    private String[] initArgs;

    Target lost = null;


    //Called from jason.infra.centralised.RunCentralisedMAS
    @Override
    public void init(String[] args) {

       firstAction.env=this;

       initArgs=args;

        targetsRecentlyNotified = new ForgetfulSet<Target>();
        losingTargetsRecentlyNotified = new ForgetfulSet<AgentModel>();
        
        //Everytime that a target moves or spawn call updatePercepts
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
                         Thread.sleep(1200);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        updatePercepts();
                    }
                }
            }
        }.start();
        

        //Loading Agents Json description
        parseAgents("agents.json");

        //INIT JASON AGENTS

        initCameraAgentsPositions();

        initCameraAgentsViewZones();

        setCameraAgentsNoNeighbors();        

        addPercept(Literal.parseLiteral("numberOfAgents(16)"));

    }

    //Last steps to bind JAson agent to model and start the view
    public void setUpModel_View(){

        //INIT MODEL AGENTS
        List<AgentModel> agents = initCameraAgentsPositionsModel();

        initCameraAgentsShadowZone(agents); 

        initCameraAgentsViewZonesModel(agents);

        //Build the model
        model = new HouseModel(agents, initArgs[1], initArgs[2]);

        //Show the GUI if requested
        if (initArgs[0].equals("gui")) {
           HouseView view = new HouseView(model);
           model.setView(view);
        }
    }

    //Parse a json agent description file into a string
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


    //Create the ShadowZone (last area where agent can see) for each agent
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

    //Set-up model-agents position 
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

    //Build the view zone for each agent
    private void initCameraAgentsViewZonesModel(List<AgentModel> agents){
        for(AgentModel agent : agents)
            agent.setRadius(AGENT_RADIUS);
    }

    //Master method that simulate the environment
    void updatePercepts() {
        Location    targetPos = null;
        AgentModel tracker = null;
        Rectangle camViewZone = null;

        //Clear all percepts for each agent
        clearAllPercepts();

        updateModel();
        

        for(Target target : model.getTargets()) {
            boolean moved = Target.BLOCK_LIST.contains(target);
            targetPos = target.getPosition();
            
            //Agent who is tracking target
            tracker = model.getInverseAgentsTrackingMap().get(target);   

            //If target is on track right now
            if(tracker != null) {
                
                if(target.getIdAgent()==null){
                    //System.out.println("BOOOOOH");//TODO togliere
                    addPercept(tracker.getName(), 
                                Literal.parseLiteral("lost"));
                    continue;
                }
                camViewZone = tracker.getViewZone();
                
                // ** TRACKING **
                //Update tracking percept in agent's BB
                boolean inShadowZones = tracker.isInShadowZones(targetPos);
                
                if(camViewZone.contains(targetPos.x, targetPos.y) || inShadowZones){
                    
                    addPercept(tracker.getName(), 
                        Literal.parseLiteral("tracking(" + target.getIdAgent() + ", " + 
                        target.getProgressiveNumber() + ", " + target.getPosition().x + ", " + 
                        target.getPosition().y + ")"));
                    
                    if(inShadowZones){
                        addPercept(tracker.getName(), 
                                    Literal.parseLiteral(String.format("losingTarget(%s,%d,%d,%d)",
                                                         target.getIdAgent(), target.getProgressiveNumber(),
                                                         targetPos.x, targetPos.y)));
                        //while(true);//TODO forse rimettere
                    }
                }
                else{
                    /*removePercept(tracker.getName(), 
                        Literal.parseLiteral("tracking(" + target.getIdAgent() + ", " + 
                        target.getProgressiveNumber() + ", _, _)"));*/
                }
            }
            for(AgentModel agent : model.getCameraAgents()) {
                camViewZone = agent.getViewZone();

                if(tracker == null || target.getIdAgent() == null || !tracker.equals(agent)) {
                    // ** TARGET **
                    // A camera agent percepts a target if and only if the target is in
                    // the camera view zone and the camera is not tracking it yet.
                    if(camViewZone.contains(targetPos.x, targetPos.y) || 
                        agent.isInShadowZones(targetPos)){
                        //System.out.println("L'agente " + agent.getName() + " ha trovato il target " + target.toString());
                        //lista.add(agent);
                        addPercept(agent.getName(), 
                            Literal.parseLiteral("target(" + targetPos.x + ", " + targetPos.y + ")"));
                    }
                }
            }

            //System.out.println(lista);
            //System.out.println("Count = " + lista.size());
            //lista.clear();

            //targetsRecentlyNotified.add(target);
        }

        //System.out.println("Mappa dei tracciamenti:");
        //TODO lo tolho per le per statistiche
        /*for(AgentModel a : model.getAgentsTrackingMap().keySet()) {
            System.out.println(String.format("Agente %s traccia target %s.", a, model.getAgentsTrackingMap().get(a)));
        }
        System.out.println("");*/
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
        lost = null;
        synchronized(Target.BLOCK_LIST){
            // clear the tracking-map cause there may be lost targets
            Map<AgentModel, Target> agentToTracked = model.getAgentsTrackingMap();
            Map<Target, AgentModel> trackedToAgent = model.getInverseAgentsTrackingMap();
            agentToTracked.clear();
            trackedToAgent.clear();

          //  System.out.println("getTargets " + model.getTargets().toString());
           // System.out.println("blocklist" + Target.BLOCK_LIST.toString());
            for(Target target : model.getTargets()){
                Location loc = null;

                //Check last usable target position
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

                    //System.out.print(ag.getName()+": ");
                    if(itLiteral.hasNext()){
                        Literal lit = itLiteral.next();
                        //System.out.print(lit.toString()+" | ");
                        int x = Integer.valueOf(lit.getTerm(2).toString());
                        int y = Integer.valueOf(lit.getTerm(3).toString());
                        if(loc.x==x && loc.y==y){
                            //System.out.println("Aggiornamento mappe: Target " + target.getId() + " agente " + ag.getName());
                            //System.out.println(String.format("locx: %d locy: %d trackingX: %d trackingY: %d", loc.x, loc.y, x, y));
                            String agentId = lit.getTerm(0).toString();
                            int progressiveId = Integer.valueOf(lit.getTerm(1).toString());

                            //Last added target is tracked by this agent
                            if(target.getIdAgent() == null){
                                target.setIdAgent(agentId);
                                target.setProgressiveNumber(progressiveId);
                            }

                            agentToTracked.put(ag, target);
                            trackedToAgent.put(target, ag);
                            //TODO solo per statistiche
                            freeTargets.remove(target);
                            
                            if(ag.getViewZone().contains(new Point(target.getPosition().x, 
                                                                    target.getPosition().y)) || 
                                ag.isInShadowZones(target.getPosition())){
                                found = true;
                            }
                            break;
                        }
                    }
                }
                if(!found){
                    target.setIdAgent(null);
                    target.setProgressiveNumber(-1);
                }
            }
            //System.out.println("Mappa " + trackedToAgent.toString());
        }

        //TODO solo per statistiche
        if(freeTargets.size() != 0) 
            System.out.println("target liberi: " + 
                                freeTargets.size() + 
                                freeTargets);
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
                return true;
            }
        }

        return false;
    }


    //Implements agents custom behavior
    @Override
    public boolean executeAction(String ag, Structure action) {
        AgentModel agent = null;
        Target target = null;
        Location targetLoc = null;
        boolean result = false;

        if(action.getFunctor().equals("track")) {
            for(AgentModel agS : model.getCameraAgents()) {
                if(agS.getName().equals(ag)) agent = agS; 
            }

            targetLoc = new Location(Integer.valueOf(action.getTerms().get(2).toString()), 
                Integer.valueOf(action.getTerms().get(3).toString()));

            for(Target tar : model.getTargets()) {
                if(targetLoc.equals(tar.getPosition())) target = tar;
            }

            //model.getAgentsTrackingMap().put(agent, target);
            //model.getInverseAgentsTrackingMap().put(target, agent);

            //System.out.println(String.format("aggiunto tracking di (%s,%s) a %s", action.getTerms().get(0).toString(),
            //action.getTerms().get(1).toString(), agent.getName()));

            addPercept(agent.getName(), 
                Literal.parseLiteral("tracking(" + 
                action.getTerms().get(0).toString() + ", "+
                action.getTerms().get(1).toString() + ", "+
                action.getTerms().get(2).toString() + ", "+
                action.getTerms().get(3).toString() +
                ")"));
           
            result = true;
        }
        else if(action.getFunctor().equals("observeTarget")) 
            result = true;
        
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
