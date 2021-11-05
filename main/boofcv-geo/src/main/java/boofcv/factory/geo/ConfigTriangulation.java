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

package boofcv.factory.geo;

import boofcv.abst.geo.triangulate.TriangulateRefineMetricLS;
import boofcv.abst.geo.triangulate.TriangulateRefineProjectiveLS;
import boofcv.alg.geo.triangulate.PixelDepthLinearMetric;
import boofcv.alg.geo.triangulate.Triangulate2ViewsGeometricMetric;
import boofcv.alg.geo.triangulate.TriangulateProjectiveLinearDLT;
import boofcv.misc.ConfigConverge;
import boofcv.struct.Configuration;

/**
 * Configuration for triangulation methods.
 *
 * @author Peter Abeles
 */
public class ConfigTriangulation implements Configuration {

	/**
	 * Which algorithm to use
	 */
	public Type type = Type.DLT;

	/**
	 * If an iterative technique is selected this is the convergence criteria
	 */
	public ConfigConverge converge = new ConfigConverge(1e-8, 1e-8, 10);

	public ConfigTriangulation() {
	}

	public ConfigTriangulation( Type type ) {
		this.type = type;
	}

	public static ConfigTriangulation DLT() {
		return new ConfigTriangulation(Type.DLT);
	}

	public static ConfigTriangulation ALGEBRAIC() {
		return new ConfigTriangulation(Type.ALGEBRAIC);
	}

	public static ConfigTriangulation GEOMETRIC() {
		return new ConfigTriangulation(Type.GEOMETRIC);
	}

	public ConfigTriangulation setTo( ConfigTriangulation src ) {
		this.type = src.type;
		this.converge.setTo(src.converge);
		return this;
	}

	@Override
	public void checkValidity() {
		converge.checkValidity();
	}

	public enum Type {
		/**
		 * Discrete lienear transform
		 *
		 * @see PixelDepthLinearMetric
		 * @see TriangulateProjectiveLinearDLT
		 */
		DLT,
		/**
		 * Optimal solution for algebraic error
		 *
		 * @see TriangulateRefineProjectiveLS
		 */
		ALGEBRAIC,
		/**
		 * Optimal solution for geometric error. Finds a solution using DLT then refines the geometric error.
		 *
		 * @see Triangulate2ViewsGeometricMetric
		 * @see TriangulateRefineMetricLS
		 */
		GEOMETRIC
	}
}
