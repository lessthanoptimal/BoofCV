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

package boofcv.alg.mvs;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.distort.PointToPixelTransform_F64;
import boofcv.struct.image.ImageDimension;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.ops.ConvertMatrixData;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Given parameters from bundle adjustment, compute all the parameters needed to compute a rectified stereo image
 * pair. The rectified image's shape is adjusted to maximize usable area, but will maintain
 * the same number of pixels as the original view.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class BundleToRectificationStereoParameters {
	/** Storage for intrinsic parameters of original distorted view-1 */
	public final CameraPinholeBrown intrinsic1 = new CameraPinholeBrown();
	/** Storage for intrinsic parameters of original distorted view-2 */
	public final CameraPinholeBrown intrinsic2 = new CameraPinholeBrown();

	/** From distorted to undistorted pixels in view-1 */
	public PixelTransform<Point2D_F64> view1_dist_to_undist;

	/** Storage for intrinsic parameters of original view without lens distortion. view-1 */
	public final DMatrixRMaj K1 = new DMatrixRMaj(3, 3);
	/** Storage for intrinsic parameters of original view without lens distortion. view-2 */
	public final DMatrixRMaj K2 = new DMatrixRMaj(3, 3);

	/** Intrinsic parameters of the rectified view-1 */
	public final DMatrixRMaj rectifiedK = new DMatrixRMaj(3, 3);

	/** The rectified image's shape. The shape is adjust to minimize wasted pixels */
	public final ImageDimension rectifiedShape = new ImageDimension();

	/** Rectification homography. Undistorted pixels to rectified pixels. view-1 F64 */
	public final DMatrixRMaj undist_to_rect1 = new DMatrixRMaj(3, 3);
	/** Rectification homography. Undistorted pixels to rectified pixels. view-2 F64 */
	public final DMatrixRMaj undist_to_rect2 = new DMatrixRMaj(3, 3);

	/** Rectification homography. Undistorted pixels to rectified pixels. view-1 F32 */
	public final FMatrixRMaj undist_to_rect1_F32 = new FMatrixRMaj(3, 3);
	/** Rectification homography. Undistorted pixels to rectified pixels. view-2 F32 */
	public final FMatrixRMaj undist_to_rect2_F32 = new FMatrixRMaj(3, 3);

	/** Rotation from original to rectified coordinate system */
	public final DMatrixRMaj rotate_orig_to_rect = new DMatrixRMaj(3, 3);

	// Used to compute rectification parameters for a stereo pair
	final RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();

	final Se3_F64 view1_to_view1 = new Se3_F64();

	/**
	 * Specifies lens parameters for view-1. This is done independently since often the same view is compared against
	 * multiple other views
	 */
	public void setView1( BundleAdjustmentCamera bundle1, int width, int height ) {
		BoofMiscOps.checkTrue(width > 0);
		BoofMiscOps.checkTrue(height > 0);

		BundleAdjustmentOps.convert(bundle1, width, height, intrinsic1);
		PerspectiveOps.pinholeToMatrix(intrinsic1, K1);

		intrinsic1.width = width;
		intrinsic1.height = height;

		Point2Transform2_F64 p_to_p = new LensDistortionBrown(intrinsic1).undistort_F64(true, true);
		view1_dist_to_undist = new PointToPixelTransform_F64(p_to_p);
	}

	/**
	 * Specifies the second view and computes rectification parameters. Must be called after {@link #setView1}.
	 *
	 * @param bundle2 (Input) Intrinsic lens distortion parameters
	 * @param view1_to_view2 (Input) Extrinsic relationship between view-1 and view-2
	 */
	public void processView2( BundleAdjustmentCamera bundle2, int width, int height , Se3_F64 view1_to_view2 ) {
		checkTrue(intrinsic1.width != 0, "Did you call setView1() Must be called first.");
		BundleAdjustmentOps.convert(bundle2, width, height, intrinsic2);
		PerspectiveOps.pinholeToMatrix(intrinsic2, K2);

		// Compute the parameters for the rectified view
		rectifyAlg.process(K1, view1_to_view1, K2, view1_to_view2);

		// rectification matrix for each image
		undist_to_rect1.setTo(rectifyAlg.getUndistToRectPixels1());
		undist_to_rect2.setTo(rectifyAlg.getUndistToRectPixels2());
		rotate_orig_to_rect.setTo(rectifyAlg.getRectifiedRotation());

		// Sanity check to see if it's bad
		BoofMiscOps.checkTrue(!MatrixFeatures_DDRM.hasUncountable(undist_to_rect1));
		BoofMiscOps.checkTrue(!MatrixFeatures_DDRM.hasUncountable(undist_to_rect2));

		// New calibration matrix,
		rectifiedK.setTo(rectifyAlg.getCalibrationMatrix());

		// Maximize the view of the left image and adjust the size of the rectified image
		RectifyImageOps.fullViewLeft(intrinsic1, undist_to_rect1, undist_to_rect2, rectifiedK, rectifiedShape);

		// undistorted and rectify images
		ConvertMatrixData.convert(undist_to_rect1, undist_to_rect1_F32);
		ConvertMatrixData.convert(undist_to_rect2, undist_to_rect2_F32);
	}
}
