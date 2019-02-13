/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.SequencePoint2Transform2_F32;
import georegression.geometry.UtilPoint2D_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.shapes.RectangleLength2D_F32;
import org.ejml.data.FMatrixRMaj;
import org.ejml.dense.row.CommonOps_FDRM;

import java.util.ArrayList;
import java.util.List;

/**
 * Operations related to manipulating lens distortion in images
 *
 * @author Peter Abeles
 */
public class LensDistortionOps_F32 {


	/**
	 * Creates a {@link Point2Transform2_F32} for converting pixels from original camera model into a new synthetic
	 * model.  The scaling of the image can be adjusted to ensure certain visibility requirements.
	 *
	 * @param type The type of adjustment it will apply to the transform
	 * @param paramOriginal Camera model for the current image
	 * @param paramDesired Desired camera model for the distorted image
	 * @param desiredToOriginal If true then the transform's input is assumed to be pixels in the desired
	 *                       image and the output will be in original image, if false then the reverse transform
	 *                       is returned.
	 * @param paramMod The modified camera model to meet the requested visibility requirements.  Null if you don't want it.
	 * @return The requested transform
	 */
	public static <O extends CameraPinhole, D extends CameraPinhole>
	Point2Transform2_F32 transformChangeModel(AdjustmentType type,
											  O paramOriginal,
											  D paramDesired,
											  boolean desiredToOriginal,
											  D paramMod)
	{
		LensDistortionNarrowFOV original = LensDistortionFactory.narrow(paramOriginal);
		LensDistortionNarrowFOV desired = LensDistortionFactory.narrow(paramDesired);

		Point2Transform2_F32 ori_p_to_n = original.undistort_F32(true, false);
		Point2Transform2_F32 des_n_to_p = desired.distort_F32(false, true);

		Point2Transform2_F32 ori_to_des = new SequencePoint2Transform2_F32(ori_p_to_n,des_n_to_p);

		Point2D_F32 work = new Point2D_F32();
		RectangleLength2D_F32 bound;
		if( type == AdjustmentType.FULL_VIEW ) {
			bound = DistortImageOps.boundBox_F32(paramOriginal.width, paramOriginal.height,
					new PointToPixelTransform_F32(ori_to_des),work);
		} else if( type == AdjustmentType.EXPAND) {
			bound = LensDistortionOps_F32.boundBoxInside(paramOriginal.width, paramOriginal.height,
					new PointToPixelTransform_F32(ori_to_des),work);
			// ensure there are no strips of black
			LensDistortionOps_F32.roundInside(bound);
		} else if( type == AdjustmentType.CENTER) {
			bound = LensDistortionOps_F32.centerBoxInside(paramOriginal.width, paramOriginal.height,
					new PointToPixelTransform_F32(ori_to_des),work);
		} else if( type == AdjustmentType.NONE ) {
			bound = new RectangleLength2D_F32(0,0,paramDesired.width, paramDesired.height);
		} else {
			throw new IllegalArgumentException("Unsupported type "+type);
		}

		float scaleX = bound.width/paramDesired.width;
		float scaleY = bound.height/paramDesired.height;

		float scale;

		if( type == AdjustmentType.FULL_VIEW ) {
			scale = (float)Math.max(scaleX, scaleY);
		} else if( type == AdjustmentType.EXPAND) {
			scale = (float)Math.min(scaleX, scaleY);
		} else if( type == AdjustmentType.CENTER) {
			scale = (float)Math.max(scaleX, scaleY);
		} else {
			scale = 1.0f;
		}

		float deltaX = (bound.x0 + (scaleX-scale)*paramDesired.width/ 2.0f );
		float deltaY = (bound.y0 + (scaleY-scale)*paramDesired.height/ 2.0f );

		// adjustment matrix
		FMatrixRMaj A = new FMatrixRMaj(3,3,true,scale,0,deltaX,0,scale,deltaY,0,0,1);
		FMatrixRMaj A_inv = new FMatrixRMaj(3, 3);
		if (!CommonOps_FDRM.invert(A, A_inv)) {
			throw new RuntimeException("Failed to invert adjustment matrix.  Probably bad.");
		}

		if( paramMod != null ) {
			PerspectiveOps.adjustIntrinsic(paramDesired, A_inv, paramMod);
		}

		if( desiredToOriginal ) {
			Point2Transform2_F32 des_p_to_n = desired.undistort_F32(true, false);
			Point2Transform2_F32 ori_n_to_p = original.distort_F32(false, true);
			PointTransformHomography_F32 adjust = new PointTransformHomography_F32(A);
			return new SequencePoint2Transform2_F32(adjust,des_p_to_n,ori_n_to_p);
		} else {
			PointTransformHomography_F32 adjust = new PointTransformHomography_F32(A_inv);
			return new SequencePoint2Transform2_F32(ori_to_des, adjust);
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
	public static RectangleLength2D_F32 boundBoxInside(int srcWidth, int srcHeight,
													   PixelTransform<Point2D_F32> transform,
													   Point2D_F32 work )
	{
		List<Point2D_F32> points = computeBoundingPoints(srcWidth, srcHeight, transform, work);
		Point2D_F32 center = new Point2D_F32();
		UtilPoint2D_F32.mean(points,center);

		float x0,x1,y0,y1;
		x0 = y0 = Float.MAX_VALUE;
		x1 = y1 = -Float.MAX_VALUE;

		for (int i = 0; i < points.size(); i++) {
			Point2D_F32 p = points.get(i);
			if( p.x < x0 )
				x0 = p.x;
			if( p.x > x1 )
				x1 = p.x;
			if( p.y < y0 )
				y0 = p.y;
			if( p.y > y1 )
				y1 = p.y;
		}

		x0 -= center.x;
		x1 -= center.x;
		y0 -= center.y;
		y1 -= center.y;

		float ox0 = x0;
		float oy0 = y0;
		float ox1 = x1;
		float oy1 = y1;


		for (int i = 0; i < points.size(); i++) {
			Point2D_F32 p = points.get(i);
			float dx = p.x-center.x;
			float dy = p.y-center.y;

			// see if the point is inside the box
			if( dx > x0 && dy > y0 && dx < x1 && dy < y1 ) {
				// find smallest reduction in side length and closest to original rectangle
				float d0 = (float) (float)Math.abs(dx - x0) + x0 - ox0;
				float d1 = (float) (float)Math.abs(dx - x1) + ox1 - x1;
				float d2 = (float) (float)Math.abs(dy - y0) + y0 - oy0;
				float d3 = (float) (float)Math.abs(dy - y1) + oy1 - y1;

				if ( d0 <= d1 && d0 <= d2 && d0 <= d3) {
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

		return new RectangleLength2D_F32(x0+center.x,y0+center.y,x1-x0,y1-y0);
	}

	/**
	 * Attempts to center the box inside. It will be approximately fitted too.
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static RectangleLength2D_F32 centerBoxInside(int srcWidth, int srcHeight,
														PixelTransform<Point2D_F32> transform ,
														Point2D_F32 work ) {

		List<Point2D_F32> points = computeBoundingPoints(srcWidth, srcHeight, transform, work);

		Point2D_F32 center = new Point2D_F32();
		UtilPoint2D_F32.mean(points,center);

		float x0,x1,y0,y1;
		float bx0,bx1,by0,by1;
		x0=x1=y0=y1=0;
		bx0=bx1=by0=by1=Float.MAX_VALUE;

		for (int i = 0; i < points.size(); i++) {
			Point2D_F32 p = points.get(i);
			float dx = p.x-center.x;
			float dy = p.y-center.y;
			float adx = (float)Math.abs(dx);
			float ady = (float)Math.abs(dy);

			if( adx < ady ) {
				if( dy < 0 ) {
					if( adx < by0 ) {
						by0 = adx;
						y0 = dy;
					}
 				} else {
					if( adx < by1 ) {
						by1 = adx;
						y1 = dy;
					}
				}
			} else {
				if( dx < 0 ) {
					if( ady < bx0 ) {
						bx0 = ady;
						x0 = dx;
					}
				} else {
					if( ady < bx1 ) {
						bx1 = ady;
						x1 = dx;
					}
				}

			}
		}

		return new RectangleLength2D_F32(x0+center.x,y0+center.y,x1-x0,y1-y0);
	}

	private static List<Point2D_F32> computeBoundingPoints(int srcWidth, int srcHeight,
														   PixelTransform<Point2D_F32> transform,
														   Point2D_F32 work ) {
		List<Point2D_F32> points = new ArrayList<>();

		for (int x = 0; x < srcWidth; x++) {
			transform.compute(x, 0, work);
			points.add(new Point2D_F32(work.x, work.y));
			transform.compute(x, srcHeight, work);
			points.add(new Point2D_F32(work.x, work.y));
		}

		for (int y = 0; y < srcHeight; y++) {
			transform.compute(0, y, work);
			points.add(new Point2D_F32(work.x, work.y));
			transform.compute(srcWidth, y, work);
			points.add(new Point2D_F32(work.x, work.y));
		}
		return points;
	}

	/**
	 * Adjust bound to ensure the entire image is contained inside, otherwise there might be
	 * single pixel wide black regions
	 */
	public static void roundInside( RectangleLength2D_F32 bound ) {
		float x0 = (float)Math.ceil(bound.x0);
		float y0 = (float)Math.ceil(bound.y0);
		float x1 = (float)Math.floor(bound.x0+bound.width);
		float y1 = (float)Math.floor(bound.y0+bound.height);

		bound.x0 = x0;
		bound.y0 = y0;
		bound.width = x1-x0;
		bound.height = y1-y0;
	}
}
