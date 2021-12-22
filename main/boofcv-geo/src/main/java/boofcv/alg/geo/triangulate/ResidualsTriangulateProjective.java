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

package boofcv.alg.geo.triangulate;

import boofcv.alg.geo.PerspectiveOps;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.optimization.functions.FunctionNtoM;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Residuals for a projective triangulation where the difference between predicted and observed pixels
 * are minimized. The optimized point is in homogenous coordinates.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ResidualsTriangulateProjective implements FunctionNtoM {

	// observations of the same feature in normalized coordinates
	private List<Point2D_F64> observations;
	// Known camera motion
	private List<DMatrixRMaj> cameraMatrices;

	// 3D point in homogenous coordinates
	private final Point4D_F64 point = new Point4D_F64();
	private final Point2D_F64 predicted = new Point2D_F64();

	/**
	 * Configures inputs.
	 *
	 * @param observations Observations of the feature at different locations. Pixels.
	 * @param cameraMatrices Camera matrices
	 */
	public void setObservations( List<Point2D_F64> observations, List<DMatrixRMaj> cameraMatrices ) {
		if (observations.size() != cameraMatrices.size())
			throw new IllegalArgumentException("Different size lists");

		this.observations = observations;
		this.cameraMatrices = cameraMatrices;
	}

	@Override
	public int getNumOfInputsN() {
		return 4;
	}

	@Override
	public int getNumOfOutputsM() {
		return observations.size()*2;
	}

	@Override
	public void process( double[] input, double[] output ) {

		point.x = input[0];
		point.y = input[1];
		point.z = input[2];
		point.w = input[3];

		int outputIdx = 0;
		for (int i = 0; i < observations.size(); i++) {
			Point2D_F64 p = observations.get(i);
			DMatrixRMaj m = cameraMatrices.get(i);

			PerspectiveOps.renderPixel(m, point, predicted);

			output[outputIdx++] = predicted.x - p.x;
			output[outputIdx++] = predicted.y - p.y;
		}
	}
}
