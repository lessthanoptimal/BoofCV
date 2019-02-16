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

package boofcv.alg.misc;

import boofcv.alg.misc.impl.ImplImageBandMath;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.Planar;
import boofcv.testing.CompareIdenticalFunctions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestImageBandMath extends CompareIdenticalFunctions {

	private Random rand = new Random(234);

	private int width = 15,height=20;
	private int numBands = 12;

	protected TestImageBandMath() {
		super(ImageBandMath.class, ImplImageBandMath.class);
	}

	@Test
	public void compareFunctions() {
		super.performTests(5*7);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {

		Class[] types = candidate.getParameterTypes();

		int outputIdx;
		if( candidate.getName().equals("stdDev")) {
			outputIdx = 2;
		} else {
			outputIdx = 1;
		}

		Object[][] output = new Object[2][types.length];

		for (int i = 0; i < output.length; i++) {
			Object[] param = output[i];

			param[0] = new Planar(types[outputIdx],width,height,numBands);
			param[outputIdx] = GeneralizedImageOps.createSingleBand(types[outputIdx],width,height);
			param[outputIdx+1] = 1;
			param[outputIdx+2] = numBands-2+i; // test even and off bands. this is inclusive
			if( outputIdx == 2 ) {
				param[outputIdx - 1] = GeneralizedImageOps.createSingleBand(types[outputIdx], width, height);
				GImageMiscOps.fillUniform((ImageGray)param[outputIdx - 1],rand,0,100);
			}
			GImageMiscOps.fillUniform((Planar)param[0],rand,0,100);
			GImageMiscOps.fillUniform((ImageGray)param[outputIdx],rand,0,100);
		}

		return output;
	}
}
