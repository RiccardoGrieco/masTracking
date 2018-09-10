package camera;

import jason.architecture.*;
import jason.asSemantics.Agent;
import jason.environment.grid.Location;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;


//Model an agent from java point of view
public class AgentModel extends Agent {

    private static HouseModel model;

    //Same name in MAS console
    private String name;

    //where the agent is located
    protected Location position;

    //Area controlled by the agent
    private Rectangle viewZone;

    private boolean canMove;

    //Used to build the viewZone
    private int radius;

    public static final int CAMERA = 32;

    //Last points where an agent can see
    private List<Point> shadowZones;

    public List<Point> getshadowZones() {
        return shadowZones;
    }

    public void setShadowZones(List<Point> shadowZones) {
        this.shadowZones = shadowZones;
    }

    //Check if a target is coming out of the visual field
    public boolean isInShadowZones(Location point){
        if(shadowZones == null) return false;
        for(Point shadow: shadowZones){
            if (shadow.x==point.x && shadow.y==point.y)
                return true;
        }
        return false;
    }

     public AgentModel(){
         canMove=false;
     }

    //Two target are equals if and only if they have same jason name
    public boolean equals(Object other) {
        AgentModel otherA = (AgentModel) other;

        return name.equals(otherA.name);
    }

    public String toString() {
        return String.format("[%s]", 
                            name);
    }

    //In order to match equals
    public int hashCode() {
        return name.hashCode();
    }

    public void setViewZone(Rectangle rect) {
        viewZone = rect;
    }

    public static void setModel(HouseModel model) {
        AgentModel.model = model;
    }

    public void setLocation(Location location) {
        position = location;
    }

    //Set the radius and build the view zone
    public void setRadius(int radius) {
        this.radius = radius;
        int x=position.x,y=position.y;          
        if ((x>8 && x<10) || x>18)
            x-=radius-1;
        if((y>8 && y<10) || y>18)
            y-=radius-1;
        viewZone=new Rectangle(x,y,radius,radius);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getPosition() {
        return position;
    }

    public Rectangle getViewZone() {
        return viewZone;
    }

    public static HouseModel getModel() {
        return model;
    }

    public boolean canMove() {
        return canMove;
    }


    /* ----------------- INNER CLASSES ----------------- */
    
    /**
     * A moving agent can do what a camera agent can do, but also he moves!
     * NEVER USED
     */
    public static final class MovingAgent extends AgentModel {
        private List<Location> checkpoints;             // Fixed
        private Iterator<Location> checkpointsIterator;
        private Location nextCheckpoint;                // Next position to reach
        int waitTime;
        long lastStep;
        
        /**
         * Constructor.
         */
        public MovingAgent(String name, int x, int y, List<Location> cp){
            //super(name, x, y, true);
            super.canMove=false;
            checkpoints = cp;
            checkpointsIterator = cp.iterator();
            nextCheckpoint = checkpointsIterator.next();
            if(nextCheckpoint.equals(new Location(x, y))) 
                nextCheckpoint = checkpointsIterator.next();

            waitTime = (int) (1000 + Math.random() * 2000);
            lastStep = System.currentTimeMillis();
        }

        /**
         * Moves moving agent to the next checkpoint cell.
         */
        private void step() {
            int stepX,
                stepY;
            long now = System.currentTimeMillis();

            if(now - lastStep < waitTime) return;

            if(nextCheckpoint.equals(position)) {
                if(! checkpointsIterator.hasNext()) checkpointsIterator = checkpoints.iterator();
                nextCheckpoint = checkpointsIterator.next();
            }

            stepX = nextCheckpoint.x - position.x;
            stepY = nextCheckpoint.y - position.y;
            
            position.x += stepX;
            position.y += stepY;

            lastStep = now;
            waitTime = (int) (1000 + Math.random() * 2000);
        }
    }

    public static final class YellowBox {

        public static final int YELLOW_BOX = 64;

        private final Location position;
        public static final Color MY_YELLOW = new Color(255, 255, 0, 50);
        private int id = YELLOW_BOX;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Location getPosition() {
            return position;
        }

        public YellowBox(int x,int y){
            position=new Location(x, y);
        }
    }
}