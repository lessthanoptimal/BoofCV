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

package boofcv.abst.fiducial.calib;

import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.ThresholdType;
import boofcv.struct.Configuration;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Configuration for a {@link CalibrationDetectorSquareFiducialGrid} that uses a
 * {@link boofcv.alg.fiducial.square.DetectFiducialSquareBinary} for the inner fiducials.
 *
 * @author Peter Abeles
 */
public class ConfigSquareGridBinary implements Configuration {

	/**
	 * Description of the binary fiducial detector
	 */
	public ConfigFiducialBinary configDetector = new ConfigFiducialBinary();

	/**
	 * Description of image thresholding algorithm
	 */
	public ConfigThreshold configThreshold = ConfigThreshold.local(ThresholdType.LOCAL_SQUARE,20);

	/**
	 * Ordered list of expected square fiducial ID numbers.  Starts from top left and follows a row major
	 * ordering.
	 */
	public long ids[];

	/**
	 * Number of squares tall the grid is. Target dependent.
	 */
	public int numRows = -1;

	/**
	 * Number of squares wide the grid is. Target dependent.
	 */
	public int numCols = -1;

	/**
	 * Physical width of the square.
	 */
	public double squareWidth;

	/**
	 * Physical width space between each square
	 */
	public double spaceWidth;

	{
		configDetector.gridWidth = 3;
	}

	/**
	 * Configures the grid with the following specifications and configures the fiducial detector to be
	 * 3x3 with fiducials id number 0 to (numRows*numCols)-1.
	 */
	public ConfigSquareGridBinary(int numRows, int numCols , double squareWidth , double spaceWidth ) {
		this.numRows = numRows;
		this.numCols = numCols;
		this.squareWidth = squareWidth;
		this.spaceWidth = spaceWidth;

		this.ids = new long[numRows*numCols];
		for (int i = 0; i < ids.length; i++) {
			ids[i] = i;
		}
	}

	public ConfigSquareGridBinary() {
	}

	@Override
	public void checkValidity() {
		if( ids == null )
			throw new IllegalArgumentException("Need to specify expected ids");
	}

	/**
	 * Parses a simple configuration text sequence that describes the target type.  Example is shown below:
	 * <pre>
	 * # Description of a binary grid calibration target
	 * binary_width 3
	 * grid_shape 4
	 * square_width 40
	 * space_width 20
	 * numbers 0 1 2 3 4 5 6 7 8 9 10 11
	 * </pre>
	 * @param reader Input
	 * @return Configuration
	 * @throws IOException
	 */
	public static ConfigSquareGridBinary parseSimple( BufferedReader reader ) throws IOException {
		ConfigSquareGridBinary config = new ConfigSquareGridBinary();

		String line = reader.readLine();
		while( line != null ) {
			if( line.charAt(0) != '#') {
				String words[] = line.split(" ");
				if( words[0].equals("binary_width")) {
					config.configDetector.gridWidth = Integer.parseInt(words[1]);
				} else if( words[0].equals("grid_shape")) {
					config.numRows = Integer.parseInt(words[1]);
					config.numCols = Integer.parseInt(words[2]);
				} else if( words[0].equals("square_width")) {
					config.squareWidth = Double.parseDouble(words[1]);
				} else if( words[0].equals("space_width")) {
					config.spaceWidth = Double.parseDouble(words[1]);
				} else if( words[0].equals("numbers")) {
					config.ids = new long[ words.length-1 ];
					for (int i = 1; i < words.length; i++) {
						config.ids[i-1] = Integer.parseInt(words[i]);
					}
				}
			}

			line = reader.readLine();
		}

		return config;
	}
}
