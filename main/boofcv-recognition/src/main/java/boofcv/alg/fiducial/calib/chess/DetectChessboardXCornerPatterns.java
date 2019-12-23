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

import boofcv.abst.fiducial.calib.ConfigChessboardX;
import boofcv.alg.feature.detect.chess.DetectChessboardCornersXPyramid;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid.GridInfo;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.FastQueue;

/**
 * Chessboard detector that uses X-Corners and finds all valid chessboard patterns inside the image.
 * 
 * @author Peter Abeles
 */
public class DetectChessboardXCornerPatterns<T extends ImageGray<T>> {

	protected DetectChessboardCornersXPyramid<T> detector;
	protected ChessboardCornerClusterFinder<T> clusterFinder;
	protected ChessboardCornerClusterToGrid clusterToGrid = new ChessboardCornerClusterToGrid();

	protected FastQueue<GridInfo> found = new FastQueue<>(GridInfo.class,true);

	public DetectChessboardXCornerPatterns(ConfigChessboardX config , Class<T> imageType ) {

		detector = new DetectChessboardCornersXPyramid<>(ImageType.single(imageType));
		clusterFinder = new ChessboardCornerClusterFinder<>(imageType);

		detector.setPyramidTopSize(config.pyramidTopSize);
		detector.getDetector().setNonmaxRadius(config.cornerRadius);
		detector.getDetector().setNonmaxThresholdRatio((float)config.cornerNonMaxThreshold);

		clusterFinder.setAmbiguousTol(config.ambiguousTol);
		clusterFinder.setDirectionTol(config.directionTol);
		clusterFinder.setOrientationTol(config.orientationTol);
		clusterFinder.setMaxNeighbors(config.maxNeighbors);
		clusterFinder.setMaxNeighborDistance(config.maxNeighborDistance);
		clusterFinder.setThresholdEdgeIntensity(config.edgeThreshold);

		clusterToGrid.setRequireCornerSquares(config.requireCornerSquares);
	}

	/**
	 * Used to add a filter which will check the shape of found grids before returning them. This can help prune
	 * impossible configurations earlier and improve runtime speed.
	 */
	public void setCheckShape(ChessboardCornerClusterToGrid.CheckShape checkShape) {
		clusterToGrid.setCheckShape(checkShape);
	}

	/**
	 * Processes the image and searches for all chessboard patterns.
	 */
	public void findPatterns(T input) {
		found.reset();
		detector.process(input);
//		T blurred = detector.getDetector().getBlurred();
		clusterFinder.process(input,detector.getCorners().toList(),detector.getNumberOfLevels());
		FastQueue<ChessboardCornerGraph> clusters = clusterFinder.getOutputClusters();

		for (int clusterIdx = 0; clusterIdx < clusters.size; clusterIdx++) {
			ChessboardCornerGraph c = clusters.get(clusterIdx);

			if (!clusterToGrid.convert(c, found.grow())) {
				found.removeTail();
			}
		}
	}

	public DetectChessboardCornersXPyramid<T> getDetector() {
		return detector;
	}

	public ChessboardCornerClusterFinder<T> getClusterFinder() {
		return clusterFinder;
	}

	public ChessboardCornerClusterToGrid getClusterToGrid() {
		return clusterToGrid;
	}

	public FastQueue<GridInfo> getFoundChessboard() {
		return found;
	}
}
