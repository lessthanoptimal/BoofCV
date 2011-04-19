package gecv.struct.image;

/**
 * @author Peter Abeles
 */
public class TestImageInt32 extends StandardImageTests {


	@Override
	public ImageBase createImage(int width, int height) {
		return new ImageInt32(width, height);
	}

	@Override
	public Number randomNumber() {
		return rand.nextInt(200) - 100;
	}
}
