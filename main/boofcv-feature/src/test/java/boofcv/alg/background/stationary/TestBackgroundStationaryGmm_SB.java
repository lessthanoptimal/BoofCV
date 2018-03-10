/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.background.stationary;

import boofcv.alg.background.BackgroundModelStationary;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Peter Abeles
 */
public class TestBackgroundStationaryGmm_SB extends GenericBackgroundModelStationaryChecks {

	ImageType<GrayU8> imageType = ImageType.single(GrayU8.class);

	public TestBackgroundStationaryGmm_SB() {
		imageTypes.add(ImageType.single(GrayU8.class));
		imageTypes.add(ImageType.single(GrayF32.class));
	}

	@Override
	public <T extends ImageBase<T>> BackgroundModelStationary<T> create(ImageType<T> imageType) {
		return new BackgroundStationaryGmm_SB(1000.0f,0.001f,10,imageType);
	}

	@Test
	public void updateMixture() {
		int maxGaussians = 5;

		BackgroundStationaryGmm_SB alg =
				new BackgroundStationaryGmm_SB(1000,0.001f,maxGaussians,imageType);
		alg.setSignificantWeight(1e-4f);

		int startIndex = 24;
		float data[] = new float[50];

		// No models. it should create one
		alg.updateMixture(50,data,startIndex);
		assertTrue(data[startIndex]>0);
		assertEquals(0,data[startIndex+3],1e-4f);

		// add another model
		alg.updateMixture(150,data,startIndex);
		assertTrue(data[startIndex+3]>0);
		assertEquals(0,data[startIndex+6],1e-4f);


		// give it another observations and see if the updates move in the expected direction
		float oldWeight0 = data[startIndex];
		float oldWeight1 = data[startIndex+3];
		float oldVar0 = data[startIndex+1];
		float oldVar1 = data[startIndex+4];

		alg.updateMixture(51,data,startIndex);
		assertTrue(data[startIndex]>oldWeight0);
		assertTrue(data[startIndex+3]<oldWeight1);
		assertTrue(data[startIndex+1]<oldVar0);
		assertEquals(oldVar1,data[startIndex+4],1e-4f);
		assertEquals(0,data[startIndex+6],1e-4f);
	}

	@Test
	public void updateWeightAndPrune() {
		int maxGaussians = 5;

		BackgroundStationaryGmm_SB alg =
				new BackgroundStationaryGmm_SB(1000,0.001f,maxGaussians,imageType);
		alg.setSignificantWeight(1e-4f);

		int startIndex = 24;
		float data[] = new float[50];
		// Zero gaussians - nothing should happen
		alg.updateWeightAndPrune(data,startIndex,0,-1,0);
		assertEquals(0,data[startIndex],1e-4f);

		// Max gaussians and no best model
		for (int i = 0; i < maxGaussians; i++) {
			data[startIndex+i*3+0] = 1.0f;
			data[startIndex+i*3+1] = 3;
			data[startIndex+i*3+2] = i*10;
		}
		alg.updateWeightAndPrune(data,startIndex,maxGaussians,-1,0);
		float w = 1f/maxGaussians;
		for (int i = 0; i < maxGaussians; i++) {
			assertEquals(w,data[startIndex+i*3],1e-4f);
		}

		// Make sure best weight isn't change and sums up to one
		alg.updateWeightAndPrune(data,startIndex,maxGaussians,startIndex+1*3,0.9f);
		float sum = 0;
		for (int i = 0; i < maxGaussians; i++) {
			sum += data[startIndex+i*3];
		}
		assertEquals(1f,sum,1e-4f);
		assertEquals(0.9f/(w*4+0.9f),data[startIndex+1*3],4e-4f);

		// Prune a model and have the best model be at the end so that it gets moved
		for (int i = 0; i < maxGaussians; i++) {
			data[startIndex + i * 3] = w;
		}
		data[startIndex + 2*3] = -0.01f;
		alg.updateWeightAndPrune(data,startIndex,maxGaussians,startIndex+4*3,0.9f);
		assertEquals(0,data[startIndex + 4 * 3+1],1e-4f); // last gaussian should be marked as unused
		sum = 0;
		for (int i = 0; i < 4; i++) {
			sum += data[startIndex+i*3];
		}
		assertEquals(1f,sum,1e-4f);
	}

	@Test
	public void checkBackground() {
		int maxGaussians = 5;

		BackgroundStationaryGmm_SB alg =
				new BackgroundStationaryGmm_SB(1000,0.001f,maxGaussians,imageType);
		alg.setSignificantWeight(1e-4f);

		int startIndex = 24;
		float data[] = new float[50];

		// there is no data and variance is zero on the first one
		assertFalse(alg.checkBackground(0,data,startIndex));

		// Give it a few models
		for (int i = 0; i < maxGaussians; i++) {
			data[startIndex+i*3+0] = 1.0f/maxGaussians;
			data[startIndex+i*3+1] = 3;
			data[startIndex+i*3+2] = i*10;
		}
		assertTrue(alg.checkBackground(0,data,startIndex));
		assertTrue(alg.checkBackground(30,data,startIndex));
		// negative will be way outside the range
		assertFalse(alg.checkBackground(200,data,startIndex));

		// set the wait to be so low it won't be considered to be the background
		data[startIndex+3*3+0] = 1e-7f;
		assertFalse(alg.checkBackground(30,data,startIndex));
	}
}