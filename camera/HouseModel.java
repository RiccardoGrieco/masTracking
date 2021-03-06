package camera;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Iterator;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import jason.asSyntax.PredicateIndicator;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.Location;
import jason.asSyntax.Literal;


//Helper Model class
public class HouseModel extends GridWorldModel{

    //Environment dimensions
    public static final int HEIGHT=21, WIDTH=21;

    //Environment description path
    private static final String PATH="resources/";

    //WalkableGraph useful for targets
    private final DefaultUndirectedGraph<Location,DefaultEdge> walkableGraph=new DefaultUndirectedGraph<>(DefaultEdge.class);
    KShortestSimplePaths<Location,DefaultEdge> builderPaths;

    //Maximum Target number
    private static final int MAX_TARGET = 2;        //TODO TO CHANGE!

    //Target and Agent  List useful for the environment
    private final List<Target> targets = new ArrayList<>();
    
    private List<AgentModel> cameraAgents = new ArrayList<>();
    
    //Contains link between an agent and its target and viceversa
    private Map<AgentModel, Target> agentsTrackingMap;         
    private Map<Target, AgentModel> inverseAgentsTrackingMap;   

    //Visibility area map
    private final Map<Location, AgentModel.YellowBox> yellowBoxes=new HashMap<>();

    //Info about room sizes
    private Rectangle[] rooms;

    public Map<Location, AgentModel.YellowBox> getYellowBoxes() {
        return yellowBoxes;
    }

    public void addTarget(Target target){
        targets.add(target);
    }

    public List<Target> getTargets() {
        return targets;
    }

    public void addCameraAgent(AgentModel agent) {
        cameraAgents.add(agent);
    }

    public List<AgentModel> getCameraAgents() {
        return cameraAgents;
    }

    public Map<AgentModel, Target> getAgentsTrackingMap() {
        return agentsTrackingMap;
    }

    public Map<Target, AgentModel> getInverseAgentsTrackingMap() {
        return inverseAgentsTrackingMap;
    }

    public Rectangle[] getRooms() {
        return rooms;
    }

    public HouseModel(List<AgentModel> agents, String level, String rooms){
        super(WIDTH, HEIGHT, agents.size());

        agentsTrackingMap  = new HashMap<>();
        inverseAgentsTrackingMap = new HashMap<>();

        cameraAgents = agents;

        //Add camera and their visibility area to Canvas 
        registerCamera();

        //Parsing char matrix from level description
        char[][] walls=new char[HEIGHT][];
        parseLevelMatrix(level,walls);

        //Add wall to the model
        addGraphicsWall(walls);

        //Build the graph walkable by Target
        buildWalkableGraph(walls);

        //Parse rooms area description 
        findRooms(rooms);

        //Set reference to this model in all targets
        Target.setModel(this);

        //Set reference to this model in all agents
        AgentModel.setModel(this);

        //Start Target Spawner Thread
        spawnerTargetThread();
        
    }


    //Helper method to parse level description from file and build a char matix rappresentation
    private void parseLevelMatrix(String location,char[][] walls){
        FileReader file;
        LineNumberReader lReader;

        //Current string line
        String str;

        //Current line (y axis)
        int currentLine;

        try {
			file=new FileReader(PATH+location);
		} catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        lReader=new LineNumberReader(file);
        try {
			while((str = lReader.readLine())!=null) {
                currentLine=lReader.getLineNumber();
                if (currentLine>HEIGHT){
                    lReader.close();
                    return;
                }
                walls[currentLine-1]=str.toCharArray();

            }    
            lReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    //Add walls (as obastacles) to Canvas
    private void addGraphicsWall(char[][] walls){
        for (int i = 0; i < walls.length; i++) {
            for (int j = 0; j < walls[i].length; j++) {
                if(walls[i][j]!='0')
                addWall(j, i,j, i);
            }
        }
    }

    //Build walkable position by target graph 
    private void buildWalkableGraph(char[][] walls){

        //Build Support Matrix adj
        Location matrix[][]=new Location[HEIGHT][WIDTH];
        for (int i = 0; i < HEIGHT; i++) {
            for (int j = 0; j < WIDTH; j++) {
                if(walls[i][j]=='0'){
                    matrix[i][j]=new Location(i, j);
                    walkableGraph.addVertex(matrix[i][j]);
                }
            }
        }

        //Find path on the same horizontal line
        for (int i = 0; i < HEIGHT; i++) {
            for (int j = 0; j < WIDTH-1; j++) {
                if(matrix[i][j]!=null && matrix[i][j+1]!=null)
                    walkableGraph.addEdge(matrix[i][j], matrix[i][j+1]);
            }
        }

        //find path on the same vertical line
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT-1; j++) {
                if(matrix[j][i]!=null && matrix[j+1][i]!=null)
                    walkableGraph.addEdge(matrix[j][i], matrix[j+1][i]);
            }
        }

        //Create path finder (path shorter (or equals to) than K)
        builderPaths=new KShortestSimplePaths<>(walkableGraph);
    }

