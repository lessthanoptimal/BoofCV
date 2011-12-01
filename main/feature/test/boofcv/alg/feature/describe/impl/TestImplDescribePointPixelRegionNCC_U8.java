package boofcv.alg.feature.describe.impl;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.NccFeature;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestImplDescribePointPixelRegionNCC_U8 {

	Random rand = new Random(234);

	ImageUInt8 img = new ImageUInt8(20,30);

	public TestImplDescribePointPixelRegionNCC_U8() {
		GeneralizedImageOps.randomize(img, rand, 0, 30);
	}

	@Test
	public void inner() {
		BoofTesting.checkSubImage(this, "checkInner", false, img, 4, 6, 7, 5);
		BoofTesting.checkSubImage(this, "checkInner", false, img, 7,3,4,5);
		BoofTesting.checkSubImage(this, "checkInner", false, img, 4,6,2,4);
	}

	public void checkInner( ImageUInt8 image , int c_x , int c_y , int w , int h ) {
		ImplDescribePointPixelRegionNCC_U8 alg = new ImplDescribePointPixelRegionNCC_U8(w,h);

		NccFeature desc = new NccFeature(alg.getDescriptorLength());
		alg.setImage(image);
		assertTrue(alg.process(c_x, c_y, desc));

		int y0 = c_y-h/2;
		int x0 = c_x-w/2;
		double mean = 0;
		for( int y = y0; y < y0+h; y++ ) {
			for( int x = x0; x < x0+w; x++ ) {
				mean += image.get(x,y);
			}
		}
		mean /= w*h;
		double variance = 0;
		for( int y = y0; y < y0+h; y++ ) {
			for( int x = x0; x < x0+w; x++ ) {
				double a = image.get(x,y) - mean;
				variance += a*a;
			}
		}
		variance /= w*h;
		assertEquals(desc.mean,mean,1e-8);
		assertEquals(desc.variance,variance,1e-8);

		int index = 0;
		for( int y = y0; y < y0+h; y++ ) {
			for( int x = x0; x < x0+w; x++ , index++ ) {
				assertEquals(image.get(x,y)-mean,desc.value[index],1e-4);
			}
		}
	}

	@Test
	public void border() {
		BoofTesting.checkSubImage(this, "checkBorder", false, img, 0,0,5,7);
		BoofTesting.checkSubImage(this, "checkBorder", false, img, img.width-1,img.height-1,5,7);
		BoofTesting.checkSubImage(this, "checkBorder", false, img, 100,200,5,7);
	}

	public void checkBorder( ImageUInt8 image , int c_x , int c_y , int w , int h ) {
		ImplDescribePointPixelRegionNCC_U8 alg = new ImplDescribePointPixelRegionNCC_U8(w,h);

		NccFeature desc = new NccFeature(alg.getDescriptorLength());
		alg.setImage(image);
		assertFalse(alg.process(c_x, c_y, desc));
	}
}
