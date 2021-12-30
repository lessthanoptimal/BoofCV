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

import boofcv.abst.geo.RefineTriangulateMetricH;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.triangulate.ResidualsTriangulateProjective;
import boofcv.misc.BoofMiscOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Nonlinear least-squares triangulation.
 *
 * @author Peter Abeles
 */
public class TriangulateRefineMetricHgLS implements RefineTriangulateMetricH {

	final @Getter ResidualsTriangulateProjective func = new ResidualsTriangulateProjective();

	final @Getter UnconstrainedLeastSquares<DMatrixRMaj> minimizer;

	final DogArray<DMatrixRMaj> cameras = new DogArray<>(() -> new DMatrixRMaj(3, 4));

	final double[] param = new double[4];
	@Getter @Setter int maxIterations;
	@Getter @Setter double convergenceTol;

	public TriangulateRefineMetricHgLS( double convergenceTol,
										int maxIterations ) {
		this.convergenceTol = convergenceTol;
		this.maxIterations = maxIterations;
		minimizer = FactoryOptimization.levenbergMarquardt(null, false);
		BoofMiscOps.checkEq(4, func.getNumOfInputsN());
	}

	@Override
	public boolean process( List<Point2D_F64> observations, List<Se3_F64> listWorldToView,
							Point4D_F64 worldPt, Point4D_F64 refinedPt ) {

		cameras.resize(listWorldToView.size());
		for (int i = 0; i < cameras.size; i++) {
			PerspectiveOps.convertToMatrix(listWorldToView.get(i), cameras.get(i));
		}

		func.setObservations(observations, cameras.toList());
		minimizer.setFunction(func, null);

		param[0] = worldPt.x;
		param[1] = worldPt.y;
		param[2] = worldPt.z;
		param[3] = worldPt.w;

		minimizer.initialize(param, 0, convergenceTol*observations.size());

		for (int i = 0; i < maxIterations; i++) {
			if (minimizer.iterate())
				break;
		}

		double[] found = minimizer.getParameters();
		refinedPt.x = found[0];
		refinedPt.y = found[1];
		refinedPt.z = found[2];
		refinedPt.w = found[3];

		return true;
	}
}
