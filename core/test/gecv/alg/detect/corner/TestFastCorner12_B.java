package gecv.alg.detect.corner;

import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestFastCorner12_B {
	private int[] offsets;

	int sideLength = 7;
	byte centerVal = 100;

	ImageInt8 img;

	public TestFastCorner12_B() {

		int center = 3 * sideLength + 3;

		offsets = new int[16];
		offsets[0] = center - 3;
		offsets[1] = center - 3 - sideLength;
		offsets[2] = center - 2 - 2 * sideLength;
		offsets[3] = center - 1 - 3 * sideLength;
		offsets[4] = center - 3 * sideLength;
		offsets[5] = center + 1 - 3 * sideLength;
		offsets[6] = center + 2 - 2 * sideLength;
		offsets[7] = center + 3 - sideLength;
		offsets[8] = center + 3;
		offsets[9] = center + 3 + sideLength;
		offsets[10] = center + 2 + 2 * sideLength;
		offsets[11] = center + 1 + 3 * sideLength;
		offsets[12] = center + 3 * sideLength;
		offsets[13] = center - 1 + 3 * sideLength;
		offsets[14] = center - 2 + 2 * sideLength;
		offsets[15] = center - 3 + sideLength;

		img = new ImageInt8(sideLength, sideLength);
	}

	/**
	 * Create a set of synthetic images and see if it correctly identifies them as corners.
	 */
	@Test
	public void testPositive() {
		FastCorner12_B corner = new FastCorner12_B(img, 20, 12);

		// pixels in circle are lower than threshold
		for (int i = 0; i < 15; i++) {
			setSynthetic(img, i, 12, (byte) (centerVal - 50));

			corner.process();

			assertEquals(1, countNonZero(corner.getIntensity()));
		}

		// pixels in circle are higher than threshold
		for (int i = 0; i < 15; i++) {
			setSynthetic(img, i, 12, (byte) (centerVal + 50));

			corner.process();

			assertEquals(1, countNonZero(corner.getIntensity()));
		}

		// longer than needed
		for (int i = 0; i < 15; i++) {
			setSynthetic(img, i, 13, (byte) (centerVal + 50));

			corner.process();

			assertEquals(1, countNonZero(corner.getIntensity()));
		}

	}

	private static int countNonZero(ImageFloat32 img) {
		float[] data = img.data;

		int ret = 0;
		for (float aData : data) {
			if (aData < 0)
				fail("intensity images should have all positive elements");

			if (aData != 0)
				ret++;
		}
		return ret;
	}

	/**
	 * See if it classifies a circle that is too short
	 */
	@Test
	public void testNegativeShort() {
		FastCorner12_B corner = new FastCorner12_B(img, 20, 12);

		for (int i = 0; i < 15; i++) {
			setSynthetic(img, i, 11, (byte) (centerVal + 50));

			corner.process();

			assertEquals(0, countNonZero(corner.getIntensity()));
		}
	}

	/**
	 * Both pixels that are too high and low, but exceed the threshold are mixed
	 */
	@Test
	public void testNegativeMixed() {
		FastCorner12_B corner = new FastCorner12_B(img, 20, 12);

		for (int i = 0; i < 15; i++) {
			setSynthetic(img, i, 12, (byte) (centerVal + 50));

			img.data[offsets[(i + 7) % offsets.length]] = (byte) (centerVal - 50);

			corner.process();

			assertEquals(0, countNonZero(corner.getIntensity()));
		}
	}

	private void setSynthetic(ImageInt8 img, int start, int length, byte outerVal) {
		byte data[] = img.data;

		int endA = start + length;
		int endB;

		if (endA > offsets.length) {
			endB = endA - offsets.length;
			endA = offsets.length;
		} else {
			endB = 0;
		}

		for (int i = 0; i < sideLength; i++) {
			for (int j = 0; j < sideLength; j++) {
				img.set(i, j, centerVal);
			}
		}

		for (int i = start; i < endA; i++) {
			data[offsets[i]] = outerVal;
		}

		for (int i = 0; i < endB; i++) {
			data[offsets[i]] = outerVal;
		}
	}

	@Test
	public void testSubImage() {
		fail("implement");
	}
}
