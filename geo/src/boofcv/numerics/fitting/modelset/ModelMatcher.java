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

package boofcv.numerics.fitting.modelset;

import java.util.List;


/**
 * <p>
 * Finds a set of points and parameters which fit a model.  Some of the points are assumed to be noise
 * and are pruned.  Different {@link ModelMatcher}s will do better jobs depending on the noise's characteristics.
 * </p>
 *
 * @param <Model> Type of model being fitted.
 * @param <Point> Type of data point being fitted.
 *
 * @author Peter Abeles
 */
public interface ModelMatcher<Model, Point> {

	/**
	 * Finds a set of points from the provided list that are a good fit for the internal model and
	 * computes the fit parameters for the model.
	 *
	 * @param dataSet Set of points (with noise) that are to be fit.
	 * @param initialGuess An initial guess for what the model parameters might be. Can be null.
	 * @return If it found a solution or not.
	 */
	public boolean process(List<Point> dataSet, Model initialGuess);

	/**
	 * Model for the match set
	 *
	 * @return model.
	 */
	public Model getModel();

	/**
	 * A set of points which match the provided parameters.
	 *
	 * @return List of points in the match set.
	 */
	public List<Point> getMatchSet();

	/**
	 * Returns the error of the matched set of points.  No guarantee is made for a larger
	 * or smaller value being better or worse.
	 *
	 * @return Error of matched set of points
	 */
	public double getError();
}
