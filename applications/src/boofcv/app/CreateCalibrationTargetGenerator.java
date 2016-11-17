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

package boofcv.app;

/**
 * Generates the actual calibration target.
 *
 * @author Peter Abeles
 */
public class CreateCalibrationTargetGenerator {

	String documentName;
	PaperSize paper;
	int rows,cols;
	Unit units;

	public CreateCalibrationTargetGenerator( String documentName , PaperSize paper, int rows , int cols , Unit units ) {
		this.documentName = documentName;
		this.paper = paper;
		this.rows = rows;
		this.cols = cols;
		this.units = units;
	}

	public void chessboard( double squareWidth ) {

	}

	public void squareGrid( double squareWidth , double spacing ) {

	}

	public void binaryGrid( double squareWidth , double spacing ) {

	}

	public void circleAsymmetric( double diameter , double centerDistance ) {

	}

	private void printHeader() {

	}


}
