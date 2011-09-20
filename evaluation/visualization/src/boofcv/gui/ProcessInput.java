package boofcv.gui;

import boofcv.io.InputListManager;


/**
 * Common interface for visualization applications that process a single input image.
 *
 * @author Peter Abeles
 */
public interface ProcessInput {

	public void setInputManager( InputListManager manager );

	public boolean getHasProcessedImage();
}
