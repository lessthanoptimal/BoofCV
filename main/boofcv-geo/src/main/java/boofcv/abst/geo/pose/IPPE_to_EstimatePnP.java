package boofcv.abst.geo.pose;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.alg.geo.pose.PnPInfinitesimalPlanePoseEstimation;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.Point2D3D;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class IPPE_to_EstimatePnP implements Estimate1ofPnP {

	PnPInfinitesimalPlanePoseEstimation alg;

	FastQueue<AssociatedPair> pairs = new FastQueue<>(AssociatedPair.class,true);

	public IPPE_to_EstimatePnP(Estimate1ofEpipolar homography) {
		alg = new PnPInfinitesimalPlanePoseEstimation(homography);
	}

	@Override
	public boolean process(List<Point2D3D> points, Se3_F64 estimatedModel) {

		pairs.resize(points.size());

		for (int i = 0; i < pairs.size; i++) {
			AssociatedPair pair = pairs.get(i);
			Point2D3D p = points.get(i);

			if( p.location.z != 0 ) {
				throw new IllegalArgumentException("All points must lie on the x-y plane. If data is planar rotate it first");
			}

			pair.p1.set(p.location.x,p.location.y);
			pair.p2.set(p.observation);
		}

		if( !alg.process(pairs.toList()))
			return false;

		estimatedModel.set(alg.getWorldToCamera0());

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinimumPoints();
	}
}
