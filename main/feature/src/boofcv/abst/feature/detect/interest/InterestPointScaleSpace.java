package boofcv.abst.feature.detect.interest;

import boofcv.struct.feature.ScalePoint;
import boofcv.struct.gss.GaussianScaleSpace;
import boofcv.struct.image.ImageBase;

import java.util.List;


/**
 * Interest point detector for {@link boofcv.struct.gss.GaussianScaleSpace Scale Space} images.
 *
 * @author Peter Abeles
 */
public interface InterestPointScaleSpace<T extends ImageBase, D extends ImageBase> {

	/**
	 * Detect features in the scale space image
	 *
	 * @param ss Scale space of an image
	 */
	public void detect( GaussianScaleSpace<T,D> ss );

	/**
	 * Returns all the found interest points
	 *
	 * @return List of found interest points.
	 */
	public List<ScalePoint> getInterestPoints();
}
