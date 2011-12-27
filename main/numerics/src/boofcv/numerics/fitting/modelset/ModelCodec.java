/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.numerics.fitting.modelset;


/**
 * Used to convert a model to and from an array parameterized format.
 *
 * @author Peter Abeles
 */
public interface ModelCodec<T> {

	/**
	 * Converts the parameter array into a model.
	 *
	 * @param param input model parameters.
	 * @param outputModel Option (can be null) model.
	 * @return Converted model
	 */
	T decode( double param[] , T outputModel );

	/**
	 * Converts the provided model into the array format.
	 *
	 * @param model Input model.
	 * @param param Output parameterized model. Can be null.
	 * @return parameterized model.
	 */
	double[] encode( T model , double param[] );

	/**
	 * Number of elements in array encoded parameters.
	 */
	int getParamLength();
}
