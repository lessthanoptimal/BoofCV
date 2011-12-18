package boofcv.struct.image;

import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestImageBase {

	@Test
	public void isInBounds() {
		Dummy a = new Dummy();
		a.width = 10;
		a.height = 11;
		
		assertTrue(a.isInBounds(0,0));
		assertTrue(a.isInBounds(9, 10));
		assertFalse(a.isInBounds(-1, 0));
		assertFalse(a.isInBounds(10, 0));
		assertFalse(a.isInBounds(0, -1));
		assertFalse(a.isInBounds(0, 11));
	}

	@Test
	public void indexToPixel() {
		Dummy a = new Dummy();
		
		a.startIndex = 7;
		a.stride = 5;
		a.width = 4;
		a.height = 11;

		Point2D_I32 p = a.indexToPixel(7+6*5+2);
		
		assertEquals(2,p.x);
		assertEquals(6,p.y);
	}

	@Test
	public void isSubimage() {
		Dummy a = new Dummy();
		a.startIndex = 0;
		a.stride = 10;
		a.width = 10;
		a.height = 11;
		
		assertFalse(a.isSubimage());
		
		a.startIndex = 1;
		assertTrue(a.isSubimage());
		a.startIndex = 0;

		a.stride = 5;
		assertTrue(a.isSubimage());
		a.startIndex = 10;

		a.height = 5;
		assertTrue(a.isSubimage());
	}
	
	private static class Dummy extends ImageBase
	{

		@Override
		public ImageBase subimage(int x0, int y0, int x1, int y1) {return null;}

		@Override
		public void reshape(int width, int height) {}
	}
}
