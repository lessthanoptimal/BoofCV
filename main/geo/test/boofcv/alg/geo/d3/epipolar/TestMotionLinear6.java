package boofcv.alg.geo.d3.epipolar;

import boofcv.alg.geo.AssociatedPair;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class TestMotionLinear6 extends CommonMotionNPoint {

	/**
	 * Standard test using only the minimum number of observation
	 */
	@Test
	public void minimalObservationTest() {
		standardTest(6);
	}

	/**
	 * Standard test with an over determined system
	 */
	@Test
	public void overdetermined() {
		standardTest(20);
	}

	@Override
	public Se3_F64 compute(List<AssociatedPair> obs, List<Point3D_F64> locations) {
		MotionLinear6 alg = new MotionLinear6();

		alg.process(obs,locations);

		return alg.getMotion();
	}
}
