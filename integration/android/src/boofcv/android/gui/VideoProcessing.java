package boofcv.android.gui;

import android.graphics.Canvas;
import android.hardware.Camera;
import android.view.View;

/**
 * Interface for processing and visualizing camera previews.
 *
 * @author Peter Abeles
 */
public interface VideoProcessing {

	public void init( View view , Camera camera , Camera.CameraInfo info );

	/**
	 * Called inside the GUI thread
	 *
	 * @param canvas
	 */
	public void onDraw(Canvas canvas);


	/**
	 * Called inside the preview thread.  Should be as fast as possible.  All expensive computations should be pushed
	 * into their own thread.
	 *
	 * @param bytes Data from preview image
	 * @param camera Reference to camera
	 */
	public void convertPreview( byte[] bytes, Camera camera );

	/**
	 * Blocks until all processing has stopped
	 */
	public void stopProcessing();

}
