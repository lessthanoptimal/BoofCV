package boofcv.alg.tracker.circulant;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.core.image.ConvertImage;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageFloat64;
import boofcv.struct.image.InterleavedF64;
import georegression.struct.shapes.Rectangle2D_I32;
import org.ddogleg.complex.ComplexMath64F;
import org.ejml.data.Complex64F;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
* @author Peter Abeles
*/
public class TestCirculantTrackerOrig {

	Random rand = new Random(234);

	int width = 60;
	int height = 80;

	@Test
	public void basicTrackingCheck() {
		ImageFloat32 a = new ImageFloat32(30,35);
		ImageFloat32 b = new ImageFloat32(30,35);

		// randomize input image and move it
		GImageMiscOps.fillUniform(a,rand,0,200);
		GImageMiscOps.fillUniform(b,rand,0,200);

		CirculantTrackerOrig alg = new CirculantTrackerOrig(1.0/16.0,0.2f,1e-2f,0.075f,255f);
		alg.initialize(a, 5, 6, 20, 25);

		shiftCopy(2,4,a,b);
		alg.performTracking(b);

		double tolerance = 1;

		Rectangle2D_I32 r = alg.getTargetLocation();
		assertEquals(5+2,r.tl_x,tolerance);
		assertEquals(6 + 4, r.tl_y, tolerance);
	}

	@Test
	public void computeCosineWindow() {
		ImageFloat64 found = new ImageFloat64(20,25);

		CirculantTrackerOrig.computeCosineWindow(found);

		// should be between 0 and 1
		for( int i = 0; i < found.data.length; i++ ) {
			assertTrue( found.data[i] >= 0 && found.data[i] <= 1);
		}

		centeredSymmetricChecks(found);
	}

	@Test
	public void computeGaussianWeights() {
		CirculantTrackerOrig alg = new CirculantTrackerOrig(0.5f,2f,0.01f,0.1f,255f);

		alg.regionTrack.set(2,3,10,15);
		alg.gaussianWeight.reshape(10,15);
		alg.gaussianWeightDFT.reshape(10, 15);

		alg.computeGaussianWeights();

		centeredSymmetricChecks(alg.gaussianWeight);
	}

	private void centeredSymmetricChecks( ImageFloat64 image ) {

		int cx = image.width/2;
		int cy = image.height/2;
		int w = image.width-1;
		int h = image.height-1;

		// edges should be smaller than center
		assertTrue( image.get(cx,cy) > image.get(0,0));
		assertTrue( image.get(cx,cy) > image.get(w,h) );
		assertTrue( image.get(cx,cy) > image.get(w,h) );
		assertTrue( image.get(cx,cy) > image.get(w,0) );

		// symmetry check
		for( int i = 0; i < cy; i++ ) {
			for( int j = 0; j < cx; j++ ) {
				double v0 = image.get(j,i);
				double v1 = image.get(w-j,i);
				double v2 = image.get(j,h-i);
				double v3 = image.get(w-j,h-i);

				assertEquals(v0,v1,1e-4);
				assertEquals(v0,v2,1e-4);
				assertEquals(v0,v3,1e-4);
			}
		}
	}

	/**
	 * Check a few simple motions.  It seems to be accurate to within 1 pixel.  Considering alphas seems to be the issue
	 */
	@Test
	public void updateTrackLocation() {
		ImageFloat32 a = new ImageFloat32(30,35);
		ImageFloat32 b = new ImageFloat32(30,35);

		// randomize input image and move it
		GImageMiscOps.fillUniform(a,rand,0,200);
		GImageMiscOps.fillUniform(b,rand,0,200);
		shiftCopy(0,0,a,b);

		CirculantTrackerOrig alg = new CirculantTrackerOrig(1f/16f,0.2f,1e-2f,0.075f,255f);
		alg.initialize(a,5,6,20,25);

		alg.updateTrackLocation(b);

		// not super precise...
		int tolerance = 0;

		// No motion motion
		Rectangle2D_I32 r = alg.getTargetLocation();
		assertEquals(5,r.tl_x,tolerance);
		assertEquals(6,r.tl_y,tolerance);

		// check estimated motion
		GImageMiscOps.fillUniform(b,rand,0,200);
		shiftCopy(-3,2,a,b);
		alg.updateTrackLocation(b);
		r = alg.getTargetLocation();
		assertEquals(5-3,r.tl_x,tolerance);
		assertEquals(6+2,r.tl_y,tolerance);

		// try out of bounds case
		GImageMiscOps.fillUniform(b,rand,0,200);
		shiftCopy(-6,0,a,b);
		alg.updateTrackLocation(b);
		assertEquals(0,r.tl_x,tolerance);
		assertEquals(6,r.tl_y,tolerance);
	}

