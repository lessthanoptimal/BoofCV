/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.derivative;

import boofcv.abst.filter.ImageFunctionSparse;
import boofcv.alg.filter.derivative.DerivativeLaplacian;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.derivative.FactoryDerivativeSparse;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFactoryDerivativeSparse extends BoofStandardJUnit {

	int width = 20;
	int height = 30;

	@Test void laplacian_F32() {
		GrayF32 input = new GrayF32(width,height);
		GrayF32 expected = new GrayF32(width,height);
		GImageMiscOps.fillUniform(input, rand, 0, 20);

		DerivativeLaplacian.process(input,expected, null);

		ImageFunctionSparse<GrayF32> func = FactoryDerivativeSparse.createLaplacian(GrayF32.class,null);

		func.setImage(input);
		double found = func.compute(6,8);

		assertEquals(expected.get(6,8),found,1e-4);
	}

	@Test void laplacian_I() {
		GrayU8 input = new GrayU8(width,height);
		GrayS16 expected = new GrayS16(width,height);
		GImageMiscOps.fillUniform(input, rand, 0, 20);

		DerivativeLaplacian.process(input,expected, null);

		ImageFunctionSparse<GrayU8> func = FactoryDerivativeSparse.createLaplacian(GrayU8.class,null);

		func.setImage(input);
		double found = func.compute(6,8);

		assertEquals(expected.get(6,8),found,1e-4);
	}
}
