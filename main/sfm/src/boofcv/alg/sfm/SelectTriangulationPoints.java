package boofcv.alg.sfm;

import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

/**
 * Select points with a favorable geometry for triangulation.
 *
 * @author Peter Abeles
 */
public class SelectTriangulationPoints {

	Se3_F64 fromAtoB;

	Point3D_F64 A = new Point3D_F64();
	Point3D_F64 B = new Point3D_F64();

	public void setFromAtoB(Se3_F64 fromAtoB) {
		this.fromAtoB = fromAtoB;
	}

	public double computeAcuteAngle( Point2D_F64 a , Point2D_F64 b ) {

		A.set(a.x,a.y,1);
		B.set(b.x,b.y,1);

		GeometryMath_F64.mult(fromAtoB.getR(),A,A);

		double dot = GeometryMath_F64.dot(A,B);
		return Math.acos( dot / (A.norm()*B.norm()));
	}
}
