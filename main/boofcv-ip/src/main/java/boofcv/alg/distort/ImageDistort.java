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

import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_F32;

/**
 * Copies an image onto another image while applying a transform to the pixel coordinates.
 * Pixels outside the source image can be handled using the interpolations border or by simply skipping them. This
 * behavior is set by calling the {@link #setRenderAll(boolean)} flag. By Default it will render the entire image,
 * even if pixel is outside the source image.
 *
 * @author Peter Abeles
 */
public interface ImageDistort<Input extends ImageBase, Output extends ImageBase> {

	/**
	 * Specifies how pixel coordinates are transformed from the destination
	 * to source images.
	 *
	 * @param dstToSrc Pixel coordinate transformation.
	 */
	void setModel( PixelTransform<Point2D_F32> dstToSrc );

	/**
	 * Applies the transform to the entire destination image.
	 *
	 * @param srcImg (Input) Original image.
	 * @param dstImg (Output) Distorted image.
	 */
	void apply( Input srcImg, Output dstImg );

	/**
	 * Applies the transform, but marks pixels in the mask as 1 = inside, 0 = outside. Where
	 * inside pixels are pixels in the dst image with an equivalent pixel in the src image's bounds
	 *
	 * @param srcImg (Input) Original image.
	 * @param dstImg (Output) Distorted image.
	 * @param mask (Output) Mask of inside pixels. Automatically resized to match dstImg.
	 */
	void apply( Input srcImg, Output dstImg, GrayU8 mask );

	/**
	 * Applies the transform to only the specified region inside the destination image.
	 *
	 * @param srcImg (Input) Original image.
	 * @param dstImg (Output) Distorted image.
	 * @param dstX0 Left most crop boundary. Inclusive.
	 * @param dstY0 Top most crop boundary. Inclusive.
	 * @param dstX1 Right most crop boundary. Exclusive.
	 * @param dstY1 Bottom most crop boundary. Exclusive.
	 */
	void apply( Input srcImg, Output dstImg, int dstX0, int dstY0, int dstX1, int dstY1 );

	/**
	 * Specifies if the entire output image should be rendered, even if mapping to the source image is outside
	 * the source image.
	 *
	 * @param renderAll true to render all pixels. If false then only pixels inside the source image
	 */
	void setRenderAll( boolean renderAll );

	/**
	 * Returns the render all flag
	 *
	 * @return render all flag
	 */
	boolean getRenderAll();

	/**
	 * Returns the distortion model.
	 *
	 * @return model dst to src
	 */
	PixelTransform<Point2D_F32> getModel();
}
