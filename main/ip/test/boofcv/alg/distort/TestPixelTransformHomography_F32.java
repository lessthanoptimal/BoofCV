package boofcv.alg.distort;


import georegression.struct.homo.Homography2D_F32;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homo.HomographyPointOps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestPixelTransformHomography_F32 {
	@Test
	public void constructor_32() {
		Homography2D_F32 a = new Homography2D_F32(1,2,3,4,5,6,7,8,9);

		PixelTransformHomography_F32 alg = new PixelTransformHomography_F32();
		alg.set(a);

		alg.compute(2,3);
		Point2D_F32 p = new Point2D_F32(2,3);
		Point2D_F32 expected = new Point2D_F32();
		HomographyPointOps.transform(a, p, expected);

		assertEquals(expected.x,alg.distX,1e-4);
		assertEquals(expected.y,alg.distY,1e-4);
	}

	@Test
	public void constructor_64() {
		Homography2D_F64 a = new Homography2D_F64(1,2,3,4,5,6,7,8,9);

		PixelTransformHomography_F32 alg = new PixelTransformHomography_F32();
		alg.set(a);

		alg.compute(2,3);
		Point2D_F64 p = new Point2D_F64(2,3);
		Point2D_F64 expected = new Point2D_F64();
		HomographyPointOps.transform(a,p,expected);

		assertEquals(expected.x,alg.distX,1e-4);
		assertEquals(expected.y,alg.distY,1e-4);
	}
}
