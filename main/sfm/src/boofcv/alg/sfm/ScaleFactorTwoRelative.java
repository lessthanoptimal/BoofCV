package boofcv.alg.sfm;

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

/**
 * Resolves the unknown scale factor when given a known stereo baseline and the relationship between the
 * current view and a the left and right cameras at the keyframe.  The latter is known only up to a scale factor.
 * The solution is solved for using the law of sine and the problem breaks down to a angle-side-angle triangle.
 *
 * TODO Does accuracy degrade as the three views become linear?
 *
 * @author Peter Abeles
 */
public class ScaleFactorTwoRelative {

	// known stereo baseline
	private Se3_F64 keyLeftToRight = new Se3_F64();
	private Se3_F64 keyRightToLeft = new Se3_F64();
	private double baseline;

	// storage for the inverse transformation of an input
	private Se3_F64 currToKeyRight = new Se3_F64();
	private Se3_F64 currToKeyLeft = new Se3_F64();

	// storage for different vector direction hypotheses
	Se3_F64 vL2C = new Se3_F64();
	Se3_F64 vR2C = new Se3_F64();
	Se3_F64 vC2R = new Se3_F64();
	Se3_F64 vC2L = new Se3_F64();

	public ScaleFactorTwoRelative(Se3_F64 keyRightToLeft) {
		this.keyRightToLeft.set(keyRightToLeft);
		keyRightToLeft.invert(keyLeftToRight);
		baseline = keyRightToLeft.getT().norm();
	}

	/**
	 * Computes the unknown scale factor and applies it to leftKeyToCurr.
	 *
	 * @param leftKeyToCurr Input and Output. Transform from left keyframe to the current frame in the left camera.
	 *                      scale is adjusted on output.
	 * @param rightKeyToCurr Input: Transform from right keyframe to the current frame in the left camera.
	 * @return true if the geometry allowed for the scale to be resolved.
	 */
	public boolean computeScaleFactor( Se3_F64 leftKeyToCurr ,
									   Se3_F64 rightKeyToCurr )
	{
		double lengthL2C = leftKeyToCurr.getT().norm();

		// If the length is zero there is no scale to resolve
		if( lengthL2C == 0 )
			return true;

		rightKeyToCurr.invert(currToKeyRight);
		leftKeyToCurr.invert(currToKeyLeft);


		double bestError = Double.MAX_VALUE;
		double bestAdjustment = 0;

		Point3D_F64 testA = new Point3D_F64();
		Point3D_F64 testB = new Point3D_F64();

		// there are four possible combinations of signs
		for( int i = 0; i < 4; i++ ) {
			vL2C.set(leftKeyToCurr);
			vR2C.set(rightKeyToCurr);
			vC2R.set(currToKeyRight);
			vC2L.set(currToKeyLeft);

			if( i % 2 == 1 ) {
				GeometryMath_F64.changeSign(vL2C.getT());
				GeometryMath_F64.changeSign(vC2L.getT());
			}
			if( i >= 2 ) {
				GeometryMath_F64.changeSign(vR2C.getT());
				GeometryMath_F64.changeSign(vC2R.getT());
			}

			double angleC = computeAngle(vL2C.getT(),vR2C.getT());
			double angleB = computeAngle(vC2R.getT(), keyLeftToRight.getT());
			double angleA = computeAngle(vC2L.getT(),keyRightToLeft.getT());

			double sineC = Math.sin(angleC);

			if( sineC == 0 )
				return false;

			double lengthAC = baseline*Math.sin(angleB)/sineC;
			double lengthBC = baseline*Math.sin(angleA)/sineC;

			GeometryMath_F64.scale(vL2C.getT(),lengthAC/vL2C.getT().norm());
			GeometryMath_F64.scale(vR2C.getT(),lengthBC/vR2C.getT().norm());

			testA.set(0,0,0);
			testB.set(0,0,0);

			SePointOps_F64.transform(vL2C,testA,testA);
			SePointOps_F64.transform(keyLeftToRight,testB,testB);
			SePointOps_F64.transform(vR2C,testB,testB);

			double d = testA.distance(testB);

			System.out.println("d = "+d);

			System.out.println("L2C = "+vL2C.getT());
			System.out.println("R2C = "+vR2C.getT());

			if( d < bestError ) {
				bestError = d;
				bestAdjustment = lengthAC/lengthL2C;

				if( i % 2 == 1 )
					bestAdjustment *= -1;
			}
		}

		GeometryMath_F64.scale(leftKeyToCurr.getT(),bestAdjustment);

		System.out.println("  final length "+leftKeyToCurr.getT().norm());

		return true;
	}

	/**
	 * Compute the angle using the dot product.
	 */
	private double computeAngle( Vector3D_F64 a , Vector3D_F64 b ) {
		double top = a.dot(b);

		double bottom = a.norm()*b.norm();

		return Math.acos(top / bottom);
	}
}
