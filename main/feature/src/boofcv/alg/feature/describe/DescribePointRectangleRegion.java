/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.describe;

import boofcv.struct.image.ImageGray;

/**
 * Base class for describing a rectangular region using pixels.
 *
 * @author Peter Abeles
 */
public abstract class DescribePointRectangleRegion<T extends ImageGray>
{
	// image that descriptors are being extracted from
	protected T image;

	// size of the extracted region
	protected int regionWidth;
	protected int regionHeight;
	// radius from focal pixel
	protected int radiusWidth;
	protected int radiusHeight;

	// offset in terms of pixel index from the center pixel
	protected int offset[];

	public DescribePointRectangleRegion(int regionWidth, int regionHeight) {
		this.regionWidth = regionWidth;
		this.regionHeight = regionHeight;

		this.radiusWidth = regionWidth/2;
		this.radiusHeight = regionHeight/2;

		offset = new int[ regionHeight*regionWidth ];
	}

	public void setImage( T image ) {
		this.image = image;

		for( int i = 0; i < regionHeight; i++ ) {
			for( int j = 0; j < regionWidth; j++ ) {
				offset[i*regionWidth+j] = (i-radiusHeight)*image.stride + j-radiusWidth;
			}
		}
	}

	public int getDescriptorLength() {
		return offset.length;
	}

	public int getRegionWidth() {
		return regionWidth;
	}

	public int getRegionHeight() {
		return regionHeight;
	}
}
