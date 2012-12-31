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

package boofcv.abst.filter.derivative;

import boofcv.abst.filter.ImageFunctionSparse;
import boofcv.alg.filter.derivative.LaplacianEdge;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.derivative.FactoryDerivativeSparse;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestFactoryDerivativeSparse {

	Random rand = new Random(234);

	int width = 20;
	int height = 30;

	@Test
	public void laplacian_F32() {
		ImageFloat32 input = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);
		GImageMiscOps.fillUniform(input, rand, 0, 20);

		LaplacianEdge.process(input,expected);

		ImageFunctionSparse<ImageFloat32> func = FactoryDerivativeSparse.createLaplacian(ImageFloat32.class,null);

		func.setImage(input);
		double found = func.compute(6,8);

		assertEquals(expected.get(6,8),found,1e-4);
	}

	@Test
	public void laplacian_I() {
		ImageUInt8 input = new ImageUInt8(width,height);
		ImageSInt16 expected = new ImageSInt16(width,height);
		GImageMiscOps.fillUniform(input, rand, 0, 20);

		LaplacianEdge.process(input,expected);

		ImageFunctionSparse<ImageUInt8> func = FactoryDerivativeSparse.createLaplacian(ImageUInt8.class,null);

		func.setImage(input);
		double found = func.compute(6,8);

		assertEquals(expected.get(6,8),found,1e-4);
	}
}
