package boofcv.alg.feature.describe.impl;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImplDescribePointPixelRegion_F32 {

	Random rand = new Random(234);

	ImageFloat32 img = new ImageFloat32(20,30);

	public TestImplDescribePointPixelRegion_F32() {
		GeneralizedImageOps.randomize(img,rand,0,30);
	}

	@Test
	public void inner() {
		BoofTesting.checkSubImage(this, "checkInner", false, img, 4, 6, 7, 5);
		BoofTesting.checkSubImage(this, "checkInner", false, img, 7,3,4,5);
		BoofTesting.checkSubImage(this, "checkInner", false, img, 4,6,2,4);
	}

	public void checkInner( ImageFloat32 image , int c_x , int c_y , int w , int h ) {
		ImplDescribePointPixelRegion_F32 alg = new ImplDescribePointPixelRegion_F32(w,h);

		TupleDesc_F32 desc = new TupleDesc_F32(alg.getDescriptorLength());
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

	public void checkBorder( ImageFloat32 image , int c_x , int c_y , int w , int h ) {
		ImplDescribePointPixelRegion_F32 alg = new ImplDescribePointPixelRegion_F32(w,h);

		TupleDesc_F32 desc = new TupleDesc_F32(alg.getDescriptorLength());
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
