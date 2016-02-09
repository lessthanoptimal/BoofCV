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

package boofcv.alg.feature.dense;

import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestDescribeDenseHogAlg {


	int width = 60;
	int height = 80;

	@Test
	public void process() {
		// intentionally left blank.  This is handled by image type specific checks
	}

	@Test
	public void computeCellWeights() {
		fail("Implement");
	}

	@Test
	public void getDescriptorsInRegion() {
		fail("Implement");
	}

	@Test
	public void computeDescriptor() {
		fail("Implement");
	}

	@Test
	public void computeCells() {
		Helper helper = new Helper(10,8,3);

		helper.growCellArray(width,height);

		for (int angle = 0; angle < 360; angle++) {
			helper.angle = (float)(angle*Math.PI/180.0);

			double floatBin = ((angle+90.0)*10.0/180);
			int targetBin = (int)floatBin;
			float expected = (float)(1.0-(floatBin-targetBin));

			helper.computeCellHistograms();

			targetBin %= 10;

			for (int i = 0; i < helper.cells.length; i++) {
				DescribeDenseHogAlg.Cell c = helper.cells[i];

				for (int j = 0; j < c.histogram.length; j++) {
					if( j == targetBin ) {
						assertEquals(expected,c.histogram[j],1e-5f);
					} else if( j == (targetBin+1)%10 ) {
						assertEquals(1.0f-expected,c.histogram[j],1e-5f);
					} else {
						assertEquals(0.0f,c.histogram[j],1e-5f);
					}
				}
			}
		}
	}

	@Test
	public void getRegionWidthPixel() {
		Helper helper = new Helper(10,8,3);
		assertEquals(3*8,helper.getRegionWidthPixel());
	}

	private class Helper extends DescribeDenseHogAlg<ImageFloat32,ImageFloat32> {

		public float angle;

		public Helper(int orientationBins, int widthCell, int widthBlock) {
			super(orientationBins, widthCell, widthBlock, ImageType.single(ImageFloat32.class));
		}

		@Override
		protected void computeDerivative(int pixelIndex) {
			pixelDX = (float)Math.cos(angle);
			pixelDY = (float)Math.sin(angle);
		}
	}

}