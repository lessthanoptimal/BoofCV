package gecv.struct.image;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestImageFloat32 extends StandardImageTests {


	@Override
	public ImageBase createImage(int width, int height) {
		return new ImageFloat32(width, height);
	}

	@Override
	public Number randomNumber() {
		return rand.nextFloat();
	}

}
