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

package boofcv.alg.geo.pose;

import boofcv.alg.geo.LowLevelMultiViewOps;
import boofcv.alg.geo.NormalizationPoint2D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.linsol.svd.SolveNullSpaceSvd_DDRM;
import org.ejml.interfaces.SolveNullSpace;

import java.util.List;

/**
 * Estimates a projective camera given N points, i.e. Projective N Point (PRnP). This is the projective equivalent
 * of the perspective N Point (PnP) problem. Each point consists of a 2D pixel observations and 3D
 * homogenous coordinate. Pixels are normalized to have zero means and standard deviation of 1. 3D points are
 * scaled to have a f-norm of 1. See [1] for details. Each point provides 2 linearly independent equations, requiring
 * a minimum of 6 points. More points are allowed.
 *
 * <p>WARNING: Planar surfaces are critical and it will fail on those.</p>
 *
 * <p>[1] Peter Abeles, "Scene Reconstruction Notes: BoofCV Technical Report" 2019</p>
 *
 * @author Peter Abeles
 */
public class PRnPDirectLinearTransform {

	// Used to normalize input points
	protected NormalizationPoint2D N1 = new NormalizationPoint2D();

	public SolveNullSpace<DMatrixRMaj> solverNullspace = new SolveNullSpaceSvd_DDRM();
	private DMatrixRMaj ns = new DMatrixRMaj(12, 1);

	// if true it will normalize input 3D homogenous to have f-norm of 1
	private boolean normalize3D = true;

	private DMatrixRMaj A = new DMatrixRMaj(12, 12);

	/**
	 * Computes projective camera matrix.
	 *
	 * @param worldPts points in homogenous 3D coordinates in world frame. Might be modified.
	 * @param observed pixel coordinates of points. not modified
	 * @param solutionModel (Output) 3x4 camera matrix
	 * @return true if succesfull
	 */
	public boolean process( List<Point4D_F64> worldPts, List<Point2D_F64> observed, DMatrixRMaj solutionModel ) {
		if (worldPts.size() != observed.size())
			throw new IllegalArgumentException("Number of 3D and 2D points must match");
		if (worldPts.size() < 5)
			throw new IllegalArgumentException("A minimum of 4 points are required");

		LowLevelMultiViewOps.computeNormalization(observed, N1);

		// if configured to do so normalize 3D points to have a F-norm of 1
		if (normalize3D) {
			for (int i = 0; i < worldPts.size(); i++) {
				worldPts.get(i).normalize();
			}
		}
		final int N = worldPts.size();

		A.reshape(3*N, 12);

		for (int i = 0; i < N; i++) {
			Point2D_F64 pixel = observed.get(i);
			Point4D_F64 X = worldPts.get(i);

			// apply normalization to pixels
			double x = (pixel.x - N1.meanX)/N1.stdX;
			double y = (pixel.y - N1.meanY)/N1.stdY;

			// only need to fill in non-zero elements. zeros are already zero

			// @formatter:off
			int idx = i*3*12;
			A.data[idx+4 ] = -X.x;
			A.data[idx+5 ] = -X.y;
			A.data[idx+6 ] = -X.z;
			A.data[idx+7 ] = -X.w;
			A.data[idx+8 ] = y*X.x;
			A.data[idx+9 ] = y*X.y;
			A.data[idx+10] = y*X.z;
			A.data[idx+11] = y*X.w;

			idx += 12;
			A.data[idx   ] = X.x;
			A.data[idx+1 ] = X.y;
			A.data[idx+2 ] = X.z;
			A.data[idx+3 ] = X.w;
			A.data[idx+8 ] = -x*X.x;
			A.data[idx+9 ] = -x*X.y;
			A.data[idx+10] = -x*X.z;
			A.data[idx+11] = -x*X.w;

			idx += 12;
			A.data[idx   ] = -y*X.x;
			A.data[idx+1 ] = -y*X.y;
			A.data[idx+2 ] = -y*X.z;
			A.data[idx+3 ] = -y*X.w;
			A.data[idx+4 ] = x*X.x;
			A.data[idx+5 ] = x*X.y;
			A.data[idx+6 ] = x*X.z;
			A.data[idx+7 ] = x*X.w;
			// @formatter:on
		}

		// Find the null-space, which will the camera matrix in distorted pixels
		if (!solverNullspace.process(A, 1, ns))
			return false;

		// P' = N*P
		ns.reshape(3, 4);

		// Remove the normalization
		// P = inv(N)*P'
		N1.remove(ns, solutionModel);

		return true;
	}

	public int getMinimumPoints() {
		return 6; // 2 linear constraints for each point
	}

	public boolean isNormalize3D() {
		return normalize3D;
	}

	public void setNormalize3D( boolean normalize3D ) {
		this.normalize3D = normalize3D;
	}
}
