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

package boofcv.abst.fiducial;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.fiducial.aztec.AztecCode;
import boofcv.alg.fiducial.aztec.AztecDecoderImage;
import boofcv.alg.fiducial.aztec.AztecFinderPatternDetector;
import boofcv.alg.fiducial.aztec.AztecPyramid;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * An Aztec Code detector which is designed to detect finder pattern corners to a high degree of precision.
 */
public class AztecCodePreciseDetector<T extends ImageGray<T>> implements AztecCodeDetector<T>, VerbosePrint {
	/** Converts input image into a binary image */
	@Getter InputToBinary<T> inputToBinary;
	/** Detects pyramidal finder patterns */
	@Getter AztecFinderPatternDetector<T> detectorPyramids;
	/** Decodes a marker given its finder pattern */
	@Getter AztecDecoderImage<T> decoder;

	// Storage for thresholded input image
	@Getter GrayU8 binary = new GrayU8(1, 1);

	// Specifies type of input image
	Class<T> imageType;

	// Storage for all markers, successful and failures
	final DogArray<AztecCode> allMarkers = new DogArray<>(AztecCode::new, AztecCode::reset);

	// Successfully decoded markers in the image
	final List<AztecCode> detected = new ArrayList<>();

	// Candidate markers which could not be decoded
	final List<AztecCode> failed = new ArrayList<>();

	@Nullable PrintStream verbose = null;

	public AztecCodePreciseDetector( InputToBinary<T> inputToBinary,
									 DetectPolygonBinaryGrayRefine<T> squareDetector,
									 Class<T> imageType ) {
		this.imageType = imageType;
		this.inputToBinary = inputToBinary;
		this.detectorPyramids = new AztecFinderPatternDetector<>(squareDetector);
		this.decoder = new AztecDecoderImage<>(imageType);
	}

	@Override public void process( T gray ) {
		// Reset and initialize
		allMarkers.reset();
		detected.clear();
		failed.clear();

		// Detect finder patterns
		inputToBinary.process(gray, binary);
		detectorPyramids.process(gray, binary);

		// Attempt to decode candidate markers
		List<AztecPyramid> pyramids = detectorPyramids.getFound().toList();
		if (verbose != null) verbose.println("Total pyramids found: " + pyramids.size());
		for (int locatorIdx = 0; locatorIdx < pyramids.size(); locatorIdx++) {
			AztecPyramid pyramid = pyramids.get(locatorIdx);

			if (verbose != null) verbose.println("Considering pyramid at: " + pyramid.get(0).center);

			AztecCode marker = allMarkers.grow();

			if (decoder.process(pyramid, gray, marker)) {
				detected.add(marker);
			} else {
				failed.add(marker);
			}
		}
	}

	@Override public List<AztecCode> getDetections() {return detected;}

	@Override public List<AztecCode> getFailures() {return failed;}

	@Override public Class<T> getImageType() {return imageType;}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(out, configuration, decoder, detectorPyramids);
	}
}
