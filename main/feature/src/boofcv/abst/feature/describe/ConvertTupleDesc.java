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

package boofcv.abst.feature.describe;

import boofcv.struct.feature.TupleDesc;

/**
 * Convert between different types of {@link TupleDesc}.
 *
 * @author Peter Abeles
 */
public interface ConvertTupleDesc<A extends TupleDesc, B extends TupleDesc> {

	/**
	 * Creates a new instance of the output type.
	 *
	 * @return New instance of output data type
	 */
	public B createOutput();

	/**
	 * Converts the input descriptor into the output descriptor type.
	 *
	 * @param input Original input descriptor. Not modified.
	 * @param output Converted output descriptor. Modified.
	 */
	public void convert( A input , B output );

	/**
	 * Returns the class type of the output descriptor
	 *
	 * @return Output type
	 */
	public Class<B> getOutputType();
}
