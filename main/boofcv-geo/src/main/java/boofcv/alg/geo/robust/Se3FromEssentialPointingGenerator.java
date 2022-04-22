/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.geo.Estimate1ofEpipolarPointing;
import boofcv.abst.geo.Triangulate2PointingMetricH;
import boofcv.alg.geo.DecomposeEssential;
import boofcv.struct.geo.AssociatedPair3D;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Estimates the motion between two views up to a scale factor by computing an essential matrix,
 * decomposing it, and using the positive depth constraint to select the best candidate. The returned
 * motion is the motion from the first camera frame into the second camera frame.
 *
 * @author Peter Abeles
 */
public class Se3FromEssentialPointingGenerator implements ModelGenerator<Se3_F64, AssociatedPair3D> {

	// Estimates essential matrix from observations
	Estimate1ofEpipolarPointing computeEssential;

	SelectBestStereoTransformHPointing selectBest;

	// decomposes essential matrix to extract motion
	DecomposeEssential decomposeE = new DecomposeEssential();

	DMatrixRMaj E = new DMatrixRMaj(3, 3);

	/**
	 * Specifies how the essential matrix is computed
	 *
	 * @param computeEssential Algorithm for computing the essential matrix
	 */
	public Se3FromEssentialPointingGenerator( Estimate1ofEpipolarPointing computeEssential,
											  Triangulate2PointingMetricH triangulate ) {
		this.computeEssential = computeEssential;

		selectBest = new SelectBestStereoTransformHPointing(triangulate);
	}

	/**
	 * Computes the camera motion from the set of observations. The motion is from the first
	 * into the second camera frame.
	 *
	 * @param dataSet Associated pairs in normalized camera coordinates.
	 * @param model The best pose according to the positive depth constraint.
	 */
	@Override
	public boolean generate( List<AssociatedPair3D> dataSet, Se3_F64 model ) {
		if (!computeEssential.process(dataSet, E))
			return false;

		// extract the possible motions
		decomposeE.decompose(E);
		selectBest.select(decomposeE.getSolutions(), dataSet, model);

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return computeEssential.getMinimumPoints();
	}
}
