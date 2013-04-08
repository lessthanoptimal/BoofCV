package boofcv.abst.sfm.d2;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.InvertibleTransform;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestMsToGrayMotion2D {

	@Test
	public void basicTests() {
		Dummy child = new Dummy();
		MsToGrayMotion2D alg = new MsToGrayMotion2D(child, ImageFloat32.class);

		MultiSpectral<ImageFloat32> ms = new MultiSpectral<ImageFloat32>(ImageFloat32.class,20,30,3);

		assertTrue(alg.process(ms));
		assertTrue(child.input != null );
		assertEquals(0, child.numReset);
		assertEquals(0, child.numSetToFirst);
		alg.reset();
		assertEquals(1, child.numReset);
		assertEquals(0, child.numSetToFirst);
		alg.setToFirst();
		assertEquals(1, child.numReset);
		assertEquals(1, child.numSetToFirst);
	}


	protected class Dummy implements ImageMotion2D {

		ImageBase input;
		int numReset = 0;
		int numSetToFirst = 0;

		@Override
		public boolean process(ImageBase input) {
			this.input = input;
			return true;
		}

		@Override
		public void reset() {
			numReset++;
		}

		@Override
		public void setToFirst() {
			numSetToFirst++;
		}

		@Override
		public InvertibleTransform getFirstToCurrent() {
			return new Se3_F64();
		}

		@Override
		public Class getTransformType() {
			return Se3_F64.class;
		}
	}
}
