/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

/**
 * Compares function to the code in ImplImageBandMath
 *
 * @author Peter Abeles
 */
public abstract class CompareToImplImageBandMath extends CompareIdenticalFunctions {
	private final int width;
	private final int height;
	private final int numBands = 12;

	protected CompareToImplImageBandMath(  Class testClass , int width , int height ) {
		super(testClass, ImplImageBandMath.class);
		this.width = width;
		this.height = height;
	}

	@Test
	void compareFunctions() {
		super.performTests(5*7);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		if( !super.isTestMethod(m) )
			return false;

		// filter out functions for which there are no equivalents
		String name = m.getName();
		if( name.equals("checkInput"))
			return false;
		return m.getParameterTypes().length > 3;
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {

//		System.out.println(candidate.getName()+" "+candidate.getParameterTypes().length);

		Class[] types = candidate.getParameterTypes();

		int lastImage = candidate.getName().equals("stdDev") ? 2 : 1;

		Object[][] output = new Object[2][types.length];

		for (int i = 0; i < output.length; i++) {
			Object[] param = output[i];

			param[0] = new Planar(types[1],width,height,numBands);
			GImageMiscOps.fillUniform((Planar)param[0],rand,0,100);
			param[1] = GeneralizedImageOps.createSingleBand(types[1], width, height);
			param[lastImage+1] = 1;
			param[lastImage+2] = numBands-2+i; // test even and off bands. this is inclusive
			if( lastImage == 2 ) {
				param[2] = GeneralizedImageOps.createSingleBand(types[2], width, height);
				GImageBandMath.average((Planar) param[0], (ImageGray) param[2], (int) param[lastImage+1], (int) param[lastImage+2]);
				GImageMiscOps.fillUniform((ImageGray)param[2],rand,0,100);
			}
			GImageMiscOps.fillUniform((ImageGray)param[1],rand,0,100);
		}

		return output;
	}
}
