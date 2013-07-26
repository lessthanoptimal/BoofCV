package boofcv.alg.tracker.tld;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F32;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestTldFernClassifier {

	int width = 60;
	int height = 80;

	Random rand = new Random(234);

	ImageUInt8 input = new ImageUInt8(width,height);

	InterpolatePixel<ImageUInt8> interpolate = FactoryInterpolation.bilinearPixel(ImageUInt8.class);

	public TestTldFernClassifier() {
		ImageMiscOps.fillUniform(input,rand,0,200);
		interpolate.setImage(input);
	}

	@Test
	public void updateFerns() {
		fail("update");
//		CheckUpdateFernsClass alg = new CheckUpdateFernsClass();
//
//		alg.learnFernNoise(true, new ImageRectangle());
//
//		for( int i = 0; i < alg.managers.length; i++ ) {
//			TldFernFeature f =  alg.managers[i].table[i];
//			assertTrue(f != null);
//			assertEquals(1, f.numP);
//			assertEquals(0, f.numN);
//		}
//
//		alg.num = 0;
//		alg.learnFernNoise(false, new ImageRectangle());
//
//		for( int i = 0; i < alg.managers.length; i++ ) {
//			TldFernFeature f =  alg.managers[i].table[i];
//			assertTrue(f != null);
//			assertEquals(1, f.numP);
//			assertEquals(1, f.numN);
//		}
	}

	@Test
	public void performTest() {
		fail("update");
//		CheckUpdateFernsClass alg = new CheckUpdateFernsClass();
//
//		alg.learnFernNoise(true, new ImageRectangle());
//
//		alg.num = 0;
//		assertTrue(alg.performTest(new ImageRectangle()));
	}

	@Test
	public void computeFernValue() {

		TldFernDescription fern = new TldFernDescription(rand,10);

		ImageRectangle r = new ImageRectangle(2,20,12,28);

		boolean expected[] = new boolean[10];
		for( int i = 0; i < 10; i++ ) {
			Point2D_F32 a = fern.pairs[i].a;
			Point2D_F32 b = fern.pairs[i].b;

			float valA = interpolate.get(r.x0 + a.x*(r.x1-r.x0), r.y0 + a.y*(r.y1-r.y0));
			float valB = interpolate.get(r.x0 + b.x*(r.x1-r.x0), r.y0 + b.y*(r.y1-r.y0));

			expected[9-i] = valA < valB;
		}

		TldFernClassifier<ImageUInt8> alg = createAlg();
		alg.setImage(input);

		int found = alg.computeFernValue(r.x0,r.y0,r.getWidth(),r.getHeight(),fern);

		for( int i = 0; i < 10; i++ ) {
			assertTrue(expected[i] == (((found >> i) & 0x0001) == 1));
		}

	}

	private TldFernClassifier<ImageUInt8> createAlg() {
		fail("update");
//		InterpolatePixel<ImageUInt8> interpolate = FactoryInterpolation.bilinearPixel(ImageUInt8.class);
//		return new TldFernClassifier<ImageUInt8>(rand,10,8,interpolate);
		return null;
	}

//	private class CheckUpdateFernsClass extends TldFernClassifier {
//
//		int num = 0;
//
//		private CheckUpdateFernsClass() {
//			super(rand,10,8,null);
//		}
//
//		@Override
//		protected int computeFernValue(float x0, float y0, float rectWidth, float rectHeight, TldFernDescription fern) {
//			return num++;
//		}
//	}
}
