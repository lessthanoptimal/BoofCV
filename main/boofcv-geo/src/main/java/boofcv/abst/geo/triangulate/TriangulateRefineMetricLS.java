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

package boofcv.abst.geo.triangulate;

import boofcv.abst.geo.RefineTriangulateMetric;
import boofcv.alg.geo.triangulate.ResidualsTriangulateMetricSimple;
import boofcv.misc.BoofMiscOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Nonlinear least-squares triangulation.
 *
 * @author Peter Abeles
 */
public class TriangulateRefineMetricLS implements RefineTriangulateMetric {

	final @Getter ResidualsTriangulateMetricSimple func = new ResidualsTriangulateMetricSimple();

	final @Getter UnconstrainedLeastSquares<DMatrixRMaj> minimizer;

	final double[] param = new double[3];
	@Getter @Setter int maxIterations;
	@Getter @Setter double convergenceTol;

	public TriangulateRefineMetricLS( double convergenceTol,
									  int maxIterations ) {
		this.convergenceTol = convergenceTol;
		this.maxIterations = maxIterations;
		minimizer = FactoryOptimization.levenbergMarquardt(null, false);
		BoofMiscOps.checkEq(3, func.getNumOfInputsN());
	}

	@Override
	public boolean process( List<Point2D_F64> observations, List<Se3_F64> listWorldToView,
							Point3D_F64 worldPt, Point3D_F64 refinedPt ) {
		func.setObservations(observations, listWorldToView);
		minimizer.setFunction(func, null);

		param[0] = worldPt.x;
		param[1] = worldPt.y;
		param[2] = worldPt.z;

		minimizer.initialize(param, 0, convergenceTol*observations.size());

		for (int i = 0; i < maxIterations; i++) {
			if (minimizer.iterate())
				break;
		}

		double[] found = minimizer.getParameters();
		refinedPt.x = found[0];
		refinedPt.y = found[1];
		refinedPt.z = found[2];

		return true;
	}
}
