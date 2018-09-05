package camera;
import jason.architecture.*;
import jason.asSemantics.Agent;
import jason.environment.grid.Location;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;


public class AgentModel extends Agent {
    private String name;
    protected Location position;
    private Rectangle viewZone;
    private boolean canMove;
    private int radius;

    public static final int CAMERA = 32;

    private List<Point> shadowZones;

    /**
     * @return the points
     */
    public List<Point> getshadowZones() {
        return shadowZones;
    }

    /**
     * @param shadowZones the shadowZones to set
     */
    public void setShadowZones(List<Point> shadowZones) {
        this.shadowZones = shadowZones;
    }

    private static HouseModel model;

    /**
     * Constructor.
     */

     public AgentModel(){
     }
    public AgentModel(String n, int x, int y, boolean canM) {
        name = n;
        position = new Location(x, y);
        canMove = canM;
    }

    public boolean equals(Object other) {
        AgentModel otherA = (AgentModel) other;

        return name == otherA.name;
    }

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

    /**
     * @param name the name to set
     */
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

    /**
     * @return true if and only is the agent can move
     */
    public boolean canMove() {
        return canMove;
    }


    /* ----------------- INNER CLASSES ----------------- */
    
    /**
     * A moving agent can do what a camera agent can do, but also he moves!
     */
    public  static final class MovingAgent extends AgentModel {
        private List<Location> checkpoints;             // Fixed
        private Iterator<Location> checkpointsIterator;
        private Location nextCheckpoint;                // Next position to reach
        int waitTime;
        long lastStep;
        
        /**
         * Constructor.
         */
        public MovingAgent(String name, int x, int y, List<Location> cp){
            super(name, x, y, true);

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

        /**
         * @return the id
         */
        public int getId() {
            return id;
        }

        /**
         * @param id the id to set
         */
        public void setId(int id) {
            this.id = id;
        }

        /**
         * @return the position
         */
        public Location getPosition() {
            return position;
        }

        public YellowBox(int x,int y){
            position=new Location(x, y);
        }
    }
}