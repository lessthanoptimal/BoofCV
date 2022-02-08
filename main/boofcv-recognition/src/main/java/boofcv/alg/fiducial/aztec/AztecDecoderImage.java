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

import boofcv.struct.image.ImageGray;
import lombok.Getter;
import org.ddogleg.struct.DogArray;

import java.util.List;

/**
 * Given locations of candidate finder patterns and the source image, decode all the markers inside the image and
 * reject false positives.
 *
 * @author Peter Abeles
 */
public class AztecDecoderImage<T extends ImageGray<T>> {
	/** Found and successfully decoded markers in the image */
	@Getter final DogArray<AztecCode> found = new DogArray<>(AztecCode::new, AztecCode::reset);

	/** Markers that it failed to decode */
	@Getter final DogArray<AztecCode> failed = new DogArray<>(AztecCode::new, AztecCode::reset);

	/**
	 * Should it consider a QR code which has been encoded with a transposed bit pattern?
	 *
	 * TODO update when config exists
	 * //	 * @see boofcv.factory.fiducial.ConfigQrCode#considerTransposed
	 */
	public boolean considerTransposed = true;

	AztecDecoder decoderBits = new AztecDecoder();

	public void process( List<AztecPyramid> locatorPatterns, T gray ) {
		found.reset();
		failed.reset();

		for (int i = 0; i < locatorPatterns.size(); i++) {

		}
	}

	protected boolean decodeMode( AztecPyramid locator, AztecCode code ) {
		// TODO read bits around marker

		// TODO try each possible orientation. Select one with lowest bit error. Stop if perfection is found

		return true;
	}

	protected boolean decodeData( AztecCode code ) {

		return true;
	}
}
