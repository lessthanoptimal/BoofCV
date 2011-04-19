package gecv.struct.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author Peter Abeles
 */
public class UtilImageIO {

    /**
     * A function that load the specified image.  If anything goes wrong it returns a
     * null.
     */
    public static BufferedImage loadImage( String fileName ) {
        BufferedImage img;
        try {
            img = ImageIO.read(new File(fileName));
        } catch (IOException e) {
            return null;
        }

        return img;

    }

    public static void saveImage( BufferedImage img , String fileName ) {
        try {
            String type;
            String a[] = fileName.split("[.]");
            if( a.length > 0 ) {
                type = a[a.length-1];
            } else {
                type = "jpg";
            }

            ImageIO.write(img,type,new File(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
