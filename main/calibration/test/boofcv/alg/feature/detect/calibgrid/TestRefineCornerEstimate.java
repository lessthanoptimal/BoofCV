package boofcv.alg.feature.detect.calibgrid;

import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRefineCornerEstimate {

	int width = 50;
	int height = 60;

	@Test
	public void stuff() {
		ImageUInt8 orig = new ImageUInt8(width,height);
		ImageSInt16 derivX = new ImageSInt16(width,height);
		ImageSInt16 derivY = new ImageSInt16(width,height);

		ImageTestingOps.fillRectangle(orig, 100, 10, 10, 40, 50);
//		ImageTestingOps.addGaussian(orig,new Random(),1,0,255);

		GradientSobel.process(orig,derivX,derivY,null);
				
		RefineCornerEstimate<ImageSInt16> alg = new RefineCornerEstimate<ImageSInt16>();
		alg.setInputs(derivX,derivY);

		assertTrue(alg.process(0, 0, 20, 20));
		System.out.println("found = "+alg.getX()+"  "+alg.getY());
		assertEquals(10, alg.getX(), 1e-8);
		assertEquals(10,alg.getY(),1e-8);

		assertTrue(alg.process(width-15,height-15,width,height));
		assertEquals(40,alg.getX(),1e-8);
		assertEquals(50,alg.getY(),1e-8);
	}
}
