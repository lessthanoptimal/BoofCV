/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.numerics.fitting.modelset;

import java.util.List;


/**
 * Given a set of points, compute a set of model parameters which can describe that observation.
 *
 * @author Peter Abeles
 */
public interface ModelFitter<T> {

	/**
	 * Returns the number of elements in the parameters array.
	 *
	 * @return Number of elements in the parameter array.
	 */
	int getParameterLength();

	/**
	 * Computes a set of model parameters for the given set of points.
	 *
	 * @param dataSet	Points that the model is to be fit to.
	 * @param foundParam Where the computed model parameters are written to.
	 * @return true if successful, false otherwise.
	 */
	boolean fitModel(List<T> dataSet, double[] foundParam);
}
