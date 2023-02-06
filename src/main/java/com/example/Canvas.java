package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/*
 * A JComponent class for the Flashbang JFrame
 * This class is a basic canvas for the flashbang effect's JFrame, just paints it white
 */

public class Canvas extends JComponent
{
    private int w;
    private int h;

    public Canvas(int width, int height)
    {
        w = width;
        h = height;
    }

    protected void paintComponent(Graphics g)
    {
        Graphics2D g2d = (Graphics2D) g;
        Rectangle2D.Double r = new Rectangle2D.Double(0, 0, w, h);

        g2d.setColor(Color.white);
        g2d.fill(r);
    }
}