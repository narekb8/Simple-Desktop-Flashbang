package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/*
 * A JComponent Class for the Flashbang JFrame
 * This class is used to take the after image, make it transparent and draw it to the flashbang's JFrame
 * Functions like any basic canvas, just paints it using the image instead
 */

public class TransparentImage extends JComponent
{
    BufferedImage img;
    public Graphics2D g2d;

    public TransparentImage(BufferedImage Image)
    {
        img = Image;
    }

    protected void paintComponent(Graphics g)
    {
        Graphics2D g2D = (Graphics2D) g;

	    AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .3f);
	    g2D.setComposite(composite); // Set current alpha
	    g2D.drawImage(img, 0, 0, null);

    }
}
