package boofcv.alg.tracker.tld;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.transform.ii.IntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestTldVarianceFilter {

	Random rand = new Random(234);

	@Test
	public void selectThreshold() {
		ImageUInt8 image = new ImageUInt8(50,80);
		ImageMiscOps.fillUniform(image, rand, 0, 200);

		TldVarianceFilter alg = new TldVarianceFilter(ImageUInt8.class);
		alg.setImage(image);

		alg.selectThreshold(new ImageRectangle(10,8,21,33));

		double found = alg.getThresholdLower();
		double expected = computeVariance(image,10,8,21,33)/2.0;

		assertEquals(expected, found, 1e-8);
	}

	@Test
	public void computeVariance() {
		ImageUInt8 image = new ImageUInt8(50,80);
		ImageMiscOps.fillUniform(image, rand, 0, 200);

		TldVarianceFilter alg = new TldVarianceFilter(ImageUInt8.class);
		alg.setImage(image);

		double found = alg.computeVariance(10,8,21,33);
		double expected = computeVariance(image,10,8,21,33);

		assertEquals(expected, found, 1e-8);
	}


	@Test
	public void transformSq_U8() {
		ImageUInt8 image = new ImageUInt8(50,80);
		ImageMiscOps.fillUniform(image,rand,0,200);

		// test a regular image and a sub-image
		transformSq_U8(image);
		transformSq_U8(BoofTesting.createSubImageOf(image));
	}

	public void transformSq_U8( ImageUInt8 image ) {
		ImageSInt64 ii = new ImageSInt64(50,80);

		TldVarianceFilter.transformSq(image,ii);

		long found = IntegralImageOps.block_unsafe(ii,9,10,19,20);
		long expected = (long)computeSumSq(image,10,11,20,21);

		assertEquals(expected, found);
	}

	@Test
	public void transformSq_F32() {
		ImageFloat32 image = new ImageFloat32(50,80);
		ImageMiscOps.fillUniform(image,rand,0,100);

		// test a regular image and a sub-image
		transformSq_F32(image);
		transformSq_F32(BoofTesting.createSubImageOf(image));
	}

	public void transformSq_F32( ImageFloat32 image ) {
		ImageFloat64 ii = new ImageFloat64(50,80);

		TldVarianceFilter.transformSq(image,ii);

		double found = IntegralImageOps.block_unsafe(ii,9,10,19,20);
		double expected = computeSumSq(image,10,11,20,21);

		assertEquals(expected, found, 0.01);
	}

	public double computeSumSq( ImageSingleBand image , int x0 , int y0 , int x1 , int y1 ) {

		double total = 0;
		for( int y = y0; y < y1; y++ ) {
			for( int x = x0; x < x1; x++ ) {
				double v = GeneralizedImageOps.get(image,x,y);

				total += v*v;
			}
		}

		return total;
	}

	public double computeVariance( ImageSingleBand image , int x0 , int y0 , int x1 , int y1 ) {

		double area = (x1-x0)*(y1-y0);

		double mean = 0;
		for( int y = y0; y < y1; y++ ) {
			for( int x = x0; x < x1; x++ ) {
				mean += GeneralizedImageOps.get(image,x,y);
			}
		}
		mean /= area;

		double var = 0;
		for( int y = y0; y < y1; y++ ) {
			for( int x = x0; x < x1; x++ ) {
				double d = GeneralizedImageOps.get(image,x,y) - mean;
				var += d*d;
			}
		}

		return var/area;
	}

}
