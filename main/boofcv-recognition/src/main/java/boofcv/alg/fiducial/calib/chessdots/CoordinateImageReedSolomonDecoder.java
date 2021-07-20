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

package boofcv.alg.fiducial.calib.chessdots;

import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray_I8;

import java.util.List;

/**
 * Given an image and the location of an encoded coordinate, attempt to decode the coordinate based in pixel intensity.
 * Since the orientation is unknown, it will try all possible orientations.
 *
 * @author Peter Abeles
 */
public class CoordinateImageReedSolomonDecoder<T extends ImageGray<T>> {

	final ChessboardReedSolomonCodec codec;

	/** Fraction of a cell's length the data bit is */
	public @Getter @Setter double dataBitWidthFraction = 0.7;

	/** Fraction of the length the quite zone is around data bits */
	public @Getter @Setter double dataBorderFraction = 0.15;

	/** Storage for decoded message */
	final DogArray_I8 message = new DogArray_I8();

	/** Number of clockwise rotation before it could process the target */
	@Getter int rotationsCW;

	public CoordinateImageReedSolomonDecoder( ChessboardReedSolomonCodec codec ) {
		this.codec = codec;
	}

	public boolean decode( T image, List<Point2D_F64> corners, Point2D_I32 coordinate ) {

		// Compute warping from grid coordinates to image pixels

		// sample the values of each bit of data and the background

		// Compute OTSU threshold and covert to binary

		// decode and rotate 4 times and see if exactly was of them is successful

		return true;
	}
}
