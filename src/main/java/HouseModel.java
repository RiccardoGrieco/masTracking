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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.KShortestSimplePaths;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.json.JSONArray;
import org.json.JSONObject;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.Location;

public class HouseModel extends GridWorldModel{

    //World dimensions
    public static final int HEIGHT=21, WIDTH=21;

    //Descriptions location
    private static final String PATH="resources/";

    //WalkableGraph
    private final DefaultUndirectedGraph<Location,DefaultEdge> walkableGraph=new DefaultUndirectedGraph<>(DefaultEdge.class);
    KShortestSimplePaths<Location,DefaultEdge> builderPaths;

    //Maximum Target number
    private static final int MAX_TARGET=5;

    //Target List useful for the environment
    private final List<Target> targets=new ArrayList<>();

    public void addTarget(Target target){
        targets.add(target);
    }

    public List<Target> getTargets() {
        return targets;
    }

    //Rooms array
    private Rectangle[] rooms;

    public Rectangle[] getRooms() {
        return rooms;
    }

    public HouseModel(int nAgents, String level, String rooms){
        super(WIDTH,HEIGHT,nAgents);

        //Parsing char matrix from level description
        char[][] walls=new char[HEIGHT][];
        parseLevelMatrix(level,walls);

        //Add wall to the model
        addGraphicsWall(walls);

        //Build the graph walkable by mobile-agents/target
        buildWalkableGraph(walls);

        //Parse rooms area description 
        findRooms(rooms);

        //Set reference to this model in all targets
        Target.setModel(this);

        //Start Target Spawner Thread
        spawnerTargetThread();
        
    }

    private void parseLevelMatrix(String location,char[][] walls){
        //Load file var
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

    private void addGraphicsWall(char[][] walls){
        for (int i = 0; i < walls.length; i++) {
            for (int j = 0; j < walls[i].length; j++) {
                if(walls[i][j]!='0')
                addWall(j, i,j, i);
            }
        }
    }

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
        //find relationship on the same horizontal line
        for (int i = 0; i < HEIGHT; i++) {
            for (int j = 0; j < WIDTH-1; j++) {
                if(matrix[i][j]!=null && matrix[i][j+1]!=null)
                    walkableGraph.addEdge(matrix[i][j], matrix[i][j+1]);
            }
        }

        //find relationship on the same vertical line
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT-1; j++) {
                if(matrix[j][i]!=null && matrix[j+1][i]!=null)
                    walkableGraph.addEdge(matrix[j][i], matrix[j+1][i]);
            }
        }
        //Create path finder
        builderPaths=new KShortestSimplePaths<>(walkableGraph);
    }

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

    public List<Location> getAPath(Location start, Location end){           
       List<GraphPath<Location,DefaultEdge>> paths= builderPaths.getPaths(start, end, 80);
       return paths.get(ThreadLocalRandom.current().nextInt(paths.size())).getVertexList();
    }

    public void updateTarget(Location old, Location next){
        synchronized (this){
            data[old.x][old.y]=0;
            view.update(old.x,old.y);
            add(Target.TARGET,next);
        }
        
    }

    private void spawnerTargetThread(){
        Thread thread=new Thread(new Runnable(){
            @Override
            public void run() {
                int count=0;
                while(count<MAX_TARGET){
                    try {
                        //Create a new target and inform view
                        add(Target.TARGET,new Target().getPosition());
                        count++;

                        //take a long nap
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }
}