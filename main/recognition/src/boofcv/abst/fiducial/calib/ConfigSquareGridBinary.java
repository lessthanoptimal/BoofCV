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

package boofcv.abst.fiducial.calib;

import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.filter.binary.ConfigThreshold;

/**
 * TODO fill in
 *
 * @author Peter Abeles
 */
public class ConfigSquareGridBinary {

	public ConfigFiducialBinary configDetector;
	public ConfigThreshold configThreshold;

	public long ids[];

	/**
	 * Number of squares wide the grid is. Target dependent.
	 */
	public int numCols = -1;

	/**
	 * Number of squares tall the grid is. Target dependent.
	 */
	public int numRows = -1;

	/**
	 * Physical width of the square.
	 */
	public double squareWidth;

	/**
	 * Physical width of hte space between each square
	 */
	public double spaceWidth;
}
