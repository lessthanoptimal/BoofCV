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

package boofcv.alg.background;

import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.junit.Test;

import java.util.Random;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestBackgroundGmmCommon {

	Random rand = new Random(234);

	ImageType imageType = ImageType.single(GrayU8.class);

	/**
	 * Alternates between two values and see if two stable gaussians form
	 */
	@Test
	public void createTwoModels() {
		int maxGaussians = 2;

		BackgroundGmmCommon alg = new BackgroundGmmCommon(1000,0.0f,maxGaussians,imageType);
		alg.setSignificantWeight(1e-4f);
		alg.setMaxDistance(5);
		alg.setInitialVariance(12);

		int startIndex = 24;
		float data[] = new float[50];

		float stdev = 10f;
		float variance = stdev*stdev;

		for (int i = 0; i < 100000; i++) {
			float pixelValue = i%2==0?10 : 100;

			float adjusted = pixelValue + (float)(rand.nextGaussian()*stdev);
			if( Math.abs(pixelValue-adjusted) > 3*stdev ) {
				adjusted = pixelValue;
			}
			alg.updateMixture(adjusted,data,startIndex);
		}
		int ng = 0;
		for( ;ng <maxGaussians; ng++ ) {
			if( 0 == data[startIndex+ng*3+1])
				break;
		}

		// there should be just two pixtures
		assertEquals(2,ng);

		float weight0 = data[startIndex];
		float weight1 = data[startIndex+3];
		float variance0 = data[startIndex+1];
		float variance1 = data[startIndex+3+1];
		float mean0 = data[startIndex+2];
		float mean1 = data[startIndex+3+2];

		assertEquals(10,mean0,0.5);
		assertEquals(100,mean1,0.5);

		assertEquals(0.5f,weight0,0.2);
		assertEquals(0.5f,weight1,0.2);

		float varianceTol = variance/4;
		assertEquals(variance,variance0,varianceTol);
		assertEquals(variance,variance1,varianceTol);

	}

	@Test
	public void updateMixture() {
		int maxGaussians = 5;

		BackgroundGmmCommon alg = new BackgroundGmmCommon(1000,0.001f,maxGaussians,imageType);
		alg.setSignificantWeight(1e-4f);
		alg.unknownValue = 5;

		int startIndex = 24;
		float data[] = new float[50];

		// No models. it should create one
		assertEquals(5,alg.updateMixture(50,data,startIndex));
		assertTrue(data[startIndex]>0);
		assertEquals(0,data[startIndex+3],1e-4f);

		// add another model
		assertEquals(1,alg.updateMixture(150,data,startIndex));
		assertTrue(data[startIndex+3]>0);
		assertEquals(0,data[startIndex+6],1e-4f);

		// give it another observations and see if the updates move in the expected direction
		float oldWeight0 = data[startIndex];
		float oldWeight1 = data[startIndex+3];
		float oldVar0 = data[startIndex+1];
		float oldVar1 = data[startIndex+4];

		assertEquals(0,alg.updateMixture(51,data,startIndex));
		assertTrue(data[startIndex]>oldWeight0);
		assertTrue(data[startIndex+3]<oldWeight1);
		assertTrue(data[startIndex+1]<oldVar0);
		assertEquals(oldVar1,data[startIndex+4],1e-4f);
		assertEquals(0,data[startIndex+6],1e-4f);
	}

	@Test
	public void updateWeightAndPrune() {
		int maxGaussians = 5;

		BackgroundGmmCommon alg = new BackgroundGmmCommon(1000,0.001f,maxGaussians,imageType);
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

		BackgroundGmmCommon alg = new BackgroundGmmCommon(1000,0.001f,maxGaussians,imageType);
		alg.unknownValue=2;
		alg.setSignificantWeight(1e-4f);

		int startIndex = 24;
		float data[] = new float[50];

		// there is no data and variance is zero on the first one
		assertEquals(2,alg.checkBackground(0,data,startIndex));

		// Give it a few models
		for (int i = 0; i < maxGaussians; i++) {
			data[startIndex+i*3+0] = 1.0f/maxGaussians;
			data[startIndex+i*3+1] = 3;
			data[startIndex+i*3+2] = i*10;
		}
		assertEquals(0,alg.checkBackground(0,data,startIndex));
		assertEquals(0,alg.checkBackground(30,data,startIndex));
		// negative will be way outside the range
		assertEquals(1,alg.checkBackground(200,data,startIndex));

		// set the wait to be so low it won't be considered to be the background
		data[startIndex+3*3+0] = 1e-7f;
		assertEquals(1,alg.checkBackground(30,data,startIndex));
	}
}
