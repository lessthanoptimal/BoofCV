package boofcv.alg.geo.d2;


import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.DistanceFromModel;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homo.HomographyPointOps;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestDistanceHomographySq extends StandardDistanceTest<Homography2D_F64, AssociatedPair> {

	Random rand = new Random(234);

	@Override
	public DistanceFromModel<Homography2D_F64, AssociatedPair> create() {
		return new DistanceHomographySq();
	}

	@Override
	public Homography2D_F64 createRandomModel() {
		Homography2D_F64 h = new Homography2D_F64();

		h.a11 = rand.nextDouble()*5;
		h.a12 = rand.nextDouble()*5;
		h.a13 = rand.nextDouble()*5;
		h.a21 = rand.nextDouble()*5;
		h.a22 = rand.nextDouble()*5;
		h.a23 = rand.nextDouble()*5;
		h.a31 = rand.nextDouble()*5;
		h.a32 = rand.nextDouble()*5;
		h.a33 = rand.nextDouble()*5;

		return h;
	}

	@Override
	public AssociatedPair createRandomData() {
		Point2D_F64 p1 = new Point2D_F64(rand.nextGaussian(),rand.nextGaussian());
		Point2D_F64 p2 = new Point2D_F64(rand.nextGaussian(),rand.nextGaussian());

		return new AssociatedPair(p1,p2,false);
	}

	@Override
	public double distance(Homography2D_F64 h, AssociatedPair associatedPair) {

		Point2D_F64 result = new Point2D_F64();

		HomographyPointOps.transform(h,associatedPair.keyLoc,result);
		return result.distance2(associatedPair.currLoc);
	}
}
