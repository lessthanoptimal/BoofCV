package boofcv.abst.tracker;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.shapes.Quadrilateral_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericTrackerObjectRectangleTests<T extends ImageBase> {

	int width = 320;
	int height = 240;

	ImageType<T> imageType;
	protected T input;
	Quadrilateral_F64 where = new Quadrilateral_F64();

	// tolerances for different tests
	protected double tolTranslateSmall = 0.02;
	// tolerance for scale changes
	protected double tolScale = 0.1;

	// the initial location of the target in the image
	protected Quadrilateral_F64 initRegion = rect(20,25,120,160);

	protected GenericTrackerObjectRectangleTests(ImageType<T> imageType) {
		this.imageType = imageType;
	}

	public abstract TrackerObjectQuad<T> create( ImageType<T> imageType );

	@Test
	public void stationary() {
		TrackerObjectQuad<T> tracker = create(imageType);
		render(1,0,0);
		assertTrue(tracker.initialize(input, initRegion));
		assertTrue(tracker.process(input, where));

		assertEquals(20, where.a.x, 1e-8);
		assertEquals(25, where.a.y, 1e-8);
		assertEquals(120, where.c.x, 1e-8);
		assertEquals(160, where.c.y, 1e-8);
	}

	@Test
	public void translation_small() {
		TrackerObjectQuad<T> tracker = create(imageType);
		render(1,0,0);
		assertTrue(tracker.initialize(input, initRegion));

		for( int i = 0; i < 10; i++ ) {
			int tranX =  2*i;
			int tranY = -2*i;

			render(1,tranX,tranY);
			assertTrue(tracker.process(input, where));

			checkSolution(20+tranX,25+tranY,120+tranX,160+tranY,tolTranslateSmall);
		}
	}

	@Test
	public void translation_large() {
		TrackerObjectQuad<T> tracker = create(imageType);
		render(1,0,0);
		assertTrue(tracker.initialize(input, initRegion));

		int tranX =  20;
		int tranY =  30;

		render(1,tranX,tranY);
		assertTrue(tracker.process(input, where));

		checkSolution(20+tranX,25+tranY,120+tranX,160+tranY,0.05);
	}

	@Test
	public void zooming_in() {
		zoom(-1);
	}

	@Test
	public void zooming_out() {
		zoom(1);
	}

	/**
	 * Zoom in and out without any visual translation of the object.  e.g. the center is constant
	 * @param dir
	 */
	protected void zoom( double dir ) {
		TrackerObjectQuad<T> tracker = create(imageType);
		render(1,0,0);
		assertTrue(tracker.initialize(input, initRegion));

		double centerX = 20+50;
		double centerY = 25+(160-25)/2.0;

		for( int i = 0; i < 10; i++ ) {
			double scale = 1 + dir*0.2*(i/9.0);
//			System.out.println("scale "+scale);

			double w2 = 100*scale/2.0;
			double h2 = (160-25)*scale/2.0;

			double tranX = centerX - centerX*scale;
			double tranY = centerY - centerY*scale;

			render(scale,tranX,tranY);
			assertTrue(tracker.process(input, where));

//			System.out.println("actual width "+2*w2);
			checkSolution(centerX-w2,centerY-h2,centerX+w2,centerY+h2,tolScale);
		}
	}

	/**
	 * See if it correctly reinitializes.  Should produce identical results when given the same inputs after
	 * being reinitialized.
	 */
	@Test
	public void reinitialize() {
		Quadrilateral_F64 where1 = new Quadrilateral_F64();

		TrackerObjectQuad<T> tracker = create(imageType);
		render(1,0,0);
		assertTrue(tracker.initialize(input, initRegion));
		render(1,3,-3);
		assertTrue(tracker.process(input, where));
		render(1,6,-6);
		assertTrue(tracker.process(input, where));

		render(1,0,0);
		assertTrue(tracker.initialize(input, initRegion));
		render(1,3,-3);
		assertTrue(tracker.process(input, where1));
		render(1,6,-6);
		assertTrue(tracker.process(input, where1));

		// Might not be a perfect match due to robust algorithm not being reset to their initial state
		checkSolution(where1.a.x,where1.a.y,where1.c.x,where1.c.y,0.02);
	}

	private void checkSolution( double x0 , double y0 , double x1 , double y1 , double fractionError ) {
		System.out.println("Expected "+x0+" "+y0+" "+x1+" "+y1);
		System.out.println("Actual "+where.a.x+" "+where.a.y+" "+where.c.x+" "+where.c.y);

		double tolX = (x1-x0)*fractionError;
		double tolY = (y1-y0)*fractionError;
		double tol = Math.max(tolX,tolY);

		assertTrue(Math.abs(where.a.x - x0) <= tol);
		assertTrue(Math.abs(where.a.y - y0) <= tol);
		assertTrue(Math.abs(where.c.x - x1) <= tol);
		assertTrue(Math.abs(where.c.y - y1) <= tol);
	}

	protected abstract void render( double scale , double tranX , double tranY );

	private static Quadrilateral_F64 rect( int x0 , int y0 , int x1 , int y1 ) {
		return new Quadrilateral_F64(x0,y0,x1,y0,x1,y1,x0,y1);
	}

}
