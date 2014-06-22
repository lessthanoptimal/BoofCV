package boofcv.examples.stereo;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PointCloud {
	
	public static class Point {
		public Point3D_F64 point = new Point3D_F64();
		public int[] indices;
		
		public Map<Integer, Point2D_F64> points2d = new HashMap<Integer, Point2D_F64>();
		
		public Point( Point3D_F64 point, int numidx ) {
			super();
			this.point = point;
			indices = new int[ numidx ];
			for ( int i = 0; i < numidx; i++ ) {
				indices[i] = -1;
			}
		}
	}

	public List<Point> points = new ArrayList<Point>();

	public PointCloud() {
		super();
	}
}
