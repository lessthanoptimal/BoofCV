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

package boofcv.alg.enhance;

import boofcv.BoofTesting;
import boofcv.alg.enhance.impl.ImplEnhanceHistogram;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayI;
import boofcv.testing.BoofStandardJUnit;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;
import pabeles.concurrency.GrowArray;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class TestEnhanceImageOps extends BoofStandardJUnit {

	int width = 15;
	int height = 20;
	int histogramLength = 10;

	 @Test void equalize() {
		int[] histogram = new int[]{2,2,2,2,2,0,0,0,0,0};
		int[] transform = new int[10];
		EnhanceImageOps.equalize(histogram,transform);

		assertEquals(1,transform[0]);
		assertEquals(3,transform[1]);
		assertEquals(5,transform[2]);
		assertEquals(7,transform[3]);
		assertEquals(9,transform[4]);
	}

	 @Test void equalizeLocal() {

		int numFound = 0;

		Method[] methods = EnhanceImageOps.class.getMethods();
		for( int i = 0; i < methods.length; i++ ) {
			if( methods[i].getName().compareTo("equalizeLocal") != 0 )
				continue;

			numFound++;

			Class imageType = methods[i].getParameterTypes()[0];
			GrayI input = (GrayI)GeneralizedImageOps.createSingleBand(imageType, width, height);
			GrayI output = (GrayI)GeneralizedImageOps.createSingleBand(imageType,width,height);

			equalizeLocal(input, output);

			BoofTesting.checkSubImage(this,"equalizeLocal",true,input,output);
		}

		assertEquals(2, numFound);
	}

	public void equalizeLocal(GrayI input , GrayI found ) {

		GrayI expected = (GrayI) GeneralizedImageOps.createSingleBand(input.getClass(),input.width, input.height);
		GImageMiscOps.fillUniform(input, rand, 0, 9);

		GrowArray<DogArray_I32> workArrays = new GrowArray<>(DogArray_I32::new);

		for( int radius = 1; radius < 11; radius++ ) {
			BoofTesting.callStaticMethod(ImplEnhanceHistogram.class, "equalizeLocalNaive", input, radius, histogramLength, expected, workArrays);
			BoofTesting.callStaticMethod(EnhanceImageOps.class, "equalizeLocal", input, radius, found, histogramLength,workArrays);

			BoofTesting.assertEquals(expected, found, 1e-10);
		}
	}
}
