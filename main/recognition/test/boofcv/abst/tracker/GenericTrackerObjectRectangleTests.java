package boofcv.abst.tracker;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Polygon2D_I32;
import georegression.struct.shapes.RectangleCorner2D_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericTrackerObjectRectangleTests {

	int width = 320;
	int height = 240;

	ImageUInt8 input = new ImageUInt8(width,height);
	RectangleCorner2D_F64 where = new RectangleCorner2D_F64();

	public abstract TrackerObjectRectangle<ImageUInt8> create( Class<ImageUInt8> imageType );

	@Test
	public void stationary() {
		TrackerObjectRectangle<ImageUInt8> tracker = create(ImageUInt8.class);
		render(1,0,0);
		assertTrue(tracker.initialize(input, 10, 20, 110, 160));
		assertTrue(tracker.process(input, where));

		assertEquals(10, where.x0, 1e-8);
		assertEquals(20, where.y0, 1e-8);
		assertEquals(110, where.x1, 1e-8);
		assertEquals(160, where.y1, 1e-8);
	}

	@Test
	public void translation_small() {
		TrackerObjectRectangle<ImageUInt8> tracker = create(ImageUInt8.class);
		render(1,0,0);
		assertTrue(tracker.initialize(input, 20, 25, 120, 160));

		int tranX =  3;
		int tranY = -3;

		render(1,tranX,tranY);
		assertTrue(tracker.process(input, where));

		checkSolution(20+tranX,25+tranY,120+tranX,160+tranY,0.05);
	}

	@Test
	public void translation_large() {
		TrackerObjectRectangle<ImageUInt8> tracker = create(ImageUInt8.class);
		render(1,0,0);
		assertTrue(tracker.initialize(input, 20, 25, 120, 160));

		int tranX =  20;
		int tranY =  30;

		render(1,tranX,tranY);
		assertTrue(tracker.process(input, where));

		checkSolution(20+tranX,25+tranY,120+tranX,160+tranY,0.05);
	}

	@Test
	public void zooming_in() {
		TrackerObjectRectangle<ImageUInt8> tracker = create(ImageUInt8.class);
		render(1,0,0);
		assertTrue(tracker.initialize(input, 20, 25, 120, 160));

		for( int i = 0; i < 10; i++ ) {
			double scale = 1 - 0.2*(i/9.0);
//			System.out.println("scale "+scale);

			render(scale,0,0);
			assertTrue(tracker.process(input, where));

			checkSolution(20*scale,25*scale,120*scale,160*scale,0.1);
		}
	}

	@Test
	public void zooming_out() {
		TrackerObjectRectangle<ImageUInt8> tracker = create(ImageUInt8.class);
		render(1,0,0);
		assertTrue(tracker.initialize(input, 20, 25, 120, 160));

		for( int i = 0; i < 10; i++ ) {
			double scale = 1 + 0.2*(i/9.0);
//			System.out.println("scale "+scale);

			render(scale,0,0);
			assertTrue(tracker.process(input, where));

			checkSolution(20*scale,25*scale,120*scale,160*scale,0.1);
		}
	}

	/**
	 * See if it correctly reinitializes.  Should produce identical results when given the same inputs after
	 * being reinitialized.
	 */
	@Test
	public void reinitialize() {
		RectangleCorner2D_F64 where1 = new RectangleCorner2D_F64();

		TrackerObjectRectangle<ImageUInt8> tracker = create(ImageUInt8.class);
		render(1,0,0);
		assertTrue(tracker.initialize(input, 20, 25, 120, 160));
		render(1,3,-3);
		assertTrue(tracker.process(input, where));
		render(1,6,-6);
		assertTrue(tracker.process(input, where));

		render(1,0,0);
		assertTrue(tracker.initialize(input, 20, 25, 120, 160));
		render(1,3,-3);
		assertTrue(tracker.process(input, where1));
		render(1,6,-6);
		assertTrue(tracker.process(input, where1));

		// Might not be a perfect match due to robust algorithm not being reset to their initial state
		checkSolution(where1.x0,where1.y0,where1.x1,where1.y1,0.02);
	}

	private void checkSolution( double x0 , double y0 , double x1 , double y1 , double fractionError ) {
//		System.out.println("Expected "+x0+" "+y0+" "+x1+" "+y1);
//		System.out.println("Actual "+where.x0+" "+where.y0+" "+where.x1+" "+where.y1);

		double tolX = (x1-x0)*fractionError;
		double tolY = (y1-y0)*fractionError;

		assertTrue(Math.abs(where.x0 - x0) <= tolX);
		assertTrue(Math.abs(where.y0 - y0) <= tolY);
		assertTrue(Math.abs(where.x1 - x1) <= tolX);
		assertTrue(Math.abs(where.y1 - y1) <= tolY);
	}

	private void render( double scale , int tranX , int tranY ) {
		Random rand = new Random(234);

		for( int i = 0; i < 500; i++ ) {

			int x = (int)(scale*rand.nextInt(width-10)) + tranX;
			int y = (int)(scale*rand.nextInt(height-10)) + tranY;
			int w = (int)(scale*rand.nextInt(100)+20);
			int h = (int)(scale*rand.nextInt(100)+20);

			Polygon2D_I32 p = new Polygon2D_I32(4);
			p.vertexes[0].set(x,y);
			p.vertexes[1].set(x+w,y);
			p.vertexes[2].set(x+w,y+h);
			p.vertexes[3].set(x,y+h);

			convexFill(p, input,rand.nextInt(255));
		}
	}


	private void convexFill( Polygon2D_I32 poly , ImageSingleBand image , double value ) {
		int minX = Integer.MAX_VALUE; int maxX = Integer.MIN_VALUE;
		int minY = Integer.MAX_VALUE; int maxY = Integer.MIN_VALUE;

		for( int i = 0; i < poly.vertexes.length; i++ ) {
			Point2D_I32 p = poly.vertexes[i];
			if( p.y < minY ) {
				minY = p.y;
			} else if( p.y > maxY ) {
				maxY = p.y;
			}
			if( p.x < minX ) {
				minX = p.x;
			} else if( p.x > maxX ) {
				maxX = p.x;
			}
		}
		ImageRectangle bounds = new ImageRectangle(minX,minY,maxX,maxY);
		BoofMiscOps.boundRectangleInside(image, bounds);

		Point2D_F64 p = new Point2D_F64();
		Polygon2D_F64 poly64 = new Polygon2D_F64(4);
		for( int i = 0; i < 4; i++ )
			poly64.vertexes[i].set( poly.vertexes[i].x , poly.vertexes[i].y );

		for( int y = bounds.y0; y < bounds.y1; y++ ) {
			p.y = y;
			for( int x = bounds.x0; x < bounds.x1; x++ ) {
				p.x = x;

				if( Intersection2D_F64.containConvex(poly64, p)) {
					GeneralizedImageOps.set(image, x, y, value);
				}
			}
		}
	}

}
