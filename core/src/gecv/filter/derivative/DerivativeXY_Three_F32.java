package gecv.filter.derivative;

import gecv.alg.filter.derivative.GradientThree;


/**
 * @author Peter Abeles
 */
public class DerivativeXY_Three_F32 extends DerivativeXYBase_F32 {

	@Override
	public void process() {
		GradientThree.derivX_F32(image, derivX);
		GradientThree.derivY_F32(image, derivY);
	}

	@Override
	public int getBorder() {
		return 1;
	}
}