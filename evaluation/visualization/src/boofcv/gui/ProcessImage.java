package boofcv.gui;

import boofcv.io.image.ImageListManager;

import java.awt.image.BufferedImage;


/**
 * Common interface for visualization applications that process a single input image.
 *
 * @author Peter Abeles
 */
public interface ProcessImage {

	public void setImageManager( ImageListManager manager );

	public void process( BufferedImage image );

	public boolean getHasProcessedImage();
}
