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

import boofcv.abst.geo.RefineTriangulateProjective;
import boofcv.alg.geo.triangulate.ResidualsTriangulateProjective;
import boofcv.misc.BoofMiscOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.optimization.FactoryOptimization;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Nonlinear least-squares triangulation for projective geometry in homogenous coordinates.
 *
 * @author Peter Abeles
 */
public class TriangulateRefineProjectiveLS implements RefineTriangulateProjective {

	final @Getter ResidualsTriangulateProjective func = new ResidualsTriangulateProjective();

	final @Getter UnconstrainedLeastSquares<DMatrixRMaj> minimizer;

	final double[] param = new double[4];
	@Getter @Setter int maxIterations;
	@Getter @Setter double convergenceTol;

	public TriangulateRefineProjectiveLS( double convergenceTol, int maxIterations ) {
		this.convergenceTol = convergenceTol;
		this.maxIterations = maxIterations;
		minimizer = FactoryOptimization.levenbergMarquardt(null, false);
		BoofMiscOps.checkEq(4, func.getNumOfInputsN());
	}

	@Override
	public boolean process( List<Point2D_F64> observations, List<DMatrixRMaj> cameraMatrices,
							Point4D_F64 worldPt, Point4D_F64 refinedPt ) {
		func.setObservations(observations, cameraMatrices);
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
