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

package boofcv.examples.sfm;

import boofcv.alg.geo.MultiViewOps;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.NormOps_DDRM;

import java.util.List;

/**
 * Shows a few example uses of the trifocal tensor.
 */
public class ExampleTrifocalTensorUses {
	public static void main( String[] args ) {
		// Load three images/views
		String name = "rock_leaves_";
		GrayU8 gray01 = UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "01.jpg"), GrayU8.class);
		GrayU8 gray02 = UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "02.jpg"), GrayU8.class);
		GrayU8 gray03 = UtilImageIO.loadImage(UtilIO.pathExample("triple/" + name + "03.jpg"), GrayU8.class);

		// Get the trifocal tensor for these three images
		var tensor = new TrifocalTensor();
		List<AssociatedTriple> inliers = ExampleComputeTrifocalTensor.imagesToTrifocal(gray01, gray02, gray03, tensor);
		// The inlier set from robustly fitting a trifocal tensor is one of its more helpful uses.
		// It has fewer degenerate situations than a straight forward application of fundamental/essential matrices.
		// Unlike with two views where you find the distance from the epipolar line, if you use three views there is
		// a unique pixel in each view and that will improve the efficiency of many SFM applications. Yes, if you
		// know what you are doing it's possible to use multiple stereo pairs. However, most people do that incorrectly
		// which will yield worse results.

		// Trifocal tensor to 3 compatible camera matrices
		// Camera matrix for view-1, P1, is going to be identity
		DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
		DMatrixRMaj P3 = new DMatrixRMaj(3, 4);
		MultiViewOps.trifocalToCameraMatrices(tensor, P2, P3);
		// These camera matrices are useful if doing a projective reconstruction.

		// One thing that you can do with a trifocal tensor is transfer points from one view onto another
		// Similar to what you would do with a homography.
		AssociatedTriple match = inliers.get(4);
		Point3D_F64 predicted3 = new Point3D_F64(); // pixel, but in homogenous coordinates
		MultiViewOps.transfer_1_to_3(tensor, match.p1, match.p2, predicted3);

		System.out.printf("Predicted x3=(%.1f, %.1f)  actual=(%.1f, %.1f)\n",
				predicted3.x/predicted3.z, predicted3.y/predicted3.z, match.p3.x, match.p3.y);

		// You can get two fundamental matrices from the trifocal tensor
		DMatrixRMaj F21 = new DMatrixRMaj(3, 3);
		DMatrixRMaj F31 = new DMatrixRMaj(3, 3);
		MultiViewOps.trifocalToFundamental(tensor, F21, F31);

		// Scale is arbitrary so let's make it norm of 1
		CommonOps_DDRM.divide(F21, NormOps_DDRM.normF(F21));
		CommonOps_DDRM.divide(F31, NormOps_DDRM.normF(F31));

		// Demonstration the epipolar constraint works here. This should be close to zero
		System.out.println("x2'*F21*X1 = " + MultiViewOps.constraint(F21, match.p1, match.p2));
		System.out.println("x3'*F31*X1 = " + MultiViewOps.constraint(F31, match.p1, match.p3));

		// For examples of how a trifocal tensor can be used in self calibration see
		// ExampleTrifocalStereoUncalibrated
	}
}
