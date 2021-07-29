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

import boofcv.abst.fiducial.calib.ConfigChessboardX;
import boofcv.alg.feature.detect.chess.DetectChessboardCornersXPyramid;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterFinder;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph;
import boofcv.struct.GridShape;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ChessboardReedSolomonDetector<T extends ImageGray<T>> {

	// TODO Create a custom cluster finder
	// TODO only keep clusters with 1 encoded pattern inside
	// TODO concensus approach to deciding coordinates
	// TODo specify max grid coordinates

	protected ChessboardReedSolomonCodec codec;

	protected DetectChessboardCornersXPyramid<T> detector;
	protected ChessboardCornerClusterFinder<T> clusterFinder;
	protected ChessboardCornerClusterToGrid clusterToGrid = new ChessboardCornerClusterToGrid();

	List<GridShape> markers = new ArrayList<>();

	public ChessboardReedSolomonDetector( ConfigChessboardX config, Class<T> imageType ) {

		detector = new DetectChessboardCornersXPyramid<>(ImageType.single(imageType));
		clusterFinder = new ChessboardCornerClusterFinder<>(imageType);

		detector.setPyramidTopSize(config.detPyramidTopSize);
		detector.getDetector().setNonmaxRadius(config.detNonMaxRadius);
		detector.getDetector().setNonmaxThresholdRatio((float)config.detNonMaxThresholdRatio);
		detector.getDetector().setRefinedXCornerThreshold(config.detRefinedXCornerThreshold);

		clusterFinder.setAmbiguousTol(config.connAmbiguousTol);
		clusterFinder.setDirectionTol(config.connDirectionTol);
		clusterFinder.setOrientationTol(config.connOrientationTol);
		clusterFinder.setMaxNeighbors(config.connMaxNeighbors);
		clusterFinder.setMaxNeighborDistance(config.connMaxNeighborDistance);
		clusterFinder.setThresholdEdgeIntensity(config.connEdgeThreshold);
	}

	/**
	 * Processes the image and searches for all chessboard patterns.
	 */
	public void findPatterns( T input ) {
		detector.process(input);
		clusterFinder.process(input, detector.getCorners().toList(), detector.getNumberOfLevels());
		DogArray<ChessboardCornerGraph> clusters = clusterFinder.getOutputClusters();

		for (int clusterIdx = 0; clusterIdx < clusters.size; clusterIdx++) {
			// Find the chessboard pattern inside the cluster
			if (!clusterToGrid.clusterToSparse(clusters.get(clusterIdx))) {
				continue;
			}

			// TODO iterate through every white cell
			// Check squares for data patterns and save their coordinate for later retrieval

			// If no data patterns are found, extract the largest grid and return that

			// Select the coordinate system that best matches the decoded cells

			// Prune corners using prior knowledge on this coordinate
		}
	}
}
