package boofcv.abst.tracker;

import boofcv.struct.image.ImageBase;
import georegression.struct.shapes.RectangleCorner2D_F64;

/**
 * High level interface for an object tracker where the object being tracked is specified using a rectangle. The
 * input is assumed to be a sequence of consecutive video images. When initialize is called the tracker is put
 * into its initial state ago and its past history is discarded.
 *
 * @author Peter Abeles
 */
public interface TrackerObjectRectangle<T extends ImageBase> {

	/**
	 * Initializes tracking by specifying the object's location using a rectangle. Some implementations can
	 * fail if there is insufficient visual information for it to track.  All previous tracking information
	 * is discarded when this function is called.
	 *
	 * @param image Initial image in the sequence
	 * @param x0 Top-left corner of rectangle. x-axis
	 * @param y0 Top-left corner of rectangle. y-axis
	 * @param x1 Bottom-right corner of rectangle. x-axis
	 * @param y1 Bottom-right corner of rectangle. y-axis
	 * @return true if successful and false if not.
	 */
	public boolean initialize( T image , int x0 , int y0 , int x1 , int y1 );

	/**
	 * Updates the tracks location using the latest video frame.  {@link #initialize(boofcv.struct.image.ImageBase, int, int, int, int)}
	 * must be called once before this function can be called.
	 *
	 * @param image The next image in the video sequence.
	 * @param location The new location of the object being tracked.
	 * @return true if the target was found and 'location' updated.
	 */
	public boolean process( T image , RectangleCorner2D_F64 location );
}
