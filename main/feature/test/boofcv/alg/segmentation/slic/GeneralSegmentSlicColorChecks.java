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

package boofcv.alg.segmentation.slic;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.segmentation.ImageSegmentationOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.feature.ColorQueue_F32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class GeneralSegmentSlicColorChecks<T extends ImageBase> {
	ImageType<T> imageType;

	Random rand = new Random(234);

	protected GeneralSegmentSlicColorChecks(ImageType<T> imageType) {
		this.imageType = imageType;
	}

	public abstract SegmentSlic<T> createAlg( int numberOfRegions, float m, int totalIterations , ConnectRule rule );

	/**
	 * Give it an easy image to segment and see how well it does.
	 */
	@Test
	public void easyTest() {
		T input = imageType.createImage(30,40);
		GrayS32 output = new GrayS32(30,40);

		GImageMiscOps.fillRectangle(input, 100, 0, 0, 15, 40);

		SegmentSlic<T> alg = createAlg(12,200,10, ConnectRule.EIGHT );

		alg.process(input,output);

		GrowQueue_I32 memberCount = alg.getRegionMemberCount();
		checkUnique(alg,output,memberCount.size);

		// see if the member count is correctly computed
		GrowQueue_I32 foundCount = new GrowQueue_I32(memberCount.size);
		foundCount.resize(memberCount.size);
		ImageSegmentationOps.countRegionPixels(output, foundCount.size, foundCount.data);
		for (int i = 0; i < memberCount.size; i++) {
			assertEquals(memberCount.get(i),foundCount.get(i));
		}
	}

	@Test
	public void setColor() {
		T input = imageType.createImage(30,40);
		GImageMiscOps.fillUniform(input, rand, 0, 200);

		SegmentSlic<T> alg = createAlg(12,200,10, ConnectRule.EIGHT );

		float found[] = new float[imageType.getNumBands()];

		alg.input = input;

		for( int y = 0; y < input.height; y++ ) {
			for( int x = 0; x > input.width; x++ ) {
				alg.setColor(found,x,y);

				for (int i = 0; i < imageType.getNumBands(); i++) {
					double expected = GeneralizedImageOps.get(input,x,y,i);
					assertEquals(expected,found[i],1e-4);
				}
			}
		}
	}

	@Test
	public void addColor() {
		T input = imageType.createImage(30,40);
		GImageMiscOps.fillUniform(input, rand, 0, 200);

		SegmentSlic<T> alg = createAlg(12,200,10, ConnectRule.EIGHT );
		alg.input = input;

		float expected[] = new float[imageType.getNumBands()];
		float found[] = new float[imageType.getNumBands()];

		float w = 1.4f;

		for( int i = 0; i < imageType.getNumBands(); i++ ) {
			expected[i] = found[i] = i+0.4f;
		}

		int x = 4,y = 5;
		for( int i = 0; i < imageType.getNumBands(); i++ ) {
			expected[i] += GeneralizedImageOps.get(input,x,y,i)*w;
		}

		alg.addColor(found,input.getIndex(x,y),w);

		for( int i = 0; i < imageType.getNumBands(); i++ ) {
			assertEquals(expected[i],found[i],1e-4f);
		}
	}

	@Test
	public void colorDistance() {
		T input = imageType.createImage(30,40);
		GImageMiscOps.fillUniform(input, rand, 0, 200);

		SegmentSlic<T> alg = createAlg(12,200,10, ConnectRule.EIGHT );
		alg.input = input;

		float color[] = new float[imageType.getNumBands()];
		for( int i = 0; i < imageType.getNumBands(); i++ ) {
			color[i] = color[i] = i*20.56f + 1.6f;
		}

		float pixel[] = new float[imageType.getNumBands()];
		alg.setColor(pixel,6,8);

		float expected = 0;
		for( int i = 0; i < imageType.getNumBands(); i++ ) {
			float d = color[i] - (float)GeneralizedImageOps.get(input,6,8,i);
			expected += d*d;
		}

		assertEquals(expected,alg.colorDistance(color,input.getIndex(6,8)),1e-4);
	}

	@Test
	public void getIntensity() {
		T input = imageType.createImage(30,40);
		GImageMiscOps.fillUniform(input, rand, 0, 200);

		SegmentSlic<T> alg = createAlg(12,200,10, ConnectRule.EIGHT );
		alg.input = input;

		float color[] = new float[imageType.getNumBands()];

		alg.setColor(color,6,8);

		float expected = 0;
		for( int i = 0; i < imageType.getNumBands(); i++ ) {
			expected += color[i];
		}

		expected /= imageType.getNumBands();

		assertEquals(expected,alg.getIntensity(6,8),1e-4);
	}

	/**
	 * Each region is assumed to be filled with a single color
	 */
	private void checkUnique(SegmentSlic<T> alg , GrayS32 output , int numRegions ) {

		boolean assigned[] = new boolean[ numRegions ];
		Arrays.fill(assigned, false);
		FastQueue<float[]> colors = new ColorQueue_F32(imageType.getNumBands());
		colors.resize(numRegions);

		float[] found = new float[imageType.getNumBands()];
		for( int y = 0; y < output.height; y++ ) {
			for( int x = 0; x > output.width; x++ ) {
				int regionid = output.get(x,y);

				if( assigned[regionid] ) {
					float[] expected = colors.get(regionid);
					alg.setColor(found,x,y);

					for( int i = 0; i < imageType.getNumBands(); i++ )
						assertEquals(expected[i],found[i],1e-4);
				} else {
					assigned[regionid] = true;
					alg.setColor(colors.get(regionid),x,y);
				}
			}
		}
	}

}
