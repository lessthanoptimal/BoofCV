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

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;

/**
 * Nearest Neighbor interpolation for a rectangular region
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class NearestNeighborRectangle_F32 implements InterpolateRectangle<GrayF32> {

	GrayF32 image;

	@Override public void setImage( GrayF32 image ) {
		this.image = image;
	}

	@Override public GrayF32 getImage() {
		return image;
	}

	@Override public void region( float tl_x, float tl_y, GrayF32 dest ) {
		int x = (int)tl_x;
		int y = (int)tl_y;

		if (x < 0 || y < 0 || x + dest.width > image.width - 1 || y + dest.height > image.height - 1)
			throw new IllegalArgumentException("Out of bounds");

		for (int i = 0; i < dest.height; i++) {
			int indexSrc = image.startIndex + image.stride*(i + y) + x;
			int indexDst = dest.startIndex + dest.stride*i;

			System.arraycopy(image.data, indexSrc, dest.data, indexDst, dest.width);
		}
	}

	@Override public InterpolateRectangle<GrayF32> copyConcurrent() {
		return this; // no fields modified outside setImage()
	}

	@Override public InterpolateRectangle<GrayF32> copy() {
		return new NearestNeighborRectangle_F32();
	}

	@Override public ImageType<GrayF32> getImageType() {
		return ImageType.SB_F32;
	}
}
