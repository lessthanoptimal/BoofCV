package boofcv.abst.sfm;

import boofcv.abst.feature.tracker.ModelAssistedTracker;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageBase;

/**
 * Extension to {@Link ModelAssistedTracker} which allows camera calibration information to be passed to the tracker.
 * Useful for applications where the camera's calibration can change.
 *
 * @author Peter Abeles
 */
public interface ModelAssistedTrackerCalibrated<T extends ImageBase,Model,Info>
		extends ModelAssistedTracker<T,Model,Info>
{

	public void setCalibration( IntrinsicParameters param );
}
