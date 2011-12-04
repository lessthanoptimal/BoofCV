package boofcv.alg.geo.calibration;

import boofcv.alg.calibration.CalibrationGridConfig;
import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCalibrationGridConfig {

	@Test
	public void computeGridPoints() {
		CalibrationGridConfig config = new CalibrationGridConfig(2,3,0.1);

		List<Point2D_F64> l = config.computeGridPoints();

		assertEquals(6,l.size());
		assertTrue(l.get(0).distance(new Point2D_F64(0,0))<=1e-8);
		assertTrue(l.get(1).distance(new Point2D_F64(0.1,0))<=1e-8);
		assertTrue(l.get(2).distance(new Point2D_F64(0,0.1))<=1e-8);
		assertTrue(l.get(3).distance(new Point2D_F64(0.1,0.1))<=1e-8);
		assertTrue(l.get(4).distance(new Point2D_F64(0,0.2))<=1e-8);
		assertTrue(l.get(5).distance(new Point2D_F64(0.1,0.2))<=1e-8);
	}
}
