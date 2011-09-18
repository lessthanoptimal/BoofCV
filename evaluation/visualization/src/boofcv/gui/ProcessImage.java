package boofcv.gui;

import boofcv.io.image.ImageListManager;


/**
 * Common interface for visualization applications that process a single input image.
 *
 * @author Peter Abeles
 */
public interface ProcessImage {

	public void setImageManager( ImageListManager manager );

	public boolean getHasProcessedImage();
}
