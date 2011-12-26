package boofcv.alg.feature.detect.calibgrid;

import georegression.struct.line.LineParametric2D_F64;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestIntersectLinesLinear {

	/**
	 * Trivial case with two perfect observations
	 */
	@Test
	public void trivial() {
		IntersectLinesLinear alg = new IntersectLinesLinear();
		
		List<LineParametric2D_F64> lines = new ArrayList<LineParametric2D_F64>();
		lines.add( new LineParametric2D_F64(10,0,1,0));
		lines.add( new LineParametric2D_F64(1,10,0,1));

		assertTrue(alg.process(lines));

		Point2D_F64 found = alg.getPoint();
		assertEquals(1,found.x,1e-8);
		assertEquals(0,found.y,1e-8);
	}

	/**
	 * Provide two points which should completely dominate the solution with one noise point
	 * which should be virtually ignored
	 */
	@Test
	public void weight() {
		IntersectLinesLinear alg = new IntersectLinesLinear();

		List<LineParametric2D_F64> lines = new ArrayList<LineParametric2D_F64>();
		lines.add(new LineParametric2D_F64(10, 0, 10000, 0));
		lines.add(new LineParametric2D_F64(1, 10, 0, 10000));
		lines.add( new LineParametric2D_F64(2,10,0,0.01));

		assertTrue(alg.process(lines));

		Point2D_F64 found = alg.getPoint();
		assertEquals(1,found.x,0.01);
		assertEquals(0,found.y,0.01);
	}

	/**
	 * Provide redundant but perfect observations
	 */
	@Test
	public void redundant() {
		IntersectLinesLinear alg = new IntersectLinesLinear();

		List<LineParametric2D_F64> lines = new ArrayList<LineParametric2D_F64>();
		lines.add(new LineParametric2D_F64(10, 0, 1, 0));
		lines.add(new LineParametric2D_F64(1, 10, 0, 1));
		lines.add( new LineParametric2D_F64(10,0,-1,0));
		lines.add( new LineParametric2D_F64(1,10,0,-1));
		lines.add( new LineParametric2D_F64(5,0,1,0));
		lines.add( new LineParametric2D_F64(1,6,0,1));

		assertTrue(alg.process(lines));

		Point2D_F64 found = alg.getPoint();
		assertEquals(1,found.x,1e-8);
		assertEquals(0,found.y,1e-8);
	}
}
