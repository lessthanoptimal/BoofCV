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

package boofcv.alg.transform.census.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareIdenticalFunctions;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static boofcv.alg.transform.census.impl.TestImplCensusTransformInner.createSamples;
import static boofcv.alg.transform.census.impl.TestImplCensusTransformInner.samplesToIndexes;

class TestImplCensusTransformInner_MT extends CompareIdenticalFunctions {
	int width = 70,height=80;

	TestImplCensusTransformInner_MT() {
		super(ImplCensusTransformInner_MT.class, ImplCensusTransformInner.class);
	}

	@Test
	void performTests() {
		performTests(12);
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] types = candidate.getParameterTypes();
		Object[] parameters = new Object[types.length];

		parameters[0] = GeneralizedImageOps.createImage(types[0],width,height,3);
		GImageMiscOps.fillUniform((ImageBase)parameters[0],rand,0,100);

		if( candidate.getName().startsWith("dense") ) {
			parameters[1] = GeneralizedImageOps.createSingleBand(types[1],width,height);
		} else if( candidate.getName().startsWith("sample") ) {
			int r = 3;
			DogArray<Point2D_I32> samples = createSamples(r);
			DogArray_I32 indexes = samplesToIndexes((ImageGray)parameters[0],samples);
			parameters[1] = r;
			parameters[2] = indexes;
			parameters[3] = GeneralizedImageOps.createImage(types[3],width,height,1);
		} else {
			throw new RuntimeException("Egads");
		}
		return new Object[][]{parameters};
	}
}

