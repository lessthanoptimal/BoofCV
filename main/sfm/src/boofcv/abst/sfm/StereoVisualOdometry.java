package boofcv.abst.sfm;

import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageBase;

/**
 * TODO write
 * 
 * @author Peter Abeles
 */
public interface StereoVisualOdometry<T extends ImageBase> extends VisualOdometry{

	/**
	 * Specifies intrinsic and extrinsic parameters for the stereo camera system.
	 *
	 * @param parameters stereo calibration
	 */
	public void setCalibration( StereoParameters parameters );

	/**
	 * Forgets all past history and sets itself into its initial state.
	 */
	public void reset();

	/**
	 * TODO Update
	 * Process the new image and update the motion estimate.  The return value must be checked
	 * to see if the estimate was actually updated.  If false is returned then {@link #isFatal}
	 * also needs to be checked to see if the pose estimate has been reset.
	 *
	 * @return If the motion estimate has been updated or not
	 */
	public boolean process(T leftImage , T rightImage );

	/**
	 * True if a major fault has occurred and localization was lost. This value only needs to be
	 * checked when process() returns false.
	 *
	 * @return true if a major fault occurred.
	 */
	public boolean isFault();

	/**
	 * Type of input images it can process.
	 *
	 * @return The image type
	 */
	public Class<T> getImageType();

}
