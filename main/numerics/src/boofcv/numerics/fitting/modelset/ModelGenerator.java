/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.numerics.fitting.modelset;

import java.util.List;

/**
 * Given a set of points create a model hypothesis.  In most applications just a single hypothesis
 * will be generated.  In SFM applications geometric ambiguities can cause multiple hypotheses to be
 * created.
 *
 * @author Peter Abeles
 */
public interface ModelGenerator<Model,Point> {

	/**
	 * Creates a new instance of the model
	 *
	 * @return New model instance
	 */
	public Model createModelInstance();
	
	/**
	 * Creates a list of hypotheses from the set of sample points.
	 *
	 * @param dataSet Set of sample points.  Typically the minimum number possible.
	 * @param model Storage for generated model
	 * @return true if a model was generated, otherwise false is none were
	 */
	public boolean generate( List<Point> dataSet , Model model );

	/**
	 * The minimum number of points required to fit a data set
	 *
	 * @return Number of points.
	 */
	public int getMinimumPoints();
}
