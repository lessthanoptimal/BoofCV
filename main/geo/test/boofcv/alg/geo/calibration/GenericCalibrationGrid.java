package boofcv.alg.geo.calibration;

import boofcv.alg.calibration.CalibrationGridConfig;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.CommonOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class GenericCalibrationGrid {

	public static CalibrationGridConfig createStandardConfig() {
		return new CalibrationGridConfig(5,6,30);
	}

	public static DenseMatrix64F createStandardCalibration() {
		DenseMatrix64F K = new DenseMatrix64F(3,3);

		double c_x = 200;
		double c_y = 220;
		double f = (c_x/Math.tan(Math.PI*30/180.0)); // 60 degree field of view

		double sx = 0.35/f;
		double sy = sx*0.9;
		double sk = 0.00001;

		K.set(0,0,f*sx);
		K.set(0,1,f*sk);
		K.set(0,2,c_x);
		K.set(1,1,f*sy);
		K.set(1,2,c_y);
		K.set(2,2,1);

		return K;
	}

	public static List<Point3D_F64> gridPoints3D( CalibrationGridConfig config )
	{
		List<Point3D_F64> ret = new ArrayList<Point3D_F64>();

		List<Point2D_F64> obs2D = config.computeGridPoints();

		for( Point2D_F64 p2 : obs2D ) {
			ret.add(new Point3D_F64(p2.x,p2.y,0));
		}

		return ret;
	}

	public static List<Point2D_F64> observations( Se3_F64 motion , CalibrationGridConfig config )
	{
		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		List<Point2D_F64> obs2D = config.computeGridPoints();

		for( Point2D_F64 p2 : obs2D ) {
			Point3D_F64 p3 = new Point3D_F64(p2.x,p2.y,0);

			Point3D_F64 t = SePointOps_F64.transform(motion,p3,null);

			ret.add( new Point2D_F64(t.x/t.z,t.y/t.z));
		}

		return ret;
	}

	public static List<Point2D_F64> observations( DenseMatrix64F H, CalibrationGridConfig config )
	{
		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		List<Point2D_F64> obs2D = config.computeGridPoints();

		for( Point2D_F64 p2 : obs2D ) {
			Point2D_F64 t = new Point2D_F64();

			GeometryMath_F64.mult(H, p2, t);

			ret.add( t );
		}

		return ret;
	}

	/**
	 * Creates a homography as defined in Section 2.2 in Zhang98.
	 *
	 * H = K*[r1 r2 t]
	 */
	public static DenseMatrix64F computeHomography(DenseMatrix64F K, DenseMatrix64F R, Vector3D_F64 T)
	{
		DenseMatrix64F M = new DenseMatrix64F(3,3);
		CommonOps.extract(R, 0, 3, 0, 1, M, 0, 0);
		CommonOps.extract(R, 0, 3, 1, 2, M, 0, 1);
		M.set(0, 2, T.x);
		M.set(1, 2, T.y);
		M.set(2, 2, T.z);

		DenseMatrix64F H = new DenseMatrix64F(3,3);
		CommonOps.mult(K,M,H);

		return H;
	}

	/**
	 * Creates several random uncalibrated homographies
	 * @param K Calibration matrix
	 * @param N Number of homographies
	 */
	public static List<DenseMatrix64F> createHomographies( DenseMatrix64F K , int N , Random rand ) {
		List<DenseMatrix64F> homographies = new ArrayList<DenseMatrix64F>();

		for( int i = 0; i < N; i++ ) {
			Vector3D_F64 T = new Vector3D_F64();
			T.x = rand.nextGaussian()*200;
			T.y = rand.nextGaussian()*200;
			T.z = rand.nextGaussian()*50-1000;

			double rotX = (rand.nextDouble()-0.5)*0.1;
			double rotY = (rand.nextDouble()-0.5)*0.1;
			double rotZ = (rand.nextDouble()-0.5)*0.1;

			DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(rotX, rotY, rotZ, null);

			DenseMatrix64F H = computeHomography(K, R, T);
			homographies.add(H);
		}

		return homographies;
	}
}
