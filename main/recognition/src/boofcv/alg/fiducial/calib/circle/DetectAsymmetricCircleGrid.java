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

package boofcv.alg.fiducial.calib.circle;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;

import java.util.List;

/**
 * Detects asymetric grids of circles.  The grid is composed of two regular grids which are offset by half a period.
 * See image below for an example.  Rows and columns are counted by counting every row even if they are offset
 * from each other.
 *
 * <p>
 * For each circle there is one control point.  The control point is first found by detecting all the ellipses, which
 * is what a circle appears to be under perspective distortion.  The center the ellipse might not match the physical
 * center of the circle.  The intersection of lines does not change under perspective distortion.  The outer common
 * tangent lines between neigboring ellipses is found.  Then the intersection of two such lines is found.  This
 * intersection will be the physical center of the circle.
 * </p>
 *
 * <center>
 * <img src="doc-files/asymcirclegrid.jpg"/>
 * </center>
 * Example of a 8 by 5 grid; row, column.
 *
 * @author Peter Abeles
 */
// TODO Future: Only refine ellipses inside the grid
public class DetectAsymmetricCircleGrid<T extends ImageGray> {

	BinaryEllipseDetector<T> ellipseDetector;
	InputToBinary<T> inputToBinary;

	GrayU8 binary = new GrayU8(1,1);

	// description of the calibration target
	int numRows, numCols;

	public void reset() {
	}

	public boolean process(T gray) {
		this.binary.reshape(gray.width,gray.height);

		inputToBinary.process(gray, binary);

		ellipseDetector.process(gray, binary);


		return true;
	}

	public List<Point2D_F64> getCalibrationPoints() {
		return null;
	}

	public GrayU8 getBinary() {
		return binary;
	}

	public int getColumns() {
		return numCols;
	}

	public int getRows() {
		return numRows;
	}
}
