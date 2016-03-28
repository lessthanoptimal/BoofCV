/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.derivative.impl;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.core.image.border.ImageBorder;
import boofcv.core.image.border.ImageBorder_S32;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.sparse.SparseImageGradient;

/**
 * @author Peter Abeles
 */
public class TestGradientSparseThree_U8 extends GeneralGradientSparse {

	public TestGradientSparseThree_U8() {
		super(GrayU8.class, GrayS16.class);
	}

	@Override
	public SparseImageGradient createAlg(ImageBorder border) {
		return new GradientSparseThree_U8((ImageBorder_S32)border);
	}

	@Override
	public ImageGradient createGradient() {
		return FactoryDerivative.three(GrayU8.class,GrayS16.class);
	}

}
