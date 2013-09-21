/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.fft;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFourierTransformNaive {

	@Test
	public void forwardsRRI_inverseRIR() {
		float original[] = new float[]{0.5f,2f,-0.34f,5f,6f,2f,10f,10f,10f,0f,-0.4f,-6f};
		float tranImag[] = new float[original.length];
		float tranReal[] = new float[original.length];
		float found[] = new float[original.length];

		FourierTransformNaive.forward(original,tranReal,tranImag,0,original.length);
		FourierTransformNaive.inverse(tranReal, tranImag, found, 0, original.length);

		for( int i = 0; i < original.length; i++ ) {
			assertEquals(original[i],found[i],1e-4);
		}
	}

	@Test
	public void forwardsRIRI_inverseRIRI() {
		float originalR[] = new float[]{0.5f,2f,-0.34f,5f,6f,2f,10f,10f,10f,0f,-0.4f,-6f};
		float originalI[] = new float[]{-0.5f,1.5f,-3f,1.5f,3.5f,-0.6f,-4f,4f,3f,-2f,-3f,2.5f};
		float tranImag[] = new float[originalR.length];
		float tranReal[] = new float[originalR.length];
		float foundR[] = new float[originalR.length];
		float foundI[] = new float[originalR.length];

		FourierTransformNaive.forward(originalR,originalI,tranReal,tranImag,0,originalR.length);
		FourierTransformNaive.inverse(tranReal, tranImag, foundR,foundI, 0, originalR.length);

		for( int i = 0; i < originalR.length; i++ ) {
			assertEquals(originalR[i],foundR[i],1e-4);
			assertEquals(originalI[i],foundI[i],1e-4);
		}
	}

}
