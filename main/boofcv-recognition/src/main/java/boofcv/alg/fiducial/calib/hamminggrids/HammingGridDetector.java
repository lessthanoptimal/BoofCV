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

package boofcv.alg.fiducial.calib.hamminggrids;

import boofcv.alg.fiducial.square.DetectFiducialSquareHamming;
import boofcv.alg.fiducial.square.FoundFiducial;
import boofcv.factory.fiducial.ConfigHammingGrid;
import boofcv.struct.image.ImageGray;
import org.ddogleg.struct.DogArray;

import java.util.Arrays;

/**
 * Detector for a grid of {@link boofcv.alg.fiducial.square.DetectFiducialSquareHamming Square-Hamming} markers.
 * Commonly used for camera calibration and enable partial views to be used due to the self identifying patterns.
 *
 * @author Peter Abeles
 */
public class HammingGridDetector<T extends ImageGray<T>> {
	ConfigHammingGrid config;

	DetectFiducialSquareHamming<T> markerDetector;


	DogArray<Node> nodes = new DogArray<>(Node::new, Node::reset);

	public void process(T image) {
		// Detect markers inside the image
		markerDetector.process(image);

		// Sound found detections and create nodes in the graph
		DogArray<FoundFiducial> found = markerDetector.getFound();
		nodes.resetResize(found.size);
		for (int i = 0; i < found.size; i++) {
			Node n = nodes.get(i);
			n.index = i;
			n.det.setTo(found.get(i));
		}

		// Connect nodes which are neighbors to each other spatially in the image

		// TODO find neighbors of each square
		// TODO vote against a square if it's not consistent as a neighbor
		// TODO if a unique ID is duplicated then remove the duplicate with most down votes until there is just one
	}

	private void findNeighbors() {
		// TODO put cross intersection into KNN
	}

	private static class Node {
		public int index;
		public final Node[] neighbors = new Node[4];
		public final FoundFiducial det = new FoundFiducial();
		public void reset() {
			index = -1;
			Arrays.fill(neighbors, null);
			det.reset();
		}
	}
}
