package boofcv.abst.sfm;

import boofcv.alg.sfm.AssociateStereoPoint;
import boofcv.alg.sfm.StereoVoEpipolar;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.se.Se3_F64;

/**
 * @author Peter Abeles
 */
public class WrapStereoVoEpipolar<T extends ImageSingleBand>
		implements StereoVisualOdometry<T> {

	StereoVoEpipolar<T> alg;
	AssociateStereoPoint<T> stereoAssociate;

	public WrapStereoVoEpipolar(StereoVoEpipolar<T> alg , AssociateStereoPoint<T> stereoAssociate) {
		this.alg = alg;
		this.stereoAssociate = stereoAssociate;
	}

	@Override
	public boolean process(T leftImage, T rightImage) {
		stereoAssociate.setImages(leftImage,rightImage);
		return alg.process(leftImage,rightImage);
	}

	@Override
	public void reset() {
		alg.reset();
	}

	@Override
	public boolean isFatal() {
		return false;
	}

	@Override
	public Se3_F64 getCameraToWorld() {
		return alg.getCurrToWorld();
	}
}
