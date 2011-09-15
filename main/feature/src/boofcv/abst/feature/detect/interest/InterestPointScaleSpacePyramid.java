package boofcv.abst.feature.detect.interest;

import boofcv.alg.transform.gss.ScaleSpacePyramid;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageBase;

import java.util.List;


/**
 * Interest point detector for {@link ScaleSpacePyramid Scale Space Pyramid} images.
 *
 * @author Peter Abeles
 */
public interface InterestPointScaleSpacePyramid<T extends ImageBase> {

	/**
	 * Detect features in the scale space image
	 *
	 * @param ss Scale space of an image
	 */
	public void detect( ScaleSpacePyramid<T> ss );

	/**
	 * Returns all the found interest points
	 *
	 * @return List of found interest points.
	 */
	public List<ScalePoint> getInterestPoints();
}
