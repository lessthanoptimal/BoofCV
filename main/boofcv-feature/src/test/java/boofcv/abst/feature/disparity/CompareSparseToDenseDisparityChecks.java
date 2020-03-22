/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.disparity;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.feature.disparity.ConfigDisparityBM;
import boofcv.factory.feature.disparity.DisparityError;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public abstract class CompareSparseToDenseDisparityChecks<T extends ImageGray<T>>
{
	Random rand = BoofTesting.createRandom(0);
	int width = 15;
	int height = 12;

	T left;
	T right;

	ImageType<T> imageType;
	DisparityError errorType;


	public CompareSparseToDenseDisparityChecks(DisparityError errorType , ImageType<T> imageType ) {
		this.errorType = errorType;
		this.imageType = imageType;

		left = imageType.createImage(width,height);
		right = imageType.createImage(width,height);

		GImageMiscOps.fillUniform(left,rand,0,200);
		GImageMiscOps.fillUniform(right,rand,0,200);
	}

	private ConfigDisparityBM createConfig() {
		ConfigDisparityBM config = new ConfigDisparityBM();
		config.errorType = errorType;
		config.disparityMin = 0;
		config.disparityRange = 15;
		config.texture = 0.15;
		config.validateRtoL = -1; // disable since it's not supported yet
		return config;
	}

	public <D extends ImageGray<D>> StereoDisparity<T,D> createDense( ConfigDisparityBM config ) {
		return FactoryStereoDisparity.blockMatch(config,imageType.getImageClass(),(Class)(config.subpixel? GrayF32.class : GrayU8.class));
	}

	public StereoDisparitySparse<T> createSparse( ConfigDisparityBM config ) {
		return FactoryStereoDisparity.sparseRectifiedBM(config,imageType.getImageClass());
	}

	@Test
	public void checkSubpixel() {
		ConfigDisparityBM config = createConfig();
		config.subpixel = false;
		compareResults(config);
	}

	@Test
	public void checkTexture() {
		ConfigDisparityBM config = createConfig();
		config.subpixel = true;
		config.texture = 0.0; // disable
		compareResults(config);
		config.subpixel = false;
		compareResults(config);
	}

	@Test
	public void checkRtoL() {
		// this isn't supported yet and a IllegalArgumentException should be thrown
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ConfigDisparityBM config = createConfig();
			config.validateRtoL = 1;
			compareResults(config);
		});
	}

	@Test
	public void checkDisparityMin() {
		ConfigDisparityBM config = createConfig();
		config.subpixel = true;
		config.disparityMin = 2;
		config.disparityRange = 9;
		compareResults(config);
		config.subpixel = false;
		compareResults(config);
	}

	@Test
	public void checkRegion() {
		ConfigDisparityBM config = createConfig();
		config.regionRadiusX = 5;
		config.regionRadiusY = 2;
		compareResults(config);
		config.regionRadiusX = 2;
		config.regionRadiusY = 5;
		compareResults(config);
	}

	public <D extends ImageGray<D>>
	void compareResults( ConfigDisparityBM config ) {
		StereoDisparity<T,D> dense = createDense(config);
		StereoDisparitySparse<T> sparse = createSparse(config);

		dense.process(left,right);
		D expected = dense.getDisparity();

		sparse.setImages(left,right);
		var found = new GrayF64(left.width,left.height);
		for (int y = 0; y < left.height; y++) {
			for (int x = 0; x < left.width; x++) {
				if( sparse.process(x,y) ) {
					found.set(x,y,sparse.getDisparity()-config.disparityMin);
				} else {
					found.set(x,y,config.disparityRange);
				}
			}
		}

//		System.out.println("--------------- Expected");
//		expected.print();
//		System.out.println("--------------- Found");
//		found.print();

		BoofTesting.assertEquals(expected,found,1e-4);
	}


}
