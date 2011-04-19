package gecv.filter.derivative;

import gecv.alg.filter.convolve.ConvolveImage;
import gecv.alg.filter.convolve.KernelFactory;
import gecv.struct.convolve.Kernel1D_F32;


/**
 * Finds the derivative using a Gaussian kernel.  This is the same as convolving the image
 * and then computing the derivative
 *
 * @author Peter Abeles
 */
public class DerivativeXY_Gaussian_F32 extends DerivativeXYBase_F32 {

	private Kernel1D_F32 deriv;

	public DerivativeXY_Gaussian_F32(double sigma, int radius) {
		deriv = KernelFactory.gaussianDerivative1D_F32(sigma, radius, true);
	}

	public DerivativeXY_Gaussian_F32(int radius) {
		deriv = KernelFactory.gaussianDerivative1D_F32((2 * radius + 1) / 5.0, radius, true);
	}


	@Override
	public void process() {
		ConvolveImage.horizontal(deriv, image, derivX, true);
		ConvolveImage.vertical(deriv, image, derivY, false);
	}

	@Override
	public int getBorder() {
		return deriv.getRadius();
	}
}