package boofcv.alg.feature.line.gridline;

import boofcv.alg.feature.detect.line.gridline.Edgel;
import boofcv.alg.feature.detect.line.gridline.GridLineModelDistance;
import georegression.metric.UtilAngle;
import georegression.struct.line.LinePolar2D_F32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestGridLineModelDistance {

	/**
	 * Check to see that if two points exceed a max angle the distance is infinite
	 */
	@Test
	public void maxAngle() {
		LinePolar2D_F32 l = new LinePolar2D_F32((float)(Math.sqrt(2*5*5)),(float)(Math.PI/4.0));

		GridLineModelDistance alg = new GridLineModelDistance(0.2f);
		alg.setModel(l);

		// standard out of bounds
		assertEquals(Double.MAX_VALUE,alg.computeDistance(new Edgel(5,5,(float)(Math.PI/4.0)+0.3f)),1e-4);
		// standard in bounds
		assertEquals(0,alg.computeDistance(new Edgel(5,5,(float)(Math.PI/4.0)+0.1f)),1e-4);
		// see if it respects half angle
		assertEquals(0,alg.computeDistance(new Edgel(5,5, (float)UtilAngle.bound(Math.PI/4.0+Math.PI))),1e-4);
	}

	/**
	 * Assuming two points have an angle less than the max, check the distance
	 */
	@Test
	public void distance() {
		float theta = (float)(Math.PI/4.0);
		LinePolar2D_F32 l = new LinePolar2D_F32((float)(Math.sqrt(2*5*5)),theta);

		GridLineModelDistance alg = new GridLineModelDistance(0.2f);
		alg.setModel(l);

		assertEquals(0,alg.computeDistance(new Edgel(5,5, theta)),1e-4);
		assertEquals(7.0711, alg.computeDistance(new Edgel(0, 0, theta)), 0.1);
	}

	@Test
	public void computeDistance_list() {
		float theta = (float)(Math.PI/4.0);
		LinePolar2D_F32 l = new LinePolar2D_F32((float)(Math.sqrt(2*5*5)),theta);

		GridLineModelDistance alg = new GridLineModelDistance(0.2f);
		alg.setModel(l);

		double distance[] = new double[2];
		List<Edgel> points = new ArrayList<Edgel>();

		points.add(new Edgel(5,5, theta));
		points.add(new Edgel(0,0, theta));

		alg.computeDistance(points,distance);

		assertEquals(0,distance[0],1e-4);
		assertEquals(7.0711,distance[1],1e-4);
	}
}
