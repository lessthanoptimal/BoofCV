package boofcv.alg.tracker.fused;

import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Puts all active tracks and newly detected tracks onto a grid.  Adds new tracks to a list if they
 * are not too close to any other tracks.
 *
 * @author Peter Abeles
 */
public class EnforceUniquePoints {
	ImageUInt8 grid = new ImageUInt8(1,1);
	int radius;

	Map<Integer,List<Info>> info = new HashMap<Integer, List<Info>>();

	FastQueue<Point2D_F64> found = new FastQueue<Point2D_F64>(100,Point2D_F64.class,true);

	public EnforceUniquePoints(int radius) {
		this.radius = radius;
	}

	public void setInputSize( int width , int height ) {
		this.grid.reshape(width,height);
	}

	public void process( InterestPointDetector detector , List<Point2D_F64> activeTracks ) {
		GeneralizedImageOps.fill(grid, 0);
		info.clear();
		found.reset();

		for( Point2D_F64 p : activeTracks ) {
			grid.unsafe_set((int)p.x,(int)p.y,1);
		}

		for( int i = 0; i < detector.getNumberOfFeatures(); i++ ) {
			Point2D_F64 p = detector.getLocation(i);
			int x_c = (int)p.x;
			int y_c = (int)p.y;

			int value = grid.get(x_c,y_c);
			if( value == 2 ) {
				int where = grid.getIndex(x_c,y_c);

				info.get(where).add( new Info(detector.getScale(i) , detector.getOrientation(i)) );
			} else if( value == 0 ) {
				int where = grid.getIndex(x_c,y_c);

				List<Info> l = new ArrayList<Info>();
				l.add( new Info(detector.getScale(i) , detector.getOrientation(i)) );

				info.put(where,l);
				grid.unsafe_set(x_c, y_c, 2);
				found.grow().set(p);
			}
		}
	}

	public FastQueue<Point2D_F64> getFound() {
		return found;
	}

	public List<Info> getFeatureInfo( int x , int y ) {
		int where = grid.getIndex(x,y);
		return info.get(where);
	}

	public static class Info
	{
		public double orientation;
		public double scale;

		public Info(double scale, double orientation) {
			this.scale = scale;
			this.orientation = orientation;
		}
	}
}
