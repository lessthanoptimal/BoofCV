/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle;

import boofcv.abst.geo.bundle.BundleAdjustmentSchur_DSCC;
import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.DMatrixSparseTriplet;
import org.ejml.data.IGrowArray;
import org.ejml.ops.DConvertMatrixStruct;
import org.ejml.sparse.csc.CommonOps_DSCC;

/**
 * Computes the Jacobian for {@link BundleAdjustmentSchur_DSCC} using sparse matrices
 * in EJML. Parameterization is done using the format in {@link CodecSceneStructureMetric}.
 *
 * @author Peter Abeles
 */
public class BundleAdjustmentMetricSchurJacobian_DSCC
		extends BundleAdjustmentMetricSchurJacobian<DMatrixSparseCSC> {
	DMatrixSparseTriplet leftTriplet = new DMatrixSparseTriplet(1, 1, 1);
	DMatrixSparseTriplet rightTriplet = new DMatrixSparseTriplet(1, 1, 1);
	IGrowArray work = new IGrowArray();

	@Override
	public void process( double[] input, DMatrixSparseCSC left, DMatrixSparseCSC right ) {
		internalProcess(input, leftTriplet, rightTriplet);

		DConvertMatrixStruct.convert(leftTriplet, left, work);
		DConvertMatrixStruct.convert(rightTriplet, right, work);

		// There is no good way to do an element-wise add in these sparse data structures. What it does here is
		// allow you to construct an invalid matrix with too many elements with the same coordinate. Then below
		// it will find those duplicates and add them all together. This situation is actually not common and requires
		// the same motion to be referenced by multiple elements in a chain of relative views.
		// NOTE: If profiling shows this to be a bottleneck it's possible to detect this rare situation and skip this
		//       step entirely.
		CommonOps_DSCC.duplicatesAdd(left, work);
		CommonOps_DSCC.duplicatesAdd(right, work);
	}

	@Override
	protected void set( DMatrix matrix, int row, int col, double value ) {
		((DMatrixSparseTriplet)matrix).addItem(row, col, value);
	}

	@Override
	protected void add( DMatrix matrix, int row, int col, double value ) {
		((DMatrixSparseTriplet)matrix).addItem(row, col, value);
	}
}
