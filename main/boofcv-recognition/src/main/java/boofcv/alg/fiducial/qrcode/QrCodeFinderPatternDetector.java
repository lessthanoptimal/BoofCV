/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.qrcode;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;

/**
 * @author Peter Abeles
 */
// TODO
public class QrCodeFinderPatternDetector<T extends ImageGray<T>> {

	// converts input image into a binary image
	InputToBinary<T> inputToBinary;

	// Detects squares inside the image
	DetectPolygonBinaryGrayRefine<T> squareDetector;



	public void process( T gray, GrayU8 binary ) {
		// detect squares but don't refine  until later
		squareDetector.process(gray,binary);

		// Create graph of neighboring squares and prune squares which are inside of other squares


		// make sure the size of the squares make sense

		// Refine the square

		// Check the size/shape with tighter tolerances

		// examine gray scale intensities

		// see if it has the expected distribution
	}

	private void identifyFinderSquares() {

		// Create NN search

		// for each square search around it's center to see if there are other squares there

		// remove all but the largest square

		// test remaining squares to see have the expected B&W pattern

	}

	private void createGraph() {
		// create a graph of finder squares using nearest neighbor search
	}

	private void identifyTripleSquares() {
		// See if two squares have parallel sides

		// Determine orientation and check for white on outside and timing pattern on inside
	}

	private void connectSquaresIntoFinderPattern() {
		// sides need to be approximately parallel

		// need to be the expected distance apart
	}

	private void refineSquares() {

	}


}
