package boofcv.alg.feature.orientation.impl;

import boofcv.alg.feature.orientation.GenericOrientationIntegralTests;
import boofcv.alg.feature.orientation.OrientationIntegralBase;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;


/**
 * @author Peter Abeles
 */
public class TestImplOrientationSlidingWindowIntegral_F32 {
	double angleTol = Math.PI/9;
	int r = 3;

	@Test
	public void standardUnweighted() {
		GenericOrientationIntegralTests<ImageFloat32> tests = new GenericOrientationIntegralTests<ImageFloat32>();

		OrientationIntegralBase<ImageFloat32> alg = new ImplOrientationSlidingWindowIntegral_F32(20,Math.PI/3,r,false, 4);

		// region samples is r*2 +1 + sampleRadius
		tests.setup(angleTol, r*2+3 , alg,ImageFloat32.class);
		tests.performAll();
	}

	@Test
	public void standardWeighted() {
		GenericOrientationIntegralTests<ImageFloat32> tests = new GenericOrientationIntegralTests<ImageFloat32>();

		OrientationIntegralBase<ImageFloat32> alg = new ImplOrientationSlidingWindowIntegral_F32(20,Math.PI/3,r,true, 4);

		tests.setup(angleTol, r*2+3 ,alg,ImageFloat32.class);
		tests.performAll();
	}
}
