package boofcv.alg.sfm;

import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestScaleFactorTwoRelative {

	Random rand = new Random(2344);

	@Test
	public void simpleKnown() {
		Se3_F64 l2c = new Se3_F64();
		l2c.getT().set(0,-2,0);
		RotationMatrixGenerator.eulerXYZ(0,Math.PI/2.0,0,l2c.getR());
		Se3_F64 r2l = new Se3_F64();
		r2l.getT().set(1,0,0);

		Se3_F64 r2c = r2l.concat(l2c,null);

		ScaleFactorTwoRelative alg = new ScaleFactorTwoRelative(r2l);
		assertTrue(alg.computeScaleFactor(l2c,r2c));

		assertEquals(2,l2c.getT().norm(),1e-8);
	}

	@Test
	public void randomTransforms() {
		for( int i = 0; i < 100; i++ ) {

			Se3_F64 l2c = createRandom();
			Se3_F64 r2l = createRandom();

			Se3_F64 r2c = r2l.concat(l2c,null);

			double solution = l2c.getT().norm();

			// scramble the scale factors.  Both positive and negative scales
			GeometryMath_F64.scale(l2c.getT(),(rand.nextDouble()-0.5)*4.0);
			GeometryMath_F64.scale(r2c.getT(),(rand.nextDouble()-0.5)*4.0);

			ScaleFactorTwoRelative alg = new ScaleFactorTwoRelative(r2l);

			assertTrue(alg.computeScaleFactor(l2c,r2c));

			assertEquals(solution,l2c.getT().norm(),1e-8);
		}
	}

	public Se3_F64 createRandom() {
		Se3_F64 ret = new Se3_F64();

		ret.T.x = (rand.nextDouble()-0.5)*3;
		ret.T.y = (rand.nextDouble()-0.5)*3;
		ret.T.z = (rand.nextDouble()-0.5)*3;

		double a = (rand.nextDouble()-0.5)*Math.PI;
		double b = (rand.nextDouble()-0.5)*Math.PI;
		double c = (rand.nextDouble()-0.5)*Math.PI;

		RotationMatrixGenerator.eulerXYZ(a,b,c,ret.R);

		return ret;
	}
}
