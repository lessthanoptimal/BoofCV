/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.robust;

import java.util.List;

public interface ModelTestingInterface<Model, Point> {

	/**
	 * Creates a random model
	 */
	public Model createRandomModel();

	/**
	 * Creates a random point that fits the provided model
	 */
	public Point createRandomPointFromModel( Model model );

	/**
	 * Checks to see of the dat set are described by the model correctly
	 */
	public boolean doPointsFitModel( Model model , List<Point> dataSet );
}
