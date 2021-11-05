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

import boofcv.struct.Configuration;

/**
 * Configuration that specifies how a {@link boofcv.struct.feature.TupleDesc} should be converted into one of
 * a different data structure
 *
 * @author Peter Abeles
 **/
public class ConfigConvertTupleDesc implements Configuration {
	/**
	 * Data structure of the output descriptor.
	 */
	public DataType outputData = DataType.NATIVE;

	public ConfigConvertTupleDesc setTo( ConfigConvertTupleDesc src ) {
		this.outputData = src.outputData;
		return this;
	}

	@Override public void checkValidity() {}

	/** Array data type for output tuple */
	public enum DataType {
		/**
		 * Use the native format of the descriptor. I.e. do nothing.
		 */
		NATIVE,
		BINARY,
		U8,
		S8,
		F32,
		F64
	}
}