    //Build Rooms area from their description
    private void findRooms(String file){
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
        JSONObject jsonObject=new JSONObject(result);
        JSONArray jsonArray=jsonObject.getJSONArray("rooms");
        rooms= new Rectangle[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                jsonObject=jsonArray.getJSONObject(i);
                int x,y, width, height;
                x=jsonObject.getInt("topLeftX");
                y=jsonObject.getInt("topLeftY");
                width=jsonObject.getInt("width");
                height=jsonObject.getInt("height");
                rooms[i]=new Rectangle(x,y,width,height);
            }
    }


    //Return a random path form start point to end point with a number of stemp <=80
    public List<Location> getAPath(Location start, Location end){           
       List<GraphPath<Location,DefaultEdge>> paths= builderPaths.getPaths(start, end, 80);
       return paths.get(ThreadLocalRandom.current().nextInt(paths.size())).getVertexList();
    }

    //Avoid repaint Canvas
    public void updateTarget(Location old, Location next){
        synchronized (this){
            remove(Target.TARGET, old.x, old.y);
            add(Target.TARGET,next);
        }
        
    }

    //Span target manager
    private void spawnerTargetThread(){
        Thread thread=new Thread(new Runnable(){
            @Override
            public void run() {
                int count=0;
                while(count<MAX_TARGET){
                    try {
                        synchronized(Target.BLOCK_LIST){
                            Target newTarget = new Target();
                            targets.add(newTarget);
                            System.out.println("A wild target appears in position (" + 
                                newTarget.getPosition() + ")!");

                            //Create a new target and inform view
                            add(Target.TARGET, newTarget.getPosition());
                            count++;

                            //Notify environment about new target
                            Target.BLOCK_LIST.add(newTarget);
                            Target.BLOCK_LIST.notifyAll();
                        }
                        //Take a long nap
                        Thread.sleep(20000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    //Inform view about camera agents and their view zone
    private void registerCamera(){
        for (AgentModel agent : cameraAgents) {
            add(AgentModel.CAMERA, agent.getPosition());
            initYellowBoxes(agent);
            
        }
        initSpecialYellowBoxes();

        for (AgentModel.YellowBox box : yellowBoxes.values()) {
            add(box.getId(), box.getPosition().x, box.getPosition().y);
        }
    }

    //Find view area for each camera
    private void initYellowBoxes(AgentModel agent){
        Rectangle viewZone=agent.getViewZone();
        Location position=agent.getPosition();
        for (int i = (int)viewZone.getMinX(); i < (int)viewZone.getMaxX(); i++) {
            for (int j = (int)viewZone.getMinY(); j < (int)viewZone.getMaxY(); j++){
                AgentModel.YellowBox box=yellowBoxes.get(new Location(i, j));
                if(box!=null)
                    box.setId(2*box.getId());
                else if(position.x==i && position.y==j)
                    continue;
                else
                    yellowBoxes.put(new Location(i, j), new AgentModel.YellowBox(i, j));
            }
        }
    }

    //Shared view area
    private void initSpecialYellowBoxes(){
        int a =WIDTH/2;
        int b=HEIGHT/5;
        for (int j = b; j < (b+1)*3; j+=a) {
            for (int i = 0; i < 3; i++) {
                Location location=new Location(j+i, 10);
                AgentModel.YellowBox box=new AgentModel.YellowBox(j+i, 10);
                if (i==1)
                    box.setId(8*box.getId());
                else
                    box.setId(2*box.getId());
                yellowBoxes.put(location, box);
            }
        }
        
        for (int j = b; j < (b+1)*3; j+=a) {
            for (int i = 0; i < 3; i++) {
                Location location=new Location( 10,j+i);
                AgentModel.YellowBox box=new AgentModel.YellowBox( 10,j+i);
                if (i==1)
                    box.setId(8*box.getId());
                else
                    box.setId(2*box.getId());
                yellowBoxes.put(location, box);
            }
        }
    }

    /**
     * Return true if and only if 'agent' is tracking 'target'.
     */
    public boolean isAlreadyTracking(AgentModel agent, Target target) {

        if(agentsTrackingMap.get(agent) == null) return false;  // is tracking no one
        
        if(agentsTrackingMap.get(agent).getId() == target.getId()) return true;

        return false;   // is tracking a different target
    }
}