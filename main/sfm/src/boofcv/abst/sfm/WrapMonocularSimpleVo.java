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
	public Se3_F64 getCameraToWorld() {
		return alg.getCameraLocation();
	}
}
