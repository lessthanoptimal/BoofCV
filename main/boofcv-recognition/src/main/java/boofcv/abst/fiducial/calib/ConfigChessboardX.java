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

package boofcv.abst.fiducial.calib;

import boofcv.alg.fiducial.calib.chess.DetectChessboardBinaryPattern;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.Configuration;
import lombok.Getter;
import lombok.Setter;

/**
 * Calibration parameters for chessboard style calibration grid.
 *
 * @author Peter Abeles
 * @see DetectChessboardBinaryPattern
 */
@Getter @Setter
public class ConfigChessboardX implements Configuration {

	/**
	 * Size of a corner in the corner detector. 1 is recommended in general. 2 or higher can be used to run faster
	 * if the image quality and the apparent target size is large.
	 */
	public int detNonMaxRadius = 1;

	/**
	 * Non-maximum threshold relative to maximum x-corner intensity. 0 to 1, inclusive
	 */
	public double detNonMaxThresholdRatio = 0.05;

	/**
	 * After the initial candidate x-corners have been found a more accurate x-corner intensity is computed which
	 * doesn't compromise as much for speed. If the refined intensity is less than this value it will be discarded.
	 */
	public double detRefinedXCornerThreshold = 0.001;

	/**
	 * The minimum allowed size for the top most layer in the pyramid. size = min(width,height). To have
	 * only one layer in the pyramid at the same resolution as the input set this to a value of &le; 0
	 */
	public int detPyramidTopSize = 100;

	/**
	 * Relative threshold for two corners being connected. The edge between them must have sufficient intensity.
	 * The definition of sufficient is based on the contrast of the two x-corners.
	 */
	public double connEdgeThreshold = 0.05;

	/**
	 * How similar the direction of two corners relative to each other need to be. 0 to 1. Higher is more tolerant
	 */
	public double connDirectionTol = 0.85;

	/**
	 * How similar two corner orientations need to be
	 */
	public double connOrientationTol = 0.65;

	/**
	 * Ratio used to decide if two corners are spatially close enough to each other to be considered
	 * as the same corner.
	 */
	public double connAmbiguousTol = 0.25;

	/**
	 * Maximum number of neighbors returned by nearest neighbor search
	 */
	public int connMaxNeighbors = 20;

	/**
	 * Maximum search distance for nearest neighbor search. Units = pixels.
	 */
	public double connMaxNeighborDistance = Double.MAX_VALUE;

	/**
	 * If true then a chessboard has to have at least one square which is connected to only one other
	 * square. BoofCV's calibration targets requirements this. Other projects might not.
	 */
	public boolean gridRequireCornerSquares = false;

	public ConfigChessboardX setTo( ConfigChessboardX src ) {
		this.detNonMaxRadius = src.detNonMaxRadius;
		this.detNonMaxThresholdRatio = src.detNonMaxThresholdRatio;
		this.detRefinedXCornerThreshold = src.detRefinedXCornerThreshold;
		this.detPyramidTopSize = src.detPyramidTopSize;
		this.connEdgeThreshold = src.connEdgeThreshold;
		this.connDirectionTol = src.connDirectionTol;
		this.connOrientationTol = src.connOrientationTol;
		this.connAmbiguousTol = src.connAmbiguousTol;
		this.connMaxNeighbors = src.connMaxNeighbors;
		this.connMaxNeighborDistance = src.connMaxNeighborDistance;
		this.gridRequireCornerSquares = src.gridRequireCornerSquares;
		return this;
	}

	@Override
	public void checkValidity() {
		BoofMiscOps.checkFraction(connDirectionTol, "directionTol must be 0 to 1");
	}
}
