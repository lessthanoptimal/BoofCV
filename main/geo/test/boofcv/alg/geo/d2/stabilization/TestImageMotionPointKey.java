package boofcv.alg.geo.d2.stabilization;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.ModelMatcher;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.se.Se2_F32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestImageMotionPointKey {

	/**
	 * Give it a very simple example and see if it computes the correct motion and has the expected behavior
	 * when processing an image
	 */
	@Test
	public void process() {
		// what the initial transform should be
		Se2_F32 initial = new Se2_F32(1,2,3);
		Se2_F32 computed = new Se2_F32(4,5,6);
		Se2_F32 model = new Se2_F32();
		DummyTracker tracker = new DummyTracker();
		DummyModelMatcher matcher = new DummyModelMatcher(computed,5);

		ImageUInt8 input = new ImageUInt8(20,30);
		
		ImageMotionPointKey<ImageUInt8,Se2_F32> alg = new ImageMotionPointKey<ImageUInt8,Se2_F32>(tracker,matcher,model);

		// specify an initial transform
		alg.setInitialTransform(initial);
		
		// the first time it processes an image it should always return true since no motion is estimated
		assertTrue(alg.process(input));

		// the transform should be the same as the initial one
		// and requested that new tracks be spawned
		Se2_F32 found = alg.getWorldToCurr();
		assertEquals(initial.getX(),found.getX(),1e-8);
		assertEquals(1,tracker.numSpawn);

		// now it should compute some motion
		assertTrue(alg.process(input));

		// no new tracks should have been spawned
		assertEquals(1,tracker.numSpawn);

		// test the newly computed results
		Se2_F32 keyToCurr = alg.getKeyToCurr();
		assertEquals(computed.getX(),keyToCurr.getX(),1e-8);

		// see if this transform was correctly computed
		Se2_F32 worldToCurr = initial.concat(keyToCurr, null);
		found = alg.getWorldToCurr();
		assertEquals(worldToCurr.getX(), found.getX(), 1e-8);
	}

	@Test
	public void changeWorld() {
		Se2_F32 oldToNew = new Se2_F32(1,2,0);
		Se2_F32 model = new Se2_F32();

		// the world frame will initially be the identify matrix
		ImageMotionPointKey<ImageUInt8,Se2_F32> alg = new ImageMotionPointKey<ImageUInt8,Se2_F32>(null,null,model);

		// change it to this frame
		alg.changeWorld(oldToNew);

		// see of all the other ones are updated correctly
		Se2_F32 worldToCurr = alg.getWorldToCurr();
		Se2_F32 worldToKey = alg.getWorldToKey();

		// since they were initially the identity this test should work
		assertEquals(oldToNew.getX(),worldToCurr.getX(),1e-8);
		assertEquals(oldToNew.getX(),worldToKey.getX(),1e-8);
	}

	/**
	 * Test the keyframe based on the definition of the keyframe
	 */
	@Test
	public void changeKeyFrame() {
		Se2_F32 computed = new Se2_F32(4,5,6);
		Se2_F32 model = new Se2_F32();
		DummyTracker tracker = new DummyTracker();
		DummyModelMatcher matcher = new DummyModelMatcher(computed,5);
		
		ImageUInt8 input = new ImageUInt8(20,30);

		ImageMotionPointKey<ImageUInt8,Se2_F32> alg = new ImageMotionPointKey<ImageUInt8,Se2_F32>(tracker,matcher,model);
		
		// process twice to change the transforms
		alg.process(input);
		alg.process(input);

		// sanity check
		Se2_F32 worldToKey = alg.getWorldToKey();
		assertEquals(0,worldToKey.getX(),1e-8);
		assertEquals(1,tracker.numSpawn);
		
		// invoke the function being tested
		alg.changeKeyFrame();
		
		// the keyframe should be changed and new tracks spawned
		assertEquals(1,tracker.numSetKeyframe);
		assertEquals(2,tracker.numSpawn);
		
		// worldToKey should now be equal to worldToCurr
		worldToKey = alg.getWorldToKey();
		assertEquals(computed.getX(),worldToKey.getX(),1e-8);
	}
	
	private static class DummyTracker implements ImagePointTracker
	{
		public int numSpawn = 0;
		public int numSetKeyframe = 0;

		@Override
		public void process(ImageSingleBand image) {}

		@Override
		public boolean addTrack(double x, double y) {
			return true;
		}

		@Override
		public void spawnTracks() {numSpawn++;}

		@Override
		public void dropTracks() {}

		@Override
		public void setCurrentToKeyFrame() {numSetKeyframe++;}

		@Override
		public void dropTrack(AssociatedPair track) {}

		@Override
		public List<AssociatedPair> getActiveTracks() {
			return new ArrayList<AssociatedPair>();
		}

		@Override
		public List<AssociatedPair> getDroppedTracks() {
			return null;
		}

		@Override
		public List<AssociatedPair> getNewTracks() {
			return null;
		}
	}
	
	private class DummyModelMatcher implements ModelMatcher<Se2_F32,AssociatedPair> {

		Se2_F32 found;
		int matchSetSize;

		private DummyModelMatcher(Se2_F32 found, int matchSetSize) {
			this.found = found;
			this.matchSetSize = matchSetSize;
		}

		@Override
		public boolean process(List<AssociatedPair> dataSet, Se2_F32 initialGuess) {
			return true;
		}

		@Override
		public Se2_F32 getModel() {
			return found;
		}

		@Override
		public List<AssociatedPair> getMatchSet() {
			List<AssociatedPair> ret = new ArrayList<AssociatedPair>();
			for( int i = 0; i < matchSetSize; i++ ) {
				ret.add( new AssociatedPair());
			}
			return ret;
		}

		@Override
		public double getError() {
			return 0;
		}
	}
}
