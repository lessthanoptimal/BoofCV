/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.geo;

import boofcv.misc.ConfigConverge;

/**
 * @author Peter Abeles
 */
public class ConfigTriangulation {

	/**
	 * Which algorithm to use
	 */
	public Type type = Type.DLT;

	/**
	 * If an iterative technique is selected this is the convergence criteria
	 */
	public ConfigConverge optimization = new ConfigConverge(1e-8,1e-8,20);

	public ConfigTriangulation() {
	}

	public ConfigTriangulation(Type type) {
		this.type = type;
	}

	public enum Type {
		/**
		 * Discrete lienear transform
		 */
		DLT,
		/**
		 * Optimal solution for algebraic error
		 */
		ALGEBRAIC,
		/**
		 * Optimal solution for geometric error
		 */
		GEOMETRIC
	}
}
