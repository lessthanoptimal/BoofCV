package boofcv.abst.sfm;

import boofcv.alg.sfm.MonocularSimpleVo;
import boofcv.struct.image.ImageBase;
import georegression.struct.se.Se3_F64;

/**
 * Wrapper around {@link MonocularSimpleVo} for {@link MonocularVisualOdometry}.
 * 
 * @author Peter Abeles
 */
public class WrapMonocularSimpleVo<T extends ImageBase>
		implements MonocularVisualOdometry<T>{
	
	MonocularSimpleVo<T> alg;

	Se3_F64 c2w = new Se3_F64();

	public WrapMonocularSimpleVo(MonocularSimpleVo<T> alg) {
		this.alg = alg;
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
		return alg.isFatal();
	}

	@Override
	public Se3_F64 getLeftToWorld() {
		Se3_F64 w2c = alg.getWorldToCamera();
		w2c.invert(c2w);
		return c2w;
	}
}
