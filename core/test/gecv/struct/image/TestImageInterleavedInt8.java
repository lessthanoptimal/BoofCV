package gecv.struct.image;

/**
 * @author Peter Abeles
 */
public class TestImageInterleavedInt8 extends StandardImageInterleavedTests {


	@Override
	public ImageInterleaved createImage(int width, int height, int numBands) {
		return new ImageInterleavedInt8(width, height, numBands);
	}

	@Override
	public Number randomNumber() {
		return (byte) (rand.nextInt(255) - 126);
	}
}
