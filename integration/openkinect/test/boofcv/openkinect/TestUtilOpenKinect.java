package boofcv.openkinect;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageUInt16;
import org.ddogleg.struct.GrowQueue_I8;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestUtilOpenKinect {

	int width = 320;
	int height = 240;
	Random rand = new Random(234);

	@Test
	public void saveDepth_parseDepth() throws IOException {

		ImageUInt16 depth = new ImageUInt16(width,height);
		ImageMiscOps.fillUniform(depth,rand,0,10000);
		GrowQueue_I8 data = new GrowQueue_I8();
		ImageUInt16 found = new ImageUInt16(width,height);

		UtilOpenKinect.saveDepth(depth, "temp.depth", data);


		UtilOpenKinect.parseDepth("temp.depth",found,data);

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {

				int a = depth.get(j,i);
				int b = found.get(j,i);

				assertEquals(a,b);
			}
		}

		// clean up
		File f = new File("temp.depth");
		assertTrue(f.delete());
	}

}
