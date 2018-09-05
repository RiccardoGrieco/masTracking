package camera;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import jason.environment.grid.GridWorldView;

public final class HouseView extends GridWorldView{

    //Custom colors
    private static final Color MY_WHITE=new Color(238,238,238);
   

    //Our model useful to update the canvas when new object are added
    private final HouseModel model;

    //Texture location
    private final static String TITLE="MAS Tracking", FONT="Arial", ICON="resources/icon.png", 
            WALL="resources/walltexture.jpg", ROBOT="resources/robotexture.png",
            CAMERA="resources/cameratexture.png", TARGET="resources/targetTexture.jpg";

    //Misc variables
    private final static int WINDOW_SIZE=700, FONT_SIZE=16;
    
    //Texture library
    private static final Map<String,Image> TEXTURE_LIBRARY= new HashMap<>();

    //Load texture one time for all
    static{
        loadTexture();
    }

    public HouseView(HouseModel model){
        super(model, TITLE, WINDOW_SIZE);

        //Bind to model
        this.model=model;

        //Misc Swing configuration
        defaultFont=new Font(FONT,Font.BOLD,FONT_SIZE);
        ImageIcon icon=new ImageIcon(ICON);
        setIconImage(icon.getImage());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setVisible(true);
    }

    private static void loadTexture(){
        try {
            TEXTURE_LIBRARY.put(WALL, ImageIO.read(new File(WALL)));
            TEXTURE_LIBRARY.put(ROBOT, ImageIO.read(new File(ROBOT)));
            TEXTURE_LIBRARY.put(CAMERA, ImageIO.read(new File(CAMERA)));
            TEXTURE_LIBRARY.put(TARGET, ImageIO.read(new File(TARGET)));
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

    //Generic draw for object not provided by GridWorldView
    @Override
    public void draw(Graphics g, int x, int y, int object) {
        //TODO
        // disegna la view zone dell'agente camera in trasparenza (vediamo come viene)
        switch(object){
            case Target.TARGET:
            drawTarget(g, x, y);
            break;

            case AgentModel.CAMERA:
            drawCamera(g,x,y);
            break;

            case AgentModel.YellowBox.YELLOW_BOX:
            drawYellowBox(g, x, y, AgentModel.YellowBox.MY_YELLOW);
            break;

            case AgentModel.YellowBox.YELLOW_BOX*2:
            drawYellowBox(g, x, y, AgentModel.YellowBox.MY_YELLOW.darker());
            break;

            case AgentModel.YellowBox.YELLOW_BOX*4:
            drawYellowBox(g, x, y, AgentModel.YellowBox.MY_YELLOW.darker().darker());
            break;

            case AgentModel.YellowBox.YELLOW_BOX*8:
            drawYellowBox(g, x, y, AgentModel.YellowBox.MY_YELLOW.darker().darker().darker());
            break;
        }
    }

    public void drawYellowBox(Graphics g, int x, int y, Color color) {
        g.setColor(color);
        g.fillRect(x * cellSizeW + 1, y * cellSizeH+1, cellSizeW-1, cellSizeH-1);

    }

     public void drawCamera(Graphics g, int x, int y ){
        g.drawImage(TEXTURE_LIBRARY.get(CAMERA), x * cellSizeW+1, y*cellSizeH+1, cellSizeW-1, cellSizeH-1,null);
        g.setColor(AgentModel.YellowBox.MY_YELLOW);
        g.fillRect(x * cellSizeW + 1, y * cellSizeH+1, cellSizeW-1, cellSizeH-1);
     }

    
    @Override
    public void drawAgent(Graphics g, int x, int y, Color c, int id) {
        super.drawAgent(g, x, y, c, id);
        //TODO
        // draw camera agents (fixed position)
        // draw mobile agent (?)(variable position)
    }

    @Override
    public void drawObstacle(Graphics g, int x, int y) {
       g.drawImage(TEXTURE_LIBRARY.get(WALL), x * cellSizeW+1, y*cellSizeH+1, cellSizeW-1, cellSizeH-1,null);
    }

    public void drawTarget(Graphics g, int x, int y){
        g.drawImage(TEXTURE_LIBRARY.get(TARGET), x * cellSizeW+1, y*cellSizeH+1, cellSizeW-1, cellSizeH-1,null);

    }

    //Custom drawEmpty in order to avoid to repaint all the canvas
    @Override
    public void drawEmpty(Graphics g, int x, int y) {
            g.setColor(MY_WHITE);
            g.fillRect(x * cellSizeW + 1, y * cellSizeH+1, cellSizeW-1, cellSizeH-1);
            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(x * cellSizeW, y * cellSizeH, cellSizeW, cellSizeH);
    }
}