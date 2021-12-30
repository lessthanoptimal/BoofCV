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

import boofcv.struct.ImageRectangle_F32;
import boofcv.struct.ImageRectangle_F64;
import boofcv.struct.distort.PixelTransform;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.RectangleLength2D_F32;
import georegression.struct.shapes.RectangleLength2D_F64;
import georegression.struct.shapes.RectangleLength2D_I32;

/**
 * <p>
 * Provides common function for distorting images.
 * </p>
 *
 * @author Peter Abeles
 */
public class DistortImageOps {

	/**
	 * Finds an axis-aligned bounding box which would contain a image after it has been transformed.
	 * A sanity check is done to made sure it is contained inside the destination image's bounds.
	 * If it is totally outside then a rectangle with negative width or height is returned.
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param dstWidth Width of the destination image
	 * @param dstHeight Height of the destination image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static RectangleLength2D_I32 boundBox( int srcWidth, int srcHeight,
												  int dstWidth, int dstHeight,
												  Point2D_F32 work,
												  PixelTransform<Point2D_F32> transform ) {
		RectangleLength2D_I32 ret = boundBox(srcWidth, srcHeight, work, transform);

		int x0 = ret.x0;
		int y0 = ret.y0;
		int x1 = ret.x0 + ret.width;
		int y1 = ret.y0 + ret.height;

		if (x0 < 0) x0 = 0;
		if (x1 > dstWidth) x1 = dstWidth;
		if (y0 < 0) y0 = 0;
		if (y1 > dstHeight) y1 = dstHeight;

		return new RectangleLength2D_I32(x0, y0, x1 - x0, y1 - y0);
	}

	/**
	 * Finds an axis-aligned bounding box which would contain a image after it has been transformed.
	 * The returned bounding box can be larger then the original image.
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static RectangleLength2D_I32 boundBox( int srcWidth, int srcHeight,
												  Point2D_F32 work,
												  PixelTransform<Point2D_F32> transform ) {
		int x0, y0, x1, y1;

		transform.compute(0, 0, work);
		x0 = x1 = (int)work.x;
		y0 = y1 = (int)work.y;

		for (int i = 1; i < 4; i++) {
			if (i == 1)
				transform.compute(srcWidth, 0, work);
			else if (i == 2)
				transform.compute(0, srcHeight, work);
			else if (i == 3)
				transform.compute(srcWidth - 1, srcHeight, work);

			if (work.x < x0)
				x0 = (int)work.x;
			else if (work.x > x1)
				x1 = (int)work.x;
			if (work.y < y0)
				y0 = (int)work.y;
			else if (work.y > y1)
				y1 = (int)work.y;
		}

		return new RectangleLength2D_I32(x0, y0, x1 - x0, y1 - y0);
	}

	/**
	 * Finds an axis-aligned bounding box which would contain a image after it has been transformed.
	 * The returned bounding box can be larger then the original image.
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static RectangleLength2D_F32 boundBox_F32( int srcWidth, int srcHeight,
													  PixelTransform<Point2D_F32> transform,
													  Point2D_F32 transformed ) {
		ImageRectangle_F32 r = new ImageRectangle_F32();

		r.x0 = r.y0 = Float.MAX_VALUE;
		r.x1 = r.y1 = -Float.MAX_VALUE;

		for (int y = 0; y < srcHeight; y++) {
			transform.compute(0, y, transformed);
			updateBoundBox(transformed, r);
			transform.compute(srcWidth - 1, y, transformed);
			updateBoundBox(transformed, r);
		}

		for (int x = 0; x < srcWidth; x++) {
			transform.compute(x, 0, transformed);
			updateBoundBox(transformed, r);
			transform.compute(x, srcHeight - 1, transformed);
			updateBoundBox(transformed, r);
		}

		// The upper extent is inclusive not exclusive, hence the +1
		return new RectangleLength2D_F32(r.x0, r.y0, 1 + r.x1 - r.x0, 1 + r.y1 - r.y0);
	}

	private static void updateBoundBox( Point2D_F32 p, ImageRectangle_F32 r ) {
		if (p.x < r.x0)
			r.x0 = p.x;
		else if (p.x > r.x1)
			r.x1 = p.x;
		if (p.y < r.y0)
			r.y0 = p.y;
		else if (p.y > r.y1)
			r.y1 = p.y;
	}

	/**
	 * Finds an axis-aligned bounding box which would contain a image after it has been transformed.
	 * The returned bounding box can be larger then the original image.
	 *
	 * @param srcWidth Width of the source image
	 * @param srcHeight Height of the source image
	 * @param transform Transform being applied to the image
	 * @return Bounding box
	 */
	public static RectangleLength2D_F64 boundBox_F64( int srcWidth, int srcHeight,
													  PixelTransform<Point2D_F64> transform,
													  Point2D_F64 transformed ) {
		ImageRectangle_F64 r = new ImageRectangle_F64();

		r.x0 = r.y0 = Double.MAX_VALUE;
		r.x1 = r.y1 = -Double.MAX_VALUE;

		for (int y = 0; y < srcHeight; y++) {
			transform.compute(0, y, transformed);
			updateBoundBox(transformed, r);
			transform.compute(srcWidth - 1, y, transformed);
			updateBoundBox(transformed, r);
		}

		for (int x = 0; x < srcWidth; x++) {
			transform.compute(x, 0, transformed);
			updateBoundBox(transformed, r);
			transform.compute(x, srcHeight - 1, transformed);
			updateBoundBox(transformed, r);
		}

		// The upper extent is inclusive not exclusive, hence the +1
		return new RectangleLength2D_F64(r.x0, r.y0, 1 + r.x1 - r.x0, 1 + r.y1 - r.y0);
	}

	private static void updateBoundBox( Point2D_F64 transform, ImageRectangle_F64 r ) {
		if (transform.x < r.x0)
			r.x0 = transform.x;
		else if (transform.x > r.x1)
			r.x1 = transform.x;
		if (transform.y < r.y0)
			r.y0 = transform.y;
		else if (transform.y > r.y1)
			r.y1 = transform.y;
	}
}
