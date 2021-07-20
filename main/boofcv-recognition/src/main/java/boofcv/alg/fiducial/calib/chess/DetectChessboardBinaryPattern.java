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

package boofcv.alg.fiducial.calib.chess;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.struct.ConfigLength;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import lombok.Getter;

import java.util.List;

/**
 * <p>
 * Detects calibration points inside a chessboard calibration target. First the image
 * is thresholded to create a binary image for square detection, then the binary image is eroded to make sure
 * the squares don't touch. After that {@link DetectChessboardSquarePoints} is called and it detects and sorts
 * the squares.
 * </p>
 * <p>
 * The found control points are ensured to be returned in a row-major format with the correct number of rows and columns,
 * with a counter clockwise ordering. Note that when viewed on the monitor this will appear to be clockwise because
 * the y-axis points down. If there are multiple valid solution then the solution with the (0,0) grid point closest
 * top the origin is selected.
 * </p>
 * <center>
 * <img src="doc-files/chessboard.jpg"/>
 * </center>
 * Example of a 7 by 5 grid; row, column.
 *
 * @author Peter Abeles
 */
public class DetectChessboardBinaryPattern<T extends ImageGray<T>> {

	// detects the chess board
	private @Getter DetectChessboardSquarePoints<T> findSeeds;
	// binary images used to detect chess board
	private @Getter GrayU8 binary = new GrayU8(1, 1);
	private @Getter GrayU8 eroded = new GrayU8(1, 1);

	// description of the grid its detecting
	private int numRows, numCols;

	InputToBinary<T> inputToBinary;

	/**
	 * Configures detection parameters
	 *
	 * @param numRows Number of rows in the grid. Target dependent.
	 * @param numCols Number of columns in the grid. Target dependent.
	 * @param maxCornerDistance The maximum distance two square corners can be from each other in pixels
	 */
	public DetectChessboardBinaryPattern( int numRows, int numCols, ConfigLength maxCornerDistance,
										  DetectPolygonBinaryGrayRefine<T> detectorSquare,
										  InputToBinary<T> inputToBinary ) {
		this.numRows = numRows;
		this.numCols = numCols;

		this.inputToBinary = inputToBinary;

		findSeeds = new DetectChessboardSquarePoints<>(numRows, numCols, maxCornerDistance, detectorSquare);

		reset();
	}

	/**
	 * Forgets any past history and resets the detector
	 */
	public void reset() {
	}

	public boolean process( T gray ) {
		binary.reshape(gray.width, gray.height);
		eroded.reshape(gray.width, gray.height);

		inputToBinary.process(gray, binary);

		// erode to make the squares separated
		BinaryImageOps.erode8(binary, 1, eroded);

		return findSeeds.process(gray, eroded);
	}

	public List<PointIndex2D_F64> getCalibrationPoints() {
		return findSeeds.getCalibrationPoints().toList();
	}

	public int getColumns() {
		return numCols;
	}

	public int getRows() {
		return numRows;
	}
}
