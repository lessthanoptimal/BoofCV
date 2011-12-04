package boofcv.alg.geo.calibration;

import boofcv.alg.calibration.CalibrationGridConfig;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

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
}
