package boofcv.alg.feature.describe;


import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F64;

/**
 * @author Peter Abeles
 */
public class TestSurfDescribeOps_feature extends StandardSurfTests{

	Kernel2D_F64 weightSurf = FactoryKernelGaussian.gaussianWidth(-1, 20);

	@Override
	protected void describe(double x, double y, double yaw, double scale, double[] features) {
		SurfDescribeOps.features(x,y,yaw, scale, weightSurf,4,5, sparse,features);
	}
}
