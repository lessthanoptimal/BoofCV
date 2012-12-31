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

package boofcv.factory.geo;

import boofcv.abst.geo.RefineTriangulationCalibrated;
import boofcv.abst.geo.RefineTriangulationEpipolar;
import boofcv.abst.geo.TriangulateNViewsCalibrated;
import boofcv.abst.geo.TriangulateTwoViewsCalibrated;
import boofcv.abst.geo.triangulate.*;

/**
 * Factory for creating algorithms for triangulating the 3D location of a point given 2 or more
 * observations of the point at different camera positions.
 *
 * @author Peter Abeles
 */
public class FactoryTriangulate {

	/**
	 * Triangulate two view by finding the intersection of two rays.
	 *
	 * @see boofcv.alg.geo.triangulate.TriangulateGeometric
	 *
	 * @return Two view triangulation algorithm
	 */
	public static TriangulateTwoViewsCalibrated twoGeometric() {
		return new WrapGeometricTriangulation();
	}

	/**
	 * Triangulate two view using the Discrete Linear Transform (DLT)
	 *
	 * @see boofcv.alg.geo.triangulate.TriangulateLinearDLT
	 *
	 * @return Two view triangulation algorithm
	 */
	public static TriangulateTwoViewsCalibrated twoDLT() {
		return new WrapTwoViewsTriangulateDLT();
	}

	/**
	 * Triangulate N views using the Discrete Linear Transform (DLT)
	 *
	 * @see boofcv.alg.geo.triangulate.TriangulateLinearDLT
	 *
	 * @return Two view triangulation algorithm
	 */
	public static TriangulateNViewsCalibrated nDLT() {
		return new WrapNViewsTriangulateDLT();
	}

	/**
	 * Triangulate two view by finding the depth of the pixel using a linear algorithm.
	 *
	 * @see boofcv.alg.geo.triangulate.PixelDepthLinear
	 *
	 * @return Two view triangulation algorithm
	 */
	public static TriangulateTwoViewsCalibrated twoLinearDepth() {
		return new WrapPixelDepthLinear();
	}

	/**
	 * Refine the triangulation using Sampson error.  Approximately takes in account epipolar constraints.
	 *
	 * @see boofcv.alg.geo.triangulate.ResidualsTriangulateSampson
	 *
	 * @param convergenceTol Tolerance for finishing optimization
	 * @param maxIterations Maximum number of allowed iterations
	 * @return Triangulation refinement algorithm.
	 */
	public static RefineTriangulationEpipolar refineSampson( double convergenceTol, int maxIterations ) {
		return new LeastSquaresTriangulateEpipolar(convergenceTol,maxIterations);
	}

	/**
	 * Refine the triangulation by computing the difference between predicted and actual pixel location.
	 * Does not take in account epipolar constraints.
	 *
	 * @see boofcv.alg.geo.triangulate.ResidualsTriangulateSimple
	 *
	 * @param convergenceTol Tolerance for finishing optimization
	 * @param maxIterations Maximum number of allowed iterations
	 * @return Triangulation refinement algorithm.
	 */
	public static RefineTriangulationCalibrated refineSimple( double convergenceTol, int maxIterations ) {
		return new LeastSquaresTriangulateCalibrated(convergenceTol,maxIterations);
	}
}
