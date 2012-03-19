package boofcv.abst.sfm;

import boofcv.alg.sfm.MonocularSeparatedMotion;
import boofcv.struct.image.ImageBase;
import georegression.struct.se.Se3_F64;

/**
 * Wrapper around {@link boofcv.alg.sfm.MonocularSimpleVo} for {@link boofcv.abst.sfm.MonocularVisualOdometry}.
 *
 * @author Peter Abeles
 */
public class WrapMonocularSeparatedMotion<T extends ImageBase>
		implements MonocularVisualOdometry<T>{

	MonocularSeparatedMotion<T> alg;

	Se3_F64 c2w = new Se3_F64();

	public WrapMonocularSeparatedMotion(MonocularSeparatedMotion<T> alg) {
		this.alg = alg;
	}

	public MonocularSeparatedMotion<T> getAlg() {
		return alg;
	}

	@Override
	public boolean process(T input) {
		return alg.process(input);
	}

	@Override
	public void reset() {
		throw new RuntimeException("Implement");
	}

	@Override
	public boolean isFatal() {
		return false;
	}

	@Override
	public Se3_F64 getCameraToWorld() {
		Se3_F64 w2c = alg.getWorldToKey();
		w2c.invert(c2w);
		return c2w;
	}
}
