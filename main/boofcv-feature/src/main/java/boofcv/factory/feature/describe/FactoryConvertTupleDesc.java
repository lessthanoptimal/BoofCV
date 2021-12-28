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

package boofcv.factory.feature.describe;

import boofcv.abst.feature.convert.*;
import boofcv.alg.descriptor.ConvertDescriptors;
import boofcv.struct.feature.*;

/**
 * Factory for creating different types of {@link ConvertTupleDesc}, which are used for converting image region
 * descriptors.
 *
 * @author Peter Abeles
 */
public class FactoryConvertTupleDesc {

	/**
	 * Type agnostic way to create {@link ConvertTupleDesc}.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <A extends TupleDesc<A>, B extends TupleDesc<B>>
	ConvertTupleDesc<A, B> generic( ConfigConvertTupleDesc config, int numElements, Class<A> srcType ) {
		if (config.outputData == ConfigConvertTupleDesc.DataType.NATIVE) {
			if (srcType == TupleDesc_F64.class) {
				return (ConvertTupleDesc)new ConvertTupleDoNothing<>(() -> new TupleDesc_F64(numElements));
			} else if (srcType == TupleDesc_F32.class) {
				return (ConvertTupleDesc)new ConvertTupleDoNothing<>(() -> new TupleDesc_F32(numElements));
			} else if (srcType == TupleDesc_B.class) {
				return (ConvertTupleDesc)new ConvertTupleDoNothing<>(() -> new TupleDesc_B(numElements));
			} else {
				throw new IllegalArgumentException("Add support for " + srcType.getName());
			}
		}
		if (srcType == TupleDesc_F64.class) {
			return (ConvertTupleDesc)switch (config.outputData) {
				case F32 -> new ConvertTupleDesc_F64_F32(numElements);
				case U8 -> new ConvertTupleDescPositive_F64_U8(numElements);
				case S8 -> new ConvertTupleDescSigned_F64_S8(numElements);
				default -> throw new IllegalArgumentException("Unsupported conversion");
			};
		}

		throw new IllegalArgumentException("Add support for this new conversion: " +
				srcType.getSimpleName() + " -> " + config.outputData);
	}

	/**
	 * Converts two {@link boofcv.struct.feature.TupleDesc} as describe by
	 * {@link ConvertDescriptors#positive(TupleDesc_F64, TupleDesc_U8)}.
	 *
	 * @param numElements Number of elements in the descriptor
	 * @return The converter.
	 */
	public static ConvertTupleDesc<TupleDesc_F64, TupleDesc_U8> positive_F64_U8( final int numElements ) {
		return new ConvertTupleDescPositive_F64_U8(numElements);
	}

	/**
	 * Converts two {@link boofcv.struct.feature.TupleDesc} as describe by
	 * {@link ConvertDescriptors#signed(TupleDesc_F64, TupleDesc_S8)}.
	 *
	 * @param numElements Number of elements in the descriptor
	 * @return The converter.
	 */
	public static ConvertTupleDesc<TupleDesc_F64, TupleDesc_S8> real_F64_S8( final int numElements ) {
		return new ConvertTupleDescSigned_F64_S8(numElements);
	}
}
