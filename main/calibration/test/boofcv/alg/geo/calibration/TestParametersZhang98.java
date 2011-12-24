package boofcv.alg.geo.calibration;

import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestParametersZhang98 {

	Random rand = new Random(234);

	/**
	 * Test to see if the conversion to and from a parameter array works well.
	 */
	@Test
	public void toAndFromParametersArray() {
		ParametersZhang98 p = new ParametersZhang98(3,2);

		p.a = 2;p.b=3;p.c=4;p.x0=5;p.y0=6;
		p.distortion = new double[]{1,2,3};
		for( int i = 0; i < 2; i++ ) {
			ParametersZhang98.View v = p.views[i];
			v.T.set(rand.nextDouble(),rand.nextDouble(),rand.nextDouble());
			v.rotation.theta = rand.nextDouble();
			v.rotation.unitAxisRotation.set(rand.nextDouble(),rand.nextDouble(),rand.nextDouble());
		}

		// convert it into array format
		double array[] = new double[ p.size() ];
		p.convertToParam(array);

		// create a new set of parameters and assign its value from the array
		ParametersZhang98 found = new ParametersZhang98(3,2);
		found.setFromParam(array);

		// compare the two sets of parameters
		TestCalibrationPlanarGridZhang98.checkEquals(p,found);
	}
}
