/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.distort.RemovePerspectiveDistortion;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F32;
import georegression.struct.shapes.Polygon2D_F64;

/**
 * Used to read bits in the area surrounding a square. The square's position is provided and transform to remove
 * perspective transform is computed. At this point the value of a bit can be read using a grid relative to the 0
 * vertex of the square. The square is assumed to occupy (0,0) to (7,7) inclusive.
 *
 * @author Peter Abeles
 */
// TODO remove lens distortion
public class SquareBitReader<T extends ImageGray<T>> {

	InterpolatePixelS<T> interpolate;
	float width,height;

	float threshold;

	Point2D_F32 pixel = new Point2D_F32();

	// Computes a mapping to remove perspective distortion
	private RemovePerspectiveDistortion<?> removePerspective = new RemovePerspectiveDistortion(7,7);

	public SquareBitReader( Class<T> imageType ) {
		interpolate = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);
	}

	public void setImage( T image ) {
		interpolate.setImage(image);

		this.width = image.width+0.99999f;
		this.height = image.height+0.99999f;
	}

	/**
	 * Sets the square and defines coordinate system
	 *
	 * @param square Square that the grid is based around
	 * @param threshold Threshold use to binarize image
	 * @return true if this operation was able to complete
	 */
	public boolean setSquare(Polygon2D_F64 square , float threshold ) {
		if( !removePerspective.createTransform(square.get(0),square.get(1),square.get(2),square.get(3)) )
			return false;

		this.threshold = threshold;
		return true;
	}

	/**
	 * Reads the value of the grid at the specified grid coordinate
	 * @param row grid row
	 * @param col grid col
	 * @return 0 = black 1 = white
	 */
	public int read( int row , int col ) {
		// with Perspective removed to Image coordinates.
		PointTransformHomography_F32 p2i = removePerspective.getTransform();

		p2i.compute(col,row,pixel);

		if( pixel.x < 0 || pixel.y < 0 || pixel.x > width || pixel.y > height )
			return -1;

		if( interpolate.get(pixel.x,pixel.y) > threshold )
			return 1;
		else
			return 0;
	}

}
