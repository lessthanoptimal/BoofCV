package boofcv.alg.geo.d2.stabilization;

import boofcv.struct.image.ImageUInt8;
import georegression.struct.affine.Affine2D_F32;
import org.junit.Test;

import static boofcv.alg.geo.d2.stabilization.TestImageMotionPointKey.DummyModelMatcher;
import static boofcv.alg.geo.d2.stabilization.TestImageMotionPointKey.DummyTracker;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestMotionStabilizePointKey {

	@Test
	public void checkLargeDistortion() {
		Affine2D_F32 model = new Affine2D_F32();
		Affine2D_F32 computed = new Affine2D_F32(1,0,0,1,2,3);
		DummyTracker tracker = new DummyTracker();
		DummyModelMatcher<Affine2D_F32> matcher = new DummyModelMatcher<Affine2D_F32>(computed,20);

		ImageUInt8 input = new ImageUInt8(20,30);

		MotionStabilizePointKey<ImageUInt8,Affine2D_F32> alg =
				new MotionStabilizePointKey<ImageUInt8,Affine2D_F32>(tracker,matcher,model,3,5,10);
		
		// sanity check here
		assertTrue(alg.process(input));
		assertFalse(alg.isKeyFrame());
		assertFalse(alg.isReset());

		// make sure there is a huge motion that should trigger a reset
		matcher.setMotion(new Affine2D_F32(100,0,0,200,2,3));
		assertTrue(alg.process(input));
		assertTrue(alg.isReset());

	}
}
