/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.abst.feature.dense;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.feature.dense.ConfigDenseHoG;
import boofcv.factory.feature.dense.FactoryDescribeImageDense;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDescribeImageDenseHoG {

	int width = 120;
	int height = 90;

	Random rand = new Random(234);

	ImageType imageTypes[] = new ImageType[]{
			ImageType.single(GrayU8.class), ImageType.single(GrayF32.class),
			ImageType.pl(2, ImageDataType.U8),ImageType.pl(2, ImageDataType.F32) };

	/**
	 * Checks to see if valid locations are returned.  Very simple test.  Can't test descriptor correctness since
	 * there isn't a sparse HOG yet.
	 */
	@Test
	public void checkSampleLocations() {
		ConfigDenseHoG config = new ConfigDenseHoG();
		config.fastVariant = true;
		check(config);
		config.fastVariant = false;
		check(config);
	}

	private void check(ConfigDenseHoG config) {
		int offX = config.pixelsPerCell *config.cellsPerBlockX /2;
		int offY = config.pixelsPerCell *config.cellsPerBlockY /2;
		int stride = config.pixelsPerCell *config.stepBlock;

		for( ImageType type : imageTypes ) {
			ImageBase image = type.createImage(width,height);
			GImageMiscOps.fillUniform(image,rand,0,200);

			DescribeImageDense alg = createAlg(type,config);

			alg.process(image);

			List<Point2D_I32> locations = alg.getLocations();

			assertTrue(locations.size() > 0 );
			assertEquals(locations.size(),alg.getDescriptions().size());

			for( int i = 0; i < locations.size(); i++ ) {
				Point2D_I32 p = locations.get(i);

				// see if the feature lies on a grid that's at the descriptor region's center
				assertEquals( 0 , (p.x-offX)%stride );
				assertEquals( 0 , (p.y-offY)%stride );
			}
		}
	}

	private <T extends DescribeImageDense> T createAlg( ImageType imageType , ConfigDenseHoG config ) {
		return (T) FactoryDescribeImageDense.hog(config,imageType);
	}
}