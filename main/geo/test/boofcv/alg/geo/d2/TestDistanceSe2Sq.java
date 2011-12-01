package boofcv.alg.geo.d2;

import boofcv.alg.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDistanceSe2Sq {

	Random rand = new Random(234);

	@Test
	public void computeDistance() {
		Se2_F64 model = new Se2_F64(1,2,0);
		Point2D_F64 a = new Point2D_F64(-1,0.5);
		Point2D_F64 b = new Point2D_F64(0,2.5);
		Point2D_F64 c = new Point2D_F64(0.5,2.5);

		DistanceSe2Sq alg = new DistanceSe2Sq();
		alg.setModel(model);

		assertEquals(0, alg.computeDistance(new AssociatedPair(a, b, false)), 1e-6);
		assertEquals(0.25,alg.computeDistance(new AssociatedPair(a,c,false)),1e-6);
	}

	@Test
	public void computeDistance_list() {
		Se2_F64 model = new Se2_F64(1,2,0.1);

		List<AssociatedPair> pairs = new ArrayList<AssociatedPair>();
		for( int i = 0; i < 10; i++ ) {
			Point2D_F64 a = new Point2D_F64(rand.nextGaussian(),rand.nextGaussian());
			Point2D_F64 b = new Point2D_F64(rand.nextGaussian(),rand.nextGaussian());
			pairs.add( new AssociatedPair(a,b,true));
		}

		double found[] = new double[pairs.size()];

		DistanceSe2Sq alg = new DistanceSe2Sq();
		alg.setModel(model);

		alg.computeDistance(pairs,found);

		for( int i = 0; i < pairs.size(); i++ ) {
			double expected = alg.computeDistance(pairs.get(i));
			assertEquals(expected,found[i],1e-8);
		}
	}
}
