package boofcv.alg.feature.describe;


import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel2D_F64;

/**
 * @author Peter Abeles
 */
public class TestSurfDescribeOps_featureMod extends StandardSurfTests{

	Kernel2D_F64 weightLarge = FactoryKernelGaussian.gaussianWidth(2.5,4);
	Kernel2D_F64 weightSub = FactoryKernelGaussian.gaussianWidth(2.5,9);

	@Override
	protected void describe(double x, double y, double yaw, double scale, double[] features) {
		SurfDescribeOps.featuresMod(x,y,yaw, scale, weightLarge,weightSub,4,5, 2,sparse,features);
	}
}
