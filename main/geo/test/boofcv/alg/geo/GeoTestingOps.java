package boofcv.alg.geo;


import georegression.struct.point.Point3D_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Operations useful for unit tests
 *
 * @author Peter Abeles
 */
public class GeoTestingOps {

	public static List<Point3D_F64> randomPoints_F32( double minX , double maxX ,
													  double minY , double maxY ,
													  double minZ , double maxZ ,
													  int num , Random rand )
	{
		List<Point3D_F64> ret = new ArrayList<Point3D_F64>();

		for( int i = 0; i < num; i++ ) {
			double x = rand.nextDouble()*(maxX-minX)+minX;
			double y = rand.nextDouble()*(maxY-minY)+minY;
			double z = rand.nextDouble()*(maxZ-minZ)+minZ;

			ret.add(new Point3D_F64(x,y,z));
		}

		return ret;
	}
}
