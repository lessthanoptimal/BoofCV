package boofcv.abst.sfm.d2;

import boofcv.abst.sfm.AccessPointTracks;
import boofcv.core.image.ConvertImage;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * Wrapper which converts a multi-spectral image into a gray scale image before computing its image motion.
 *
 * @author Peter Abeles
 */
public class MsToGrayMotion2D<T extends ImageSingleBand,IT extends InvertibleTransform>
	implements ImageMotion2D<MultiSpectral<T>,IT>, AccessPointTracks
{
	// motion estimation algorithm for a single band image
	ImageMotion2D<T,IT> motion;
	// if supposed, provides access to track points
	AccessPointTracks access;
	// storage for gray scale image
	T gray;

	public MsToGrayMotion2D( ImageMotion2D<T,IT> motion , Class<T> imageType ) {
		this.motion = motion;
		gray = GeneralizedImageOps.createSingleBand(imageType,1,1);
		if( motion instanceof AccessPointTracks ) {
			access = (AccessPointTracks)motion;
		}
	}

	@Override
	public boolean process(MultiSpectral<T> input) {
		gray.reshape(input.width,input.height);
		GConvertImage.average(input, gray);
		return motion.process(gray);
	}

	@Override
	public void reset() {
		motion.reset();
	}

	@Override
	public void setToFirst() {
		motion.setToFirst();
	}

	@Override
	public IT getFirstToCurrent() {
		return motion.getFirstToCurrent();
	}

	@Override
	public Class<IT> getTransformType() {
		return motion.getTransformType();
	}

	@Override
	public long getTrackId(int index) {
		return access.getTrackId(index);
	}

	@Override
	public List<Point2D_F64> getAllTracks() {
		return access.getAllTracks();
	}

	@Override
	public boolean isInlier(int index) {
		return access.isInlier(index);
	}

	@Override
	public boolean isNew(int index) {
		return access.isNew(index);
	}
}
