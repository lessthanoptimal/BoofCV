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

package boofcv.factory.feature.describe;

import boofcv.abst.feature.describe.ConvertPositive_F64_U8;
import boofcv.abst.feature.describe.ConvertReal_F64_S8;
import boofcv.abst.feature.describe.ConvertTupleDesc;
import boofcv.alg.descriptor.ConvertDescriptors;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.feature.TupleDesc_S8;
import boofcv.struct.feature.TupleDesc_U8;

/**
 * Factory for creating different types of {@link ConvertTupleDesc}, which are used for converting image region
 * descriptors.
 *
 * @author Peter Abeles
 */
public class FactoryConvertTupleDesc {

	/**
	 * Converts two {@link boofcv.struct.feature.TupleDesc} as describe by
	 * {@link ConvertDescriptors#positive(TupleDesc_F64, TupleDesc_U8)}.
	 *
	 * @param numElements Number of elements in the descriptor
	 * @return The converter.
	 */
	public static ConvertTupleDesc<TupleDesc_F64,TupleDesc_U8> positive_F64_U8( final int numElements ) {
		return new ConvertPositive_F64_U8(numElements);
	}

	/**
	 * Converts two {@link boofcv.struct.feature.TupleDesc} as describe by
	 * {@link ConvertDescriptors#real(TupleDesc_F64, TupleDesc_S8)}.
	 *
	 * @param numElements Number of elements in the descriptor
	 * @return The converter.
	 */
	public static ConvertTupleDesc<TupleDesc_F64,TupleDesc_S8> real_F64_S8( final int numElements ) {
		return new ConvertReal_F64_S8(numElements);
	}

}
