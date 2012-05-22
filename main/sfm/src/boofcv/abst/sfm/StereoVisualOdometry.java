package boofcv.abst.sfm;

import boofcv.struct.image.ImageBase;

/**
 * TODO write
 * 
 * @author Peter Abeles
 */
public interface StereoVisualOdometry<T extends ImageBase> extends VisualOdometry{

	/**
	 * TODO Update
	 * Process the new image and update the motion estimate.  The return value must be checked
	 * to see if the estimate was actually updated.  If false is returned then {@link #isFatal}
	 * also needs to be checked to see if the pose estimate has been reset.
	 *
	 * @return If the motion estimate has been updated or not
	 */
	public boolean process(T leftImage , T rightImage );
}
