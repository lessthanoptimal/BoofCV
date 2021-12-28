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

package boofcv.alg.distort;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.*;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.RectangleLength2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Operations related to manipulating lens distortion in images
 *
 * @author Peter Abeles
 */
public class LensDistortionOps_F64 {

	/**
	 * Creates a {@link Point2Transform2_F32} for converting pixels from original camera model into a new synthetic
	 * model. The scaling of the image can be adjusted to ensure certain visibility requirements.
	 *
	 * @param type The type of adjustment it will apply to the transform
	 * @param paramOriginal Camera model for the current image
	 * @param paramDesired Desired camera model for the distorted image
	 * @param desiredToOriginal If true then the transform's input is assumed to be pixels in the desired
	 * image and the output will be in original image, if false then the reverse transform
	 * is returned.
	 * @param paramMod The modified camera model to meet the requested visibility requirements. Null if you don't want it.
	 * @return The requested transform
	 */
	public static <O extends CameraPinhole, D extends CameraPinhole>
	Point2Transform2_F64 transformChangeModel( AdjustmentType type,
											   O paramOriginal,
											   D paramDesired,
											   boolean desiredToOriginal,
											   @Nullable D paramMod ) {
		LensDistortionNarrowFOV original = LensDistortionFactory.narrow(paramOriginal);
		LensDistortionNarrowFOV desired = LensDistortionFactory.narrow(paramDesired);

		Point2Transform2_F64 ori_p_to_n = original.undistort_F64(true, false);
		Point2Transform2_F64 des_n_to_p = desired.distort_F64(false, true);

		Point2Transform2_F64 ori_to_des = new SequencePoint2Transform2_F64(ori_p_to_n, des_n_to_p);

		Point2D_F64 work = new Point2D_F64();
		RectangleLength2D_F64 bound;
		if (type == AdjustmentType.FULL_VIEW) {
			bound = DistortImageOps.boundBox_F64(paramOriginal.width, paramOriginal.height,
					new PointToPixelTransform_F64(ori_to_des), work);
		} else if (type == AdjustmentType.EXPAND) {
			bound = LensDistortionOps_F64.boundBoxInside(paramOriginal.width, paramOriginal.height,
					new PointToPixelTransform_F64(ori_to_des), work);
			// ensure there are no strips of black
			LensDistortionOps_F64.roundInside(bound);
		} else if (type == AdjustmentType.CENTER) {
			bound = LensDistortionOps_F64.centerBoxInside(paramOriginal.width, paramOriginal.height,
					new PointToPixelTransform_F64(ori_to_des), work);
		} else if (type == AdjustmentType.NONE) {
			bound = new RectangleLength2D_F64(0, 0, paramDesired.width, paramDesired.height);
		} else {
			throw new IllegalArgumentException("Unsupported type " + type);
		}

		double scaleX = bound.width/paramDesired.width;
		double scaleY = bound.height/paramDesired.height;

		double scale;

		if (type == AdjustmentType.FULL_VIEW) {
			scale = Math.max(scaleX, scaleY);
		} else if (type == AdjustmentType.EXPAND) {
			scale = Math.min(scaleX, scaleY);
		} else if (type == AdjustmentType.CENTER) {
			scale = Math.max(scaleX, scaleY);
		} else {
			scale = 1.0;
		}

		double deltaX = (bound.x0 + (scaleX - scale)*paramDesired.width/2.0);
		double deltaY = (bound.y0 + (scaleY - scale)*paramDesired.height/2.0);

		// adjustment matrix
		DMatrixRMaj A = new DMatrixRMaj(3, 3, true, scale, 0, deltaX, 0, scale, deltaY, 0, 0, 1);
		DMatrixRMaj A_inv = new DMatrixRMaj(3, 3);
		if (!CommonOps_DDRM.invert(A, A_inv)) {
			throw new RuntimeException("Failed to invert adjustment matrix. Probably bad.");
		}

		if (paramMod != null) {
			PerspectiveOps.adjustIntrinsic(paramDesired, A_inv, paramMod);
		}

		if (desiredToOriginal) {
			Point2Transform2_F64 des_p_to_n = desired.undistort_F64(true, false);
			Point2Transform2_F64 ori_n_to_p = original.distort_F64(false, true);
			PointTransformHomography_F64 adjust = new PointTransformHomography_F64(A);
			return new SequencePoint2Transform2_F64(adjust, des_p_to_n, ori_n_to_p);
		} else {
			PointTransformHomography_F64 adjust = new PointTransformHomography_F64(A_inv);
			return new SequencePoint2Transform2_F64(ori_to_des, adjust);
		}
	}

