import jason.environment.grid.GridWorldModel;

public class HouseModel extends GridWorldModel{
    private static final int HEIGHT=20, WEIGHT=20;
    public HouseModel(int nAgents){
        super(WEIGHT,HEIGHT,nAgents);
    }
}