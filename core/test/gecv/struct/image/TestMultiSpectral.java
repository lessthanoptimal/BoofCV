package gecv.struct.image;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestMultiSpectral {

	int imgWidth = 10;
	int imgHeight = 20;

	@Test
	public void constructor() {
		DummySpectral img = new DummySpectral(imgWidth, imgHeight, 3);

		assertTrue(ImageInt8.class == img.getType());
		assertTrue(3 == img.bands.length);
		assertTrue(3 == img.getNumBands());
		assertTrue(imgWidth == img.width);
		assertTrue(imgHeight == img.height);
		for (int i = 0; i < 3; i++) {
			assertTrue(img.bands[i] != null);
		}
	}


	@Test
	public void getBand() {
		DummySpectral img = new DummySpectral(imgWidth, imgHeight, 3);

		assertTrue(img.getBand(0) != null);

		try {
			img.getBand(-1);
			fail("Exception should have been thrown");
		} catch (IllegalArgumentException e) {

		}
		try {
			img.getBand(3);
			fail("Exception should have been thrown");
		} catch (IllegalArgumentException e) {

		}
	}

	public static class DummySpectral extends MultiSpectral<ImageInt8> {
		public DummySpectral(int width, int height, int numBands) {
			super(ImageInt8.class, width, height, numBands);
		}

		@Override
		protected ImageInt8 declareImage(int width, int height) {
			return new ImageInt8(width, height);
		}
	}
}
