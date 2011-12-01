package boofcv.alg.feature.describe.impl;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.TupleDesc_U8;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImplDescribePointPixelRegion_U8 {

	Random rand = new Random(234);

	ImageUInt8 img = new ImageUInt8(20,30);

	public TestImplDescribePointPixelRegion_U8() {
		GeneralizedImageOps.randomize(img,rand,0,30);
	}

	@Test
	public void inner() {
		BoofTesting.checkSubImage(this, "checkInner", false, img, 4, 6, 7, 5);
		BoofTesting.checkSubImage(this, "checkInner", false, img, 7,3,4,5);
		BoofTesting.checkSubImage(this, "checkInner", false, img, 4,6,2,4);
	}

	public void checkInner( ImageUInt8 image , int c_x , int c_y , int w , int h ) {
		ImplDescribePointPixelRegion_U8 alg = new ImplDescribePointPixelRegion_U8(w,h);

		TupleDesc_U8 desc = new TupleDesc_U8(alg.getDescriptorLength());
		alg.setImage(image);
		alg.process(c_x,c_y,desc);

		int index = 0;
		int y0 = c_y-h/2;
		int x0 = c_x-w/2;
		for( int y = y0; y < y0+h; y++ ) {
			for( int x = x0; x < x0+w; x++ , index++ ) {
				assertEquals(image.get(x,y),desc.value[index],1e-4);
			}
		}
	}

	@Test
	public void border() {
		BoofTesting.checkSubImage(this, "checkBorder", false, img, 0,0,5,7);
		BoofTesting.checkSubImage(this, "checkBorder", false, img, img.width-1,img.height-1,5,7);
		BoofTesting.checkSubImage(this, "checkBorder", false, img, 100, 200, 5, 7);
	}

	public void checkBorder( ImageUInt8 image , int c_x , int c_y , int w , int h ) {
		ImplDescribePointPixelRegion_U8 alg = new ImplDescribePointPixelRegion_U8(w,h);

		TupleDesc_U8 desc = new TupleDesc_U8(alg.getDescriptorLength());
		alg.setImage(image);
		alg.process(c_x,c_y,desc);

		int index = 0;
		int y0 = c_y-h/2;
		int x0 = c_x-w/2;
		for( int y = y0; y < y0+h; y++ ) {
			for( int x = x0; x < x0+w; x++ , index++ ) {
				if(image.isInBounds(x,y))
					assertEquals(image.get(x,y),desc.value[index],1e-4);
				else
					assertEquals(0,desc.value[index],1e-4);
			}
		}
	}
}
