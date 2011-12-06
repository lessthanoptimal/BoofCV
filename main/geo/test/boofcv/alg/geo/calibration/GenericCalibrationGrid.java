package boofcv.alg.geo.calibration;

import boofcv.alg.calibration.CalibrationGridConfig;
import boofcv.alg.geo.d3.epipolar.UtilEpipolar;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;

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
	 * Creates several random uncalibrated homographies
	 * @param K Calibration matrix
	 * @param N Number of homographies
	 */
	public static List<DenseMatrix64F> createHomographies( DenseMatrix64F K , int N , Random rand ) {
		List<Se3_F64> motions = new ArrayList<Se3_F64>();

		for( int i = 0; i < N; i++ ) {
			double x = rand.nextGaussian()*200;
			double y = rand.nextGaussian()*200;
			double z = rand.nextGaussian()*50-1000;

			double rotX = (rand.nextDouble()-0.5)*0.1;
			double rotY = (rand.nextDouble()-0.5)*0.1;
			double rotZ = (rand.nextDouble()-0.5)*0.1;

			motions.add( SpecialEuclideanOps_F64.setEulerXYZ(x, y, z, rotX, rotY, rotZ, null));
		}

		List<DenseMatrix64F> homographies;
		homographies = new ArrayList<DenseMatrix64F>();
		for( Se3_F64 se : motions ) {
			DenseMatrix64F H = UtilEpipolar.computeHomography(se.getR(), se.getT(), 1000, new Vector3D_F64(0, 0, 1), K);

			homographies.add(H);
		}

		return homographies;

	}
}
