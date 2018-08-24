import java.awt.Rectangle;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import jason.environment.grid.Location;

public final class Target{

    //Useful information for the view in order to draw the right texture
    public static final int TARGET=16;

    //Our Model reference
    private static HouseModel model;

    public static void setModel(HouseModel model) {
        Target.model = model;
    }

    //Id Target
    private static int count=0;
    private final int id;

    public int getId() {
        return id;
    }

    //Actual position and next target to be reached
    private Location position, destination;

    public Location getPosition() {
        return position;
    }

    //Current path to destination
    private List <Location> path;

    private Iterator<Location> pathIterator;

    //Update position and inform view
    public void walk(){
        if(pathIterator!=null && pathIterator.hasNext()){
            Location next=pathIterator.next();
            while (!model.isFree(next)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            model.updateTarget(position, next);
            position=next;
        }
        else
            destination=null;
         //System.out.println("X: "+position.x+" Y: "+position.y);
    }

    public Target(){

        //Assign id
        id=count++;

        //Add reference in the model
        model.addTarget(this);

        //Choose spawn Position
        position=chooseSpawnPosition();

        //It's time to start walking around
        walkThread();
    }

    //Found a target position on diffferent room to reach
    private void findDestination(){
        while (true) {
           int roomId= ThreadLocalRandom.current().nextInt(model.getRooms().length);
           Rectangle room=model.getRooms()[roomId];
           if(!room.contains(position.x, position.y)){
                Location location=new Location(ThreadLocalRandom.current().nextInt(room.x, room.x+room.width),
                ThreadLocalRandom.current().nextInt(room.y, room.y+room.height));
                if(model.isFree(location)){
                    destination=location;
                    return;
                }
           }
        }
    }

    //Found right spawn position 
    private Location chooseSpawnPosition(){
        Rectangle room=model.getRooms()[ThreadLocalRandom.current().nextInt(model.getRooms().length)];
        Location location =new Location(ThreadLocalRandom.current().nextInt(room.x, room.x+room.width),
        ThreadLocalRandom.current().nextInt(room.y, room.y+room.height));
        if(model.isFree(location))
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
                        }
                    Thread.sleep(1500);
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