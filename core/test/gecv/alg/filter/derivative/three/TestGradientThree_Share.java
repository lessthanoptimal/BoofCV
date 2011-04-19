package gecv.alg.filter.derivative.three;

import gecv.core.image.UtilImageFloat32;
import gecv.struct.image.ImageFloat32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestGradientThree_Share {
	Random rand = new Random(234);

	int width = 200;
	int height = 250;


	@Test
	public void derivX_F32() {
		ImageFloat32 img = new ImageFloat32(width, height);
		UtilImageFloat32.randomize(img, rand, 0f, 255f);

		ImageFloat32 derivX = new ImageFloat32(width, height);

		ImageFloat32 derivX2 = new ImageFloat32(width, height);

		GradientThree_Standard.derivX_F32(img, derivX2);
		GradientThree_Share.derivX_F32(img, derivX);

		GecvTesting.assertEquals(derivX2, derivX, 0, 1e-4f);
	}
}