	@Test
	public void performLearning() {
		float interp_factor = 0.075f;

		ImageFloat32 a = new ImageFloat32(20,25);
		ImageFloat32 b = new ImageFloat32(20,25);

		ImageMiscOps.fill(a,100);
		ImageMiscOps.fill(b,200);

		CirculantTrackerOrig alg = new CirculantTrackerOrig(1f/16f,0.2f,1e-2f,interp_factor,255f);
		alg.initialize(a,0,0,20,25);

		// copy its internal value
		ConvertImage.convert(alg.subPrev, a);

		// give it two images
		alg.performLearning(b);

		// make sure the images aren't full of zero
		assertTrue(Math.abs(ImageStatistics.sum(a)) > 0.1 );
		assertTrue(Math.abs(ImageStatistics.sum(alg.subInput)) > 0.1 );


		int numNotSame = 0;
		// the result should be an average of the two
		for( int i = 0; i < a.data.length; i++ ) {
			if( Math.abs(a.data[i]-alg.subInput.data[i]) > 1e-4 )
				numNotSame++;

			// should be more like the original one than the new one
			double expected = a.data[i]*(1-interp_factor) + interp_factor*alg.subInput.data[i];
			double found = alg.subPrev.data[i];

			assertEquals(expected,found,1e-4);
		}

		// make sure it is actually different
		assertTrue(numNotSame>100);
	}

	@Test
	public void dense_gauss_kernel() {
		// try several different shifts
		dense_gauss_kernel(0,0);
		dense_gauss_kernel(5,0);
		dense_gauss_kernel(0,5);
		dense_gauss_kernel(-3,-2);
	}

	public void dense_gauss_kernel( int offX , int offY ) {
		ImageFloat32 regionIn = new ImageFloat32(20,25);
		ImageFloat64 regionOut = new ImageFloat64(20,25);
		ImageFloat32 targetIn = new ImageFloat32(20,25);
		ImageFloat64 targetOut = new ImageFloat64(20,25);
		ImageFloat64 k = new ImageFloat64(20,25);

		CirculantTrackerOrig alg = new CirculantTrackerOrig(1f/16f,0.2f,1e-2f,0.075f,255f);
		alg.initialize(regionIn,0,0,20,25);

		// randomize input image and add a known shape in it
		GImageMiscOps.fillUniform(regionIn,rand,0,200);
		GImageMiscOps.fillUniform(targetIn,rand,0,200);

		// copy a shifted portion of the region
		shiftCopy(offX, offY, regionIn, targetIn);

		// initialize data structures
		alg.get_subwindow(regionIn,0,0,regionOut);
		alg.get_subwindow(targetIn,0,0,targetOut);

		// process and see if the peak is where it should be
		alg.dense_gauss_kernel(0.2f,regionOut,targetOut,k);

		int maxX=-1,maxY=-1;
		double maxValue = -1;
		for( int y = 0; y < k.height;y++ ){
			for( int x=0; x < k.width;x++ ) {
				if( k.get(x,y) > maxValue ) {
					maxValue = k.get(x,y);
					maxX = x;
					maxY = y;
				}
			}
		}

		int expectedX = k.width/2-offX;
		int expectedY = k.height/2-offY;

		assertEquals(expectedX,maxX);
		assertEquals(expectedY,maxY);
	}

	private void shiftCopy(int offX, int offY, ImageFloat32 src, ImageFloat32 dst) {
		for( int y = 0; y < src.height; y++ ) {
			for( int x = 0; x < src.width; x++ ) {
				int xx = x + offX;
				int yy = y + offY;

				if( xx >= 0 && xx < src.width && yy >= 0 && yy < src.height ) {
					dst.set(xx, yy, src.get(x, y));
				}
			}
		}
	}
	private void shiftCopy(int offX, int offY, ImageFloat32 src, ImageFloat64 dst) {
		for( int y = 0; y < src.height; y++ ) {
			for( int x = 0; x < src.width; x++ ) {
				int xx = x + offX;
				int yy = y + offY;

				if( xx >= 0 && xx < src.width && yy >= 0 && yy < src.height ) {
					dst.set(xx, yy, src.get(x, y));
				}
			}
		}
	}

	@Test
	public void imageDotProduct() {
		ImageFloat64 a = new ImageFloat64(width,height);
		ImageMiscOps.fillUniform(a,rand,0,10);

		double total = 0;
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				total += a.get(x,y)*a.get(x,y);
			}
		}
		double found = CirculantTrackerOrig.imageDotProduct(a);
		assertEquals(total,found,1e-4);
	}

	@Test
	public void elementMultConjB() {
		InterleavedF64 a = new InterleavedF64(width,height,2);
		InterleavedF64 b = new InterleavedF64(width,height,2);
		InterleavedF64 c = new InterleavedF64(width,height,2);

		ImageMiscOps.fillUniform(a,rand,-10,10);
		ImageMiscOps.fillUniform(b,rand,-10,10);
		ImageMiscOps.fillUniform(c,rand,-10,10);

		CirculantTrackerOrig.elementMultConjB(a, b, c);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				Complex64F aa = new Complex64F(a.getBand(x,y,0),a.getBand(x,y,1));
				Complex64F bb = new Complex64F(b.getBand(x,y,0),b.getBand(x,y,1));

				Complex64F cc = new Complex64F();
				ComplexMath64F.conj(bb, bb);
				ComplexMath64F.mult(aa,bb,cc);

				double foundReal = c.getBand(x,y,0);
				double foundImg = c.getBand(x,y,1);

				assertEquals(cc.real,foundReal,1e-4);
				assertEquals(cc.imaginary,foundImg,1e-4);
			}
		}
	}

	@Test
	public void computeAlphas() {
		InterleavedF64 yf = new InterleavedF64(width,height,2);
		InterleavedF64 kf = new InterleavedF64(width,height,2);
		InterleavedF64 alphaf = new InterleavedF64(width,height,2);

		ImageMiscOps.fillUniform(yf,rand,-10,10);
		ImageMiscOps.fillUniform(kf,rand,-10,10);
		ImageMiscOps.fillUniform(alphaf,rand,-10,10);

		float lambda = 0.01f;
		CirculantTrackerOrig.computeAlphas(yf, kf, lambda, alphaf);

		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				Complex64F a = new Complex64F(yf.getBand(x,y,0),yf.getBand(x,y,1));
				Complex64F b = new Complex64F(kf.getBand(x,y,0)+lambda,kf.getBand(x,y,1));

				Complex64F c = new Complex64F();
				ComplexMath64F.div(a,b,c);

				double foundReal = alphaf.getBand(x,y,0);
				double foundImg = alphaf.getBand(x,y,1);

				assertEquals(c.real,foundReal,1e-4);
				assertEquals(c.imaginary,foundImg,1e-4);
			}
		}
	}

	@Test
	public void ensureInBounds() {
		Rectangle2D_I32 r = new Rectangle2D_I32();

		r.set(-1,-2,5,6);
		CirculantTrackerOrig.ensureInBounds(r, 20, 25);
		assertEquals(0, r.tl_x);
		assertEquals(0,r.tl_y);

		r.set(16,21,5,6);
		CirculantTrackerOrig.ensureInBounds(r, 20, 25);
		assertEquals(20-5,r.tl_x);
		assertEquals(25-6,r.tl_y);

		r.set(1,2,5,6);
		CirculantTrackerOrig.ensureInBounds(r, 20, 25);
		assertEquals(1,r.tl_x);
		assertEquals(2,r.tl_y);
	}

}
