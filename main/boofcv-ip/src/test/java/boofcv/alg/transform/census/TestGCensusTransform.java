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

package boofcv.alg.transform.census;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.testing.CompareIdenticalFunctions;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

import static boofcv.alg.transform.census.impl.TestImplCensusTransformInner.createSamples;

/**
 * @author Peter Abeles
 */
class TestGCensusTransform extends CompareIdenticalFunctions {
	int width = 70, height = 80;
	Random rand = new Random(234);

	// Swap test and validation so that all the functions in census are compared against the 4 in GCensusTransform
	TestGCensusTransform() {
		super(CensusTransform.class, GCensusTransform.class);
	}

	@Test
	void performTests() {
		performTests(12);
	}

	@Override
	protected boolean isEquivalent(Method candidate, Method evaluation) {
		if( evaluation.getName().compareTo(candidate.getName()) != 0 )
			return false;

		Class<?> e[] = evaluation.getParameterTypes();
		Class<?> c[] = candidate.getParameterTypes();

		if( e.length != c.length )
			return false;

		for( int i = 0; i < e.length; i++ ) {
			if( !c[i].isAssignableFrom(e[i]))
				return false;
		}
		return true;
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class[] types = candidate.getParameterTypes();
		Object[] parameters = new Object[types.length];

		parameters[0] = GeneralizedImageOps.createImage(types[0], width, height, 3);
		GImageMiscOps.fillUniform((ImageBase) parameters[0], rand, 0, 100);

		if (candidate.getName().startsWith("dense")) {
			parameters[1] = GeneralizedImageOps.createSingleBand(types[1], width, height);
		} else if (candidate.getName().startsWith("sample")) {
			int r = 3;
			DogArray<Point2D_I32> samples = createSamples(r);
			parameters[1] = samples;
			parameters[2] = GeneralizedImageOps.createImage(types[2], width, height, 1);
			parameters[3] = null; // really should provide a border function
			parameters[4] = null;
		} else {
			throw new RuntimeException("Egads");
		}
		return new Object[][]{parameters};
	}
}
