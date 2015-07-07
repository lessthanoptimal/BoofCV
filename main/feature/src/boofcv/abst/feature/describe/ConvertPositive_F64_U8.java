/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.describe;

import boofcv.alg.descriptor.ConvertDescriptors;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.feature.TupleDesc_U8;

/**
 * Converts two types of region descriptors.
 *
 * @see ConvertDescriptors#positive(boofcv.struct.feature.TupleDesc_F64, boofcv.struct.feature.TupleDesc_U8)
 *
 * @author Peter Abeles
 */
public class ConvertPositive_F64_U8 implements ConvertTupleDesc<TupleDesc_F64, TupleDesc_U8> {

	int numElements;

	public ConvertPositive_F64_U8(int numElements) {
		this.numElements = numElements;
	}

	@Override
	public TupleDesc_U8 createOutput() {
		return new TupleDesc_U8(numElements);
	}

	@Override
	public void convert(TupleDesc_F64 input, TupleDesc_U8 output) {
		ConvertDescriptors.positive(input, output);
	}

	@Override
	public Class<TupleDesc_U8> getOutputType() {
		return TupleDesc_U8.class;
	}
}
