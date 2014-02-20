/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSegmentMeanShift {

	ImageSInt32 output = new ImageSInt32(10,15);

	@Test
	public void uniformRegion() {
		ImageUInt8 image = new ImageUInt8(10,15);
		ImageMiscOps.fill(image,10);

		SegmentMeanShift<ImageUInt8> alg =
				FactorySegmentationAlg.meanShift(
						new ConfigSegmentMeanShift(2,10,10, false),ImageType.single(ImageUInt8.class));

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
		ImageUInt8 image = new ImageUInt8(10,15);
		ImageMiscOps.fill(image,10);
		ImageMiscOps.fill(image.subimage(0,0,4,15,null),25);

		SegmentMeanShift<ImageUInt8> alg =
				FactorySegmentationAlg.meanShift(
						new ConfigSegmentMeanShift(2,10,10, false),ImageType.single(ImageUInt8.class));

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
