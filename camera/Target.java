package camera;

import java.awt.Rectangle;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import jason.environment.grid.Location;


/*Class that model a Target. A target is something that moves within the environment identified as relevant by agents.
  A target should be always tracked by an agent and is the subject of auctions between agents*/
public final class Target{

    //Contains Target that have just moved. Useful to keep agents focused on them
    public static final LinkedList<Target> BLOCK_LIST=new LinkedList<>();

    //Id Target
    private static int count=0;
    private final int id;

    //Traget tracked by Agent(idAgent)
    private String idAgent = null;

    //Id that agent assigns to target
    private int progressiveNumber;

    //Useful information for the view in order to draw the right texture
    public static final int TARGET=16;

    //Our Model reference
    private static HouseModel model;

    //Actual, previous and target position 
    private Location position, oldPosition, destination;

    //Current path to follow
    private List <Location> path;

    //Next step position iterator
    private Iterator<Location> pathIterator;


    //Two target are equals if and only if they have same id
    public boolean equals(Object other) {
        Target otherT = (Target) other;

        return id == otherT.id;
    }

    //Helpful model for MAS Console
    public String toString() {
        return String.format("[%d %d %d]", 
                            id, position.x, position.y);
    }

    //In order to match equals
    public int hashCode() {
        return id;
    }

    public void setIdAgent(String id) {
        idAgent = id;
    }

    public void setPosition(Location newPos) {
        position = newPos;
    }

    public void setProgressiveNumber(int num) {
        progressiveNumber = num;
    }

    public static void setModel(HouseModel model) {
        Target.model = model;
    }

    public int getId() {
        return id;
    }

    public Location getPosition() {
        return position;
    }

    public Location getOldPosition() {
        return oldPosition;
    }

    public void setOldPosition(Location oldPosition) {
        this.oldPosition = oldPosition;
    }

    public String getIdAgent() {
        return idAgent;
    }

    public int getProgressiveNumber() {
        return progressiveNumber;
    }

    public Target(){

        //Assign next available id 
        id = count++;

        //Add reference in the model
        //model.addTarget(this);

        //Choose spawn Position
        position = chooseSpawnPosition();
        oldPosition = null;
        
        //It's time to start walking around
        walkThread();
    }


    //Move to next position inform model and MAS environment
    public void walk(){
        if(pathIterator!=null && pathIterator.hasNext()){
            //Find next step
            Location next = pathIterator.next();

            //No space for me in the next area
            /*while (!model.isFree(AgentModel.CAMERA,next) || !model.isFree(TARGET,next)) {
                try {
                    //Oh I'll be wait here :(
                    pathIterator = null;
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/
            
            //Please inform View to redraw 
            model.updateTarget(position, next);

            //Inform environment about my move
            synchronized(BLOCK_LIST){
                oldPosition=position;
                position = next;
                BLOCK_LIST.add(this);
                BLOCK_LIST.notifyAll();
            }
        }

        //Time to find a new trip
        else
            destination=null;
    }


    //Found new position on different room to reach
    private void findDestination(){
        while (true) {
            //Choose a room
            int roomId= ThreadLocalRandom.current().nextInt(model.getRooms().length);
            Rectangle room=model.getRooms()[roomId];

            //nextRomm!=actualRoom 
            if(!room.contains(position.x, position.y)){
                //Generate casual free location 
                Location location=new Location(ThreadLocalRandom.current().nextInt(room.x, room.x+room.width),
                ThreadLocalRandom.current().nextInt(room.y, room.y+room.height));
                if(model.isFree(AgentModel.CAMERA,location)){
                    destination=location;
                    return;
                }
           }
        }
    }

    //Found free (no camera) spawn position 
    private Location chooseSpawnPosition(){
        Rectangle room=model.getRooms()[ThreadLocalRandom.current().nextInt(model.getRooms().length)];
        Location location =new Location(ThreadLocalRandom.current().nextInt(room.x, room.x+room.width),
        ThreadLocalRandom.current().nextInt(room.y, room.y+room.height));
        if(model.isFree(AgentModel.CAMERA, location))
            return location;
        else
            return chooseSpawnPosition();
    }

    //Moves target around the rooms
    private void walkThread(){
        Thread thread=new Thread(new Runnable(){
            @Override
            public void run() {
                while(true){
                    try {
                        if(destination==null){
                            findDestination();
                            path=model.getAPath(position, destination);
                            pathIterator=path.iterator();
                            if(pathIterator!=null && pathIterator.hasNext())
                                pathIterator.next();
                        }
                        
                    //Let me breathe I am breathless
                    Thread.sleep(2000);
                    walk();
				    } catch (Exception e) {
					    e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }
}