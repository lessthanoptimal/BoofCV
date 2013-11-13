package boofcv.alg.tracker.circulant;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.InterleavedF32;
import georegression.struct.shapes.Rectangle2D_I32;
import org.ddogleg.complex.ComplexMath64F;
import org.ejml.data.Complex64F;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestCirculantTracker {

	Random rand = new Random(234);

	int width = 60;
	int height = 80;

	@Test
	public void basicTrackingCheck() {
		fail("Implement");
	}

	@Test
	public void computeCosineWindow() {
		ImageFloat32 found = new ImageFloat32(20,25);

		CirculantTracker.computeCosineWindow(found);

		// should be between 0 and 1
		for( int i = 0; i < found.data.length; i++ ) {
			assertTrue( found.data[i] >= 0 && found.data[i] <= 1);
		}

		centeredSymmetricChecks(found);
	}

	@Test
	public void computeGaussianWeights() {
		CirculantTracker alg = new CirculantTracker(0.5f,2f,0.01f,0.1f,255f);

		alg.region.set(2,3,10,15);
		alg.gaussianWeight.reshape(10,15);
		alg.gaussianWeightDFT.reshape(10, 15);

		alg.computeGaussianWeights();

		centeredSymmetricChecks(alg.gaussianWeight);
	}

	private void centeredSymmetricChecks( ImageFloat32 image ) {

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
				float v0 = image.get(j,i);
				float v1 = image.get(w-j,i);
				float v2 = image.get(j,h-i);
				float v3 = image.get(w-j,h-i);

				assertEquals(v0,v1,1e-4);
				assertEquals(v0,v2,1e-4);
				assertEquals(v0,v3,1e-4);
			}
		}
	}

	@Test
	public void updateTrackLocation() {
		fail("Implement");
	}

	@Test
	public void performLearning() {
		fail("Implement");
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
		ImageFloat32 region = new ImageFloat32(20,25);
		ImageFloat32 target = new ImageFloat32(20,25);
		ImageFloat32 k = new ImageFloat32(20,25);

		CirculantTracker alg = new CirculantTracker(1f/16f,0.2f,1e-2f,0.075f,255f);
		alg.initialize(region,0,0,20,25);

		// randomize input image and add a known shape in it
		GImageMiscOps.fillUniform(region,rand,0,200);
		GImageMiscOps.fillUniform(target,rand,0,200);

		// copy a shifted portion of the region
		for( int y = 0; y < region.height; y++ ) {
			for( int x = 0; x < region.width; x++ ) {
				int xx = x + offX;
				int yy = y + offY;

				if( xx >= 0 && xx < region.width && yy >= 0 && yy < region.height ) {
					target.set(xx,yy,region.get(x,y));
				}
			}
		}

		// initialize data structures
		alg.get_subwindow(region,0,0,region);
		alg.get_subwindow(target,0,0,target);

		// process and see if the peak is where it should be
		alg.dense_gauss_kernel(0.2f,region,target,k);

		int maxX=-1,maxY=-1;
		float maxValue = -1;
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

	@Test
	public void imageDotProduct() {
		ImageFloat32 a = new ImageFloat32(width,height);
		ImageMiscOps.fillUniform(a,rand,0,10);

		float total = 0;
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				total += a.get(x,y)*a.get(x,y);
			}
		}
		float found = CirculantTracker.imageDotProduct(a);
		assertEquals(total,found,1e-4);
	}

	@Test
	public void elementMultConjB() {
		InterleavedF32 a = new InterleavedF32(width,height,2);
		InterleavedF32 b = new InterleavedF32(width,height,2);
		InterleavedF32 c = new InterleavedF32(width,height,2);

		ImageMiscOps.fillUniform(a,rand,-10,10);
		ImageMiscOps.fillUniform(b,rand,-10,10);
		ImageMiscOps.fillUniform(c,rand,-10,10);

		CirculantTracker.elementMultConjB(a, b, c);

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
		InterleavedF32 yf = new InterleavedF32(width,height,2);
		InterleavedF32 kf = new InterleavedF32(width,height,2);
		InterleavedF32 alphaf = new InterleavedF32(width,height,2);

		ImageMiscOps.fillUniform(yf,rand,-10,10);
		ImageMiscOps.fillUniform(kf,rand,-10,10);
		ImageMiscOps.fillUniform(alphaf,rand,-10,10);

		float lambda = 0.01f;
		CirculantTracker.computeAlphas(yf, kf, lambda, alphaf);

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
		CirculantTracker.ensureInBounds(r, 20, 25);
		assertEquals(0, r.tl_x);
		assertEquals(0,r.tl_y);

		r.set(16,21,5,6);
		CirculantTracker.ensureInBounds(r,20,25);
		assertEquals(20-5,r.tl_x);
		assertEquals(25-6,r.tl_y);

		r.set(1,2,5,6);
		CirculantTracker.ensureInBounds(r,20,25);
		assertEquals(1,r.tl_x);
		assertEquals(2,r.tl_y);
	}

}