	/**
	 * Ensures that the entire box will be inside
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static RectangleLength2D_F64 boundBoxInside( int srcWidth, int srcHeight,
														PixelTransform<Point2D_F64> transform,
														Point2D_F64 work ) {
		List<Point2D_F64> points = computeBoundingPoints(srcWidth, srcHeight, transform, work);
		Point2D_F64 center = new Point2D_F64();
		UtilPoint2D_F64.mean(points, center);

		double x0, x1, y0, y1;
		x0 = y0 = Float.MAX_VALUE;
		x1 = y1 = -Float.MAX_VALUE;

		for (int i = 0; i < points.size(); i++) {
			Point2D_F64 p = points.get(i);
			if (p.x < x0)
				x0 = p.x;
			if (p.x > x1)
				x1 = p.x;
			if (p.y < y0)
				y0 = p.y;
			if (p.y > y1)
				y1 = p.y;
		}

		x0 -= center.x;
		x1 -= center.x;
		y0 -= center.y;
		y1 -= center.y;

		double ox0 = x0;
		double oy0 = y0;
		double ox1 = x1;
		double oy1 = y1;


		for (int i = 0; i < points.size(); i++) {
			Point2D_F64 p = points.get(i);
			double dx = p.x - center.x;
			double dy = p.y - center.y;

			// see if the point is inside the box
			if (dx > x0 && dy > y0 && dx < x1 && dy < y1) {
				// find smallest reduction in side length and closest to original rectangle
				double d0 = (double)Math.abs(dx - x0) + x0 - ox0;
				double d1 = (double)Math.abs(dx - x1) + ox1 - x1;
				double d2 = (double)Math.abs(dy - y0) + y0 - oy0;
				double d3 = (double)Math.abs(dy - y1) + oy1 - y1;

				if (d0 <= d1 && d0 <= d2 && d0 <= d3) {
					x0 = dx;
				} else if (d1 <= d2 && d1 <= d3) {
					x1 = dx;
				} else if (d2 <= d3) {
					y0 = dy;
				} else {
					y1 = dy;
				}
			}
		}

		return new RectangleLength2D_F64(x0 + center.x, y0 + center.y, x1 - x0, y1 - y0);
	}

	/**
	 * Attempts to center the box inside. It will be approximately fitted too.
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static RectangleLength2D_F64 centerBoxInside( int srcWidth, int srcHeight,
														 PixelTransform<Point2D_F64> transform,
														 Point2D_F64 work ) {

		List<Point2D_F64> points = computeBoundingPoints(srcWidth, srcHeight, transform, work);

		Point2D_F64 center = new Point2D_F64();
		UtilPoint2D_F64.mean(points, center);

		double x0, x1, y0, y1;
		double bx0, bx1, by0, by1;
		x0 = x1 = y0 = y1 = 0;
		bx0 = bx1 = by0 = by1 = Double.MAX_VALUE;

		for (int i = 0; i < points.size(); i++) {
			Point2D_F64 p = points.get(i);
			double dx = p.x - center.x;
			double dy = p.y - center.y;
			double adx = Math.abs(dx);
			double ady = Math.abs(dy);

			if (adx < ady) {
				if (dy < 0) {
					if (adx < by0) {
						by0 = adx;
						y0 = dy;
					}
				} else {
					if (adx < by1) {
						by1 = adx;
						y1 = dy;
					}
				}
			} else {
				if (dx < 0) {
					if (ady < bx0) {
						bx0 = ady;
						x0 = dx;
					}
				} else {
					if (ady < bx1) {
						bx1 = ady;
						x1 = dx;
					}
				}
			}
		}

		return new RectangleLength2D_F64(x0 + center.x, y0 + center.y, x1 - x0, y1 - y0);
	}

	private static List<Point2D_F64> computeBoundingPoints( int srcWidth, int srcHeight,
															PixelTransform<Point2D_F64> transform,
															Point2D_F64 work ) {
		List<Point2D_F64> points = new ArrayList<>();

		for (int x = 0; x < srcWidth; x++) {
			transform.compute(x, 0, work);
			points.add(new Point2D_F64(work.x, work.y));
			transform.compute(x, srcHeight, work);
			points.add(new Point2D_F64(work.x, work.y));
		}

		for (int y = 0; y < srcHeight; y++) {
			transform.compute(0, y, work);
			points.add(new Point2D_F64(work.x, work.y));
			transform.compute(srcWidth, y, work);
			points.add(new Point2D_F64(work.x, work.y));
		}
		return points;
	}

	/**
	 * Adjust bound to ensure the entire image is contained inside, otherwise there might be
	 * single pixel wide black regions
	 */
	public static void roundInside( RectangleLength2D_F64 bound ) {
		double x0 = Math.ceil(bound.x0);
		double y0 = Math.ceil(bound.y0);
		double x1 = Math.floor(bound.x0 + bound.width);
		double y1 = Math.floor(bound.y0 + bound.height);

		bound.x0 = x0;
		bound.y0 = y0;
		bound.width = x1 - x0;
		bound.height = y1 - y0;
	}
}
