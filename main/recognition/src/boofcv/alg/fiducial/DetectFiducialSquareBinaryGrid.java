/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import boofcv.alg.fiducial.square.DetectFiducialSquareImage;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * A fiducial composed of {@link boofcv.alg.fiducial.square.DetectFiducialSquareBinary} intended for use in calibration.
 * It allows parts of the fiducial to be visible and uniquely determined across multiple cameras.
 *
 * @author Peter Abeles
 */
public class DetectFiducialSquareBinaryGrid<T extends ImageSingleBand> {

	// dimension of grid.  This only refers to black squares and not the white space between
	int numRows;
	int numCols;

	// expected id numbers of each fiducials in row major grid order
	int numbers[];

	DetectFiducialSquareImage<T> detector;

	FastQueue<Detection> detections = new FastQueue<Detection>(Detection.class,true);

	public DetectFiducialSquareBinaryGrid(int numRows, int numCols, int[] numbers,
										  DetectFiducialSquareImage<T> detector)
	{
		this.numRows = numRows;
		this.numCols = numCols;
		this.numbers = numbers;
		this.detector = detector;
	}

	public boolean detect( T input ) {
		return false;
	}

	public List<Detection> getDetections() {
		return detections.toList();
	}

	public static class Detection {
		Point2D_F64 cornerA = new Point2D_F64();
		Point2D_F64 cornerB = new Point2D_F64();
		Point2D_F64 cornerC = new Point2D_F64();
		Point2D_F64 cornerD = new Point2D_F64();
		int index;
	}
}
