package boofcv.alg.sfm;

import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestComputeObservationAcuteAngle {

	@Test
	public void simpleNoRotation() {
		ComputeObservationAcuteAngle alg = new ComputeObservationAcuteAngle();

		Se3_F64 fromAtoB = new Se3_F64();
		fromAtoB.getT().set(-2,0,0);

		Point2D_F64 a = new Point2D_F64(0,0);
		Point2D_F64 b = new Point2D_F64(0,0);

		alg.setFromAtoB(fromAtoB);

		assertEquals(0, alg.computeAcuteAngle(a, b), 1e-8);

		b.set(-1,0);

		assertEquals(Math.PI/4.0,alg.computeAcuteAngle(a,b),1e-8);
	}

	@Test
	public void simpleWithRotation() {
		ComputeObservationAcuteAngle alg = new ComputeObservationAcuteAngle();

		Se3_F64 fromAtoB = new Se3_F64();
		fromAtoB.getT().set(-2,0,0);
		RotationMatrixGenerator.eulerXYZ(0,-Math.PI/4.0,0,fromAtoB.getR());

		Point2D_F64 a = new Point2D_F64(0,0);
		Point2D_F64 b = new Point2D_F64(0,0);

		alg.setFromAtoB(fromAtoB);

		assertEquals(Math.PI/4.0,alg.computeAcuteAngle(a,b),1e-8);
	}
}
