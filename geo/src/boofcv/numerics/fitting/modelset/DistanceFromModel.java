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
 * Computes the distance a sample point is from the provided model.
 * </p>
 *
 * <p>
 * Example: If the model is a rigid body transformation then there are 6 model parameters,
 * 3 for rotation and 3 for translation.  The sample point is the initial location of a point
 * in 3D space before the transform has been applied and the observed location after the transform
 * has been applied.
 * </p>
 * @author Peter Abeles
 */
public interface DistanceFromModel<Model, Point> {

	/**
	 * Sets the model parameters.
	 *
	 * @param model Model parameters.
	 */
	public void setModel(Model model);

	/**
	 * Computes the distance the point is from the model.
	 *
	 * @param pt Point being evaluated. Not modified.
	 * @return Distance the point is from the model.
	 */
	public double computeDistance(Point pt);

	/**
	 * Computes the distance a set of points is from the model and saves the results
	 * in the provided array.
	 *
	 * @param points   Set of points which are to be evaluated.
	 * @param distance Where model distance is stored.
	 */
	public void computeDistance(List<Point> points, double distance[]);
}
