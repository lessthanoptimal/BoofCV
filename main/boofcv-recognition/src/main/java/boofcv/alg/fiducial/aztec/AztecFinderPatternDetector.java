/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.aztec;

import boofcv.alg.fiducial.qrcode.SquareLocatorPatternDetectorBase;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageGray;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

/**
 * Searches for Aztec finder patterns inside an image and returns a list of candidates. Finder patterns are found
 * by looking at the external contour of block quadrilaterals and looking for quadrilaterals that have a similar
 * center pixel. If one, two, or three match then that's consider a match for compact or full-range Aztec codes.
 *
 * @author Peter Abeles
 */
public class AztecFinderPatternDetector<T extends ImageGray<T>> extends SquareLocatorPatternDetectorBase<T> {
	/**
	 * Configures the detector
	 *
	 * @param squareDetector Square detector
	 */
	public AztecFinderPatternDetector( DetectPolygonBinaryGrayRefine<T> squareDetector ) {
		super(squareDetector);
	}

	@Override protected void findLocatorPatternsFromSquares() {
//		this.positionPatterns.reset();
//		List<DetectPolygonFromContour.Info> infoList = squareDetector.getPolygonInfo();
//		for (int i = 0; i < infoList.size(); i++) {
//			DetectPolygonFromContour.Info info = infoList.get(i);
//
//			// TODO find center by intersecting lines
//
//		}

		// TODO find clusters are squares with matching corners using KNN search

		// TODO if inner most square, see if it's ring-2 using black white pattern

		// TODO from sets with a ring-2 see if next ring is a valid ring-3

		// TODO for all outer rings, look at surrounding black-white pattern to see if there are those rings too

		// todo return set of found locator patterns for complact and full-range aztec codes

		// TODO if multiple rings, rotate until the corners are aligned

	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> set ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}
}
