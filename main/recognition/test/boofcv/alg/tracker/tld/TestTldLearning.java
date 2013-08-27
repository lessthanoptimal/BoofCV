package boofcv.alg.tracker.tld;

import boofcv.struct.FastQueue;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.shapes.RectangleCorner2D_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestTldLearning {

	Random rand = new Random(234);
	TldConfig config = new TldConfig(true, ImageUInt8.class);

	@Test
	public void initialLearning() {

		DummyVariance variance = new DummyVariance();
		DummyFern fern = new DummyFern();
		DummyTemplate template = new DummyTemplate();
		DummyDetection detection = new DummyDetection();

		TldLearning alg = new TldLearning(rand,config,template,variance,fern,detection);


		FastQueue<ImageRectangle> regions = new FastQueue<ImageRectangle>(ImageRectangle.class,true);
		regions.grow();
		regions.grow();
		regions.grow();

		alg.initialLearning(new RectangleCorner2D_F64(10,20,30,40),regions);

		// Check to see if the variance threshold was set
		assertEquals(1,variance.calledSelect);
		// There should be a positive example
		assertEquals(1,fern.calledP);
		assertEquals(1,template.calledP);
		// several negative examples too
		assertEquals(3, fern.calledN);
		// only negative template for ambiguous, which there are none since I'm being lazy
		assertEquals(0, template.calledN);

		assertEquals(1,detection.calledDetection);
	}

	@Test
	public void updateLearning() {

		DummyVariance variance = new DummyVariance();
		DummyFern fern = new DummyFern();
		DummyTemplate template = new DummyTemplate();
		DummyDetection detection = new DummyDetection();

		TldLearning alg = new TldLearning(rand,config,template,variance,fern,detection);

		alg.updateLearning(new RectangleCorner2D_F64(10, 20, 30, 40));

		// Check to see if the variance threshold was set
		assertEquals(0,variance.calledSelect);
		// There should be a positive example
		assertEquals(1,fern.calledP);
		assertEquals(1,template.calledP);
		// several negative examples too
		assertEquals(10, fern.calledN);
		// only negative template for ambiguous, which there are none since I'm being lazy
		assertEquals(0, template.calledN);

		assertEquals(0,detection.calledDetection);
	}

	protected static class DummyFern extends TldFernClassifier {

		int calledP = 0;
		int calledN = 0;

		public DummyFern() {
			Random rand = new Random(234);
			int numFerns = 5;

			ferns = new TldFernDescription[numFerns];
			managers = new TldFernManager[numFerns];

			// create random ferns
			for( int i = 0; i < numFerns; i++ ) {
				ferns[i] = new TldFernDescription(rand,10);
				managers[i] = new TldFernManager(10);
			}
		}

		@Override
		public void learnFern(boolean positive, ImageRectangle r) {
			if( positive )
				calledP++;
			else
				calledN++;
		}

		@Override
		public void learnFernNoise(boolean positive, ImageRectangle r) {
			if( positive )
				calledP++;
			else
				calledN++;
		}

	}

	protected static class DummyVariance extends TldVarianceFilter {

		int calledSelect = 0;

		@Override
		public void selectThreshold( ImageRectangle r ) {
			calledSelect++;
		}

		@Override
		public boolean checkVariance( ImageRectangle r ) {
			return true;
		}

	}

	protected static class DummyTemplate extends TldTemplateMatching {

		int calledP = 0;
		int calledN = 0;

		@Override
		public void addDescriptor( boolean positive , float x0 , float y0 , float x1 , float y1 ) {
			if( positive )
				calledP++;
			else
				calledN++;
		}

	}

	protected static class DummyDetection extends TldDetection<ImageUInt8> {

		int calledDetection = 0;

		public DummyDetection() {
			ambiguous = false;

			for( int i = 0; i < 10; i++ ) {
				fernInfo.grow();
				fernInfo.get(i).r = new ImageRectangle();
			}
		}

		@Override
		protected void detectionCascade( FastQueue<ImageRectangle> cascadeRegions ) {
			calledDetection++;
		}
	}
}
