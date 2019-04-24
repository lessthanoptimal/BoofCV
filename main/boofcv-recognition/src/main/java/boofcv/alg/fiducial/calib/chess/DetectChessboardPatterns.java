/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.calib.ConfigChessboard2;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.feature.detect.chess.DetectChessboardCorners;
import boofcv.alg.feature.detect.chess.DetectChessboardCornersPyramid;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid.GridInfo;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.struct.image.GrayF32;
import org.ddogleg.struct.FastQueue;

/**
 * Detector which finds all chessboard patterns in view.
 * 
 * @author Peter Abeles
 */
public class DetectChessboardPatterns {

	protected DetectChessboardCornersPyramid detector = new DetectChessboardCornersPyramid();
	protected ChessboardCornerClusterFinder clusterFinder = new ChessboardCornerClusterFinder();
	protected ChessboardCornerClusterToGrid clusterToGrid = new ChessboardCornerClusterToGrid();

	protected FastQueue<GridInfo> found = new FastQueue<>(GridInfo.class,true);

	public DetectChessboardPatterns(ConfigChessboard2 config) {

		// the user is unlikely to set this value correctly
		config.threshold.maxPixelValue = DetectChessboardCorners.GRAY_LEVELS;

		InputToBinary<GrayF32> thresholder = FactoryThresholdBinary.threshold(config.threshold,GrayF32.class);

		detector.getDetector().setThresholding(thresholder);
		detector.getDetector().setKernelRadius(config.cornerRadius);
		detector.getDetector().setCornerIntensityThreshold(config.cornerThreshold);
		detector.setPyramidTopSize(config.pyramidTopSize);

		clusterFinder.setAmbiguousTol(config.ambiguousTol);
		clusterFinder.setDirectionTol(config.directionTol);
		clusterFinder.setOrientationTol(config.orientaitonTol);
		clusterFinder.setMaxNeighbors(config.maxNeighbors);
		clusterFinder.setMaxNeighborDistance(config.maxNeighborDistance);

	}

	/**
	 * Used to add a filter which will check the shape of found grids before returning them. This can help
	 * impossible configurations earlier and possibly reduce by an insignificant amount CPU.
	 */
	public void setCheckShape(ChessboardCornerClusterToGrid.CheckShape checkShape) {
		clusterToGrid.setCheckShape(checkShape);
	}

	/**
	 * Processes the image and searches for all chessboard patterns.
	 */
	public void findPatterns(GrayF32 input) {
		found.reset();
		detector.process(input);
		clusterFinder.process(detector.getCorners().toList());
		FastQueue<ChessboardCornerGraph> clusters = clusterFinder.getOutputClusters();

		for (int clusterIdx = 0; clusterIdx < clusters.size; clusterIdx++) {
			ChessboardCornerGraph c = clusters.get(clusterIdx);

			if (!clusterToGrid.convert(c, found.grow())) {
				found.removeTail();
			}
		}
	}

	public DetectChessboardCornersPyramid getDetector() {
		return detector;
	}

	public ChessboardCornerClusterFinder getClusterFinder() {
		return clusterFinder;
	}

	public ChessboardCornerClusterToGrid getClusterToGrid() {
		return clusterToGrid;
	}

	public FastQueue<GridInfo> getFoundChessboard() {
		return found;
	}
}
