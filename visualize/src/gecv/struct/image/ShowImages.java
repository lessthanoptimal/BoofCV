package gecv.struct.image;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Displays images in a new window.
 *
 * @author Peter Abeles
 */
public class ShowImages {

    /**
     * Creates a dialog window showing the specified image.  The function will not
     * exit until the user clicks ok
     */
    public static void showDialog( BufferedImage img )
    {
        ImageIcon icon = new ImageIcon();
        icon.setImage(img);
        JOptionPane.showMessageDialog(null, icon);
    }

    /**
     * Creates a window showing the specified image.
     */
    public static ImagePanel showWindow( BufferedImage img , String title )
    {
        JFrame frame = new JFrame(title);

        ImagePanel panel = new ImagePanel(img);

        frame.add(panel, BorderLayout.CENTER);

        frame.pack();
        frame.setVisible(true);

        return panel;
    }
}
