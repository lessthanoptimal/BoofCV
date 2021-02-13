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

package boofcv.abst.feature.convert;

import boofcv.struct.feature.TupleDesc_F32;
import boofcv.struct.feature.TupleDesc_F64;

/**
 * Convert a {@link boofcv.struct.feature.TupleDesc} from double to float.
 *
 * @author Peter Abeles
 **/
public class ConvertTupleDesc_F64_F32 implements ConvertTupleDesc<TupleDesc_F64, TupleDesc_F32> {
	int numElements;

	public ConvertTupleDesc_F64_F32( int numElements ) {
		this.numElements = numElements;
	}

	@Override public TupleDesc_F32 createOutput() {
		return new TupleDesc_F32(numElements);
	}

	@Override public void convert( TupleDesc_F64 input, TupleDesc_F32 output ) {
		final int size = input.size();
		for (int i = 0; i < size; i++) {
			output.data[i] = (float)input.data[i];
		}
	}

	@Override public Class<TupleDesc_F32> getOutputType() {
		return TupleDesc_F32.class;
	}
}
