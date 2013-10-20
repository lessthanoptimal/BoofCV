package boofcv.alg.tracker.meanshift;

import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.struct.RectangleRotate_F32;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestLocalWeightedHistogramRotRect {

	@Test
	public void computeWeights() {
		fail("Implement");
	}

	@Test
	public void createSamplePoints() {
		fail("Implement");
	}

	@Test
	public void computeHistogram() {
		fail("Implement");

		// multiple calls should produce the same solution as a single call
	}


	/**
	 * Should produce the same as the border case when they are given the same solution inside
	 */
	@Test
	public void computeHistogramInside() {
		fail("Implement");
	}

	/**
	 * Test it when the region is entirely inside the image
	 */
	@Test
	public void computeHistogramBorder_inside() {
		fail("Implement");
	}

	/**
	 * Make sure it is skipping pixels outside the image
	 */
	@Test
	public void computeHistogramBorder_outside() {
		fail("Implement");
	}

	@Test
	public void computeHistogramBin() {
		fail("implement");
	}

	@Test
	public void isInFastBounds() {
		DummyInterpolate interp = new DummyInterpolate();
		RectangleRotate_F32 rect = new RectangleRotate_F32(4,5,10,20,0);
		LocalWeightedHistogramRotRect alg = new LocalWeightedHistogramRotRect(10,3,12,3,255,interp);

		alg.c = 1; alg.s = 0;
		assertTrue(alg.isInFastBounds(rect));

		// see if it checked to see if the four corners are in bounds
		assertEquals(4,interp.list.size());
		Point2D_F32 p0 = interp.list.get(0);
		Point2D_F32 p1 = interp.list.get(1);
		Point2D_F32 p2 = interp.list.get(2);
		Point2D_F32 p3 = interp.list.get(3);

		// the order really doesn't matter, but easier to code the test this way
		assertEquals(4f-5f,p0.x,1e-4f);
		assertEquals(5f-10f,p0.y,1e-4f);

		assertEquals(4f-5f,p1.x,1e-4f);
		assertEquals(5f+10f,p1.y,1e-4f);

		assertEquals(4f+5f,p2.x,1e-4f);
		assertEquals(5f+10f,p2.y,1e-4f);

		assertEquals(4f+5f,p3.x,1e-4f);
		assertEquals(5f-10f,p3.y,1e-4f);
	}

	@Test
	public void normalizeHistogram() {
		LocalWeightedHistogramRotRect alg = new LocalWeightedHistogramRotRect(10,3,12,3,255,null);

		float total = 0;
		for( int i = 0; i < alg.histogram.length; i++ ) {
			total += alg.histogram[i] = i+1;
		}
		float expected[] = alg.histogram.clone();

		alg.normalizeHistogram();

		for( int i = 0; i < alg.histogram.length; i++ ) {
			assertEquals( expected[i]/total,alg.histogram[i],1e-4);
		}
	}

	@Test
	public void squareToImage() {
		RectangleRotate_F32 rect = new RectangleRotate_F32(4,5,10,20,0);
		LocalWeightedHistogramRotRect alg = new LocalWeightedHistogramRotRect(10,3,12,3,255,null);

		alg.c = 1; alg.s = 0;
		alg.squareToImage(0,0,rect);

		assertEquals(4,alg.imageX,1e-4f);
		assertEquals(5,alg.imageY,1e-4f);

		alg.squareToImage(-0.5f,0.5f,rect);

		assertEquals(4f-5f,alg.imageX,1e-4f);
		assertEquals(5f+10f,alg.imageY,1e-4f);

		alg.c = 0.5f; alg.s = -0.5f;
		alg.squareToImage(-0.5f,0.5f,rect);

		assertEquals(4f+2.5f-5f,alg.imageX,1e-4f);
		assertEquals(5f+5f-2.5f,alg.imageY,1e-4f);
	}

	static class DummyInterpolate implements InterpolatePixelMB {

		List<Point2D_F32> list = new ArrayList<Point2D_F32>();

		@Override
		public void setImage(ImageBase image) {}

		@Override
		public ImageBase getImage() {
			return null;
		}

		@Override
		public boolean isInFastBounds(float x, float y) {
			list.add( new Point2D_F32(x,y));
			return true;
		}

		@Override
		public int getFastBorderX() {
			return 0;
		}

		@Override
		public int getFastBorderY() {
			return 0;
		}

		@Override
		public void get(float x, float y, float[] values) {}

		@Override
		public void get_fast(float x, float y, float[] values) {}
	}

}
