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

package boofcv.alg.segmentation.ms;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.segmentation.ConfigSegmentMeanShift;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSegmentMeanShift {

	GrayS32 output = new GrayS32(10,15);

	@Test
	public void uniformRegion() {
		GrayU8 image = new GrayU8(10,15);
		ImageMiscOps.fill(image,10);

		SegmentMeanShift<GrayU8> alg =
				FactorySegmentationAlg.meanShift(
						new ConfigSegmentMeanShift(2,10,10, false),ImageType.single(GrayU8.class));

		alg.process(image,output);

		assertEquals(1, alg.getNumberOfRegions());
		assertEquals(1, alg.getRegionColor().size());
		assertEquals(1, alg.getRegionSize().size());

		for( int y = 0; y < 15; y++ ) {
			for( int x = 0; x < 10; x++ ) {
				assertEquals(0, output.get(x, y));
			}
		}
		assertEquals(10,alg.getRegionColor().get(0)[0],1e-4f);
		assertEquals(10*15,alg.getRegionSize().get(0));
	}

	@Test
	public void obviousSplit() {
		GrayU8 image = new GrayU8(10,15);
		ImageMiscOps.fill(image,10);
		ImageMiscOps.fill(image.subimage(0,0,4,15,null),25);

		SegmentMeanShift<GrayU8> alg =
				FactorySegmentationAlg.meanShift(
						new ConfigSegmentMeanShift(2,10,10, false),ImageType.single(GrayU8.class));

		alg.process(image,output);

		assertEquals(2, alg.getNumberOfRegions());
		assertEquals(2, alg.getRegionColor().size());
		assertEquals(2, alg.getRegionSize().size());

		for( int y = 0; y < 15; y++ ) {
			for( int x = 0; x < 10; x++ ) {
				if( x < 4)
					assertEquals(0,output.get(x, y));
				else
					assertEquals(1,output.get(x, y));
			}
		}
		assertEquals(25,alg.getRegionColor().get(0)[0],1e-4f);
		assertEquals(10,alg.getRegionColor().get(1)[0],1e-4f);
		assertEquals(4 * 15, alg.getRegionSize().get(0));
		assertEquals(6*15,alg.getRegionSize().get(1));
	}
}
