import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import jason.environment.grid.GridWorldView;

public class HouseView extends GridWorldView{

    private final HouseModel model;
    private final static String TITLE="MAS Tracking", FONT="Arial";
    private final  static int WINDOW_SIZE=700, FONT_SIZE=16;

    HouseView(HouseModel model){
        super(model, TITLE, WINDOW_SIZE);
        this.model=model;
        defaultFont=new Font(FONT,Font.BOLD,FONT_SIZE);
        setVisible(true);
        repaint();
    }

    @Override
    public void draw(Graphics g, int x, int y, int object) {
        super.draw(g, x, y, object);
    }

    @Override
    public void drawAgent(Graphics g, int x, int y, Color c, int id) {
        super.drawAgent(g, x, y, c, id);
    }
}