package gecv.filter.derivative;

import gecv.alg.filter.derivative.GradientSobel;


/**
 * @author Peter Abeles
 */
public class DerivativeXY_Sobel_F32 extends DerivativeXYBase_F32 {

	@Override
	public void process() {
		GradientSobel.process_F32(image, derivX, derivY);
	}

	@Override
	public int getBorder() {
		return 1;
	}
}
