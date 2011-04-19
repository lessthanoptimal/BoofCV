package gecv.struct.image;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;


/**
 * Simple JPanel for displaying buffered images.
 *
 * @author Peter Abeles
 */
public class ImagePanel extends JPanel
{
    // the image being displayed
    BufferedImage img;

    public ImagePanel( BufferedImage img ) {
        this.img = img;
        setPreferredSize(new Dimension(img.getWidth(),img.getHeight()));
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
    }

    @Override
    public void paintComponent(Graphics g) {
        //draw the image
        if( img != null)
            g.drawImage(img,0,0, this);
    }

    /**
     * Change the image being displayed.
     *
     * @param image The new image which will be displayed.
     */
    public void setBufferedImage( BufferedImage image ) {
        this.img = image;
    }
}
