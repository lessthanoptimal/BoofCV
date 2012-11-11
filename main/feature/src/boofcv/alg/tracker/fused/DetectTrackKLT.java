package boofcv.alg.tracker.fused;

import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.alg.tracker.klt.KltTrackFault;
import boofcv.alg.tracker.klt.KltTracker;
import boofcv.alg.tracker.pklt.GenericPkltFeatSelector;
import boofcv.alg.tracker.pklt.PkltManagerConfig;
import boofcv.alg.tracker.pklt.PyramidKltFeature;
import boofcv.alg.tracker.pklt.PyramidKltTracker;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.ImagePyramid;

/**
 * @author Peter Abeles
 */
public class DetectTrackKLT<I extends ImageSingleBand, D extends ImageSingleBand> {
	/** configuration for low level KLT tracker */
	public KltConfig config;

	/** The radius of each feature. 3 is a reasonable number. */
	public int featureRadius;

	/** Scale factor for each layer in the pyramid */
	public int pyramidScaling[];

	// the tracker
	protected PyramidKltTracker<I, D> tracker;

	public DetectTrackKLT(KltConfig config,
						  int featureRadius,
						  int[] pyramidScaling,
						  Class<I> inputType, Class<D> derivType) {
		this.config = config;
		this.featureRadius = featureRadius;
		this.pyramidScaling = pyramidScaling;

		InterpolateRectangle<I> interpInput = FactoryInterpolation.<I>bilinearRectangle(inputType);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.<D>bilinearRectangle(derivType);

		KltTracker<I, D> klt = new KltTracker<I, D>(interpInput,interpDeriv,config);
		tracker = new PyramidKltTracker<I,D>(klt);
	}


	public PyramidKltFeature setDescription( float x , float y , PyramidKltFeature ret ) {
		if( ret == null )
			ret = new PyramidKltFeature(pyramidScaling.length, featureRadius);
		ret.setPosition(x,y);
		tracker.setDescription(ret);

		return ret;
	}

	public void setInputs( ImagePyramid<I> image , ImagePyramid<D> derivX , ImagePyramid<D> derivY ) {
		tracker.setImage(image,derivX,derivY);
	}

	/**
	 * Updates the track using the latest inputs.  If tracking fails then the feature description
	 * in each layer is unchanged and its global position.
	 *
	 * @param feature Feature being updated
	 * @return true if tracking was successful, false otherwise
	 */
	public boolean performTracking(  PyramidKltFeature feature ) {

		KltTrackFault result = tracker.track(feature);

		if( result != KltTrackFault.SUCCESS ) {
			return false;
		} else {
			tracker.setDescription(feature);
			return true;
		}
	}
}
