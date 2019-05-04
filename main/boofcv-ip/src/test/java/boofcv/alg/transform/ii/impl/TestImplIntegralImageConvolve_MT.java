/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.ii.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.ii.IntegralKernel;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

class TestImplIntegralImageConvolve_MT extends CompareIdenticalFunctions  {
	Random rand = new Random(234);
	int width = 40;
	int height = 60;

	IntegralKernel kernelII = new IntegralKernel(2);

	TestImplIntegralImageConvolve_MT() {
		super(ImplIntegralImageConvolve_MT.class, ImplIntegralImageConvolve.class);

		kernelII.blocks[0] = new ImageRectangle(-2,-2,1,1);
		kernelII.blocks[1] = new ImageRectangle(-2,-1,1,0);
		kernelII.scales = new int[]{1,1};
	}

	@Test
	void performTests() {
		performTests(8);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] params = candidate.getParameterTypes();

		if( params.length == 3 ) {
			ImageGray inputII = GeneralizedImageOps.createSingleBand(params[0], width, height);
			GImageMiscOps.fillUniform(inputII,rand,0,1000);
			ImageGray found = GeneralizedImageOps.createSingleBand(params[2], width, height);
			return new Object[][]{{inputII,kernelII,found}};
		} else {
			ImageGray inputII = GeneralizedImageOps.createSingleBand(params[0], width, height);
			GImageMiscOps.fillUniform(inputII,rand,0,1000);
			ImageGray found = GeneralizedImageOps.createSingleBand(params[2], width, height);
			return new Object[][]{{inputII,kernelII,found,4,5}};
		}
	}
}

