/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.abst.detect.corner;

import gecv.abst.detect.extract.CornerExtractor;
import gecv.struct.image.ImageBase;

/**
 * Extracts corners from a single image. Can be configured to return all
 * the found features or just the ones with the largest intensity.
 *
 * @author Peter Abeles
 */
public class CornerDetectorImage<I extends ImageBase> extends CornerDetectorBase<I> {

	// computes the corner intensity image
	CornerIntensityImage<I> intensity;

	public CornerDetectorImage(CornerIntensityImage<I> intensity, CornerExtractor extractor,
							   int maxFoundCorners) {
		super(extractor, maxFoundCorners);
		this.intensity = intensity;
	}

	/**
	 * Computes corners inside the provided image.
	 *
	 * @param inputImage
	 */
	public void process(I inputImage) {
		intensity.process(inputImage);
		foundCorners.reset();
		extractor.process(intensity.getIntensity(), candidateCorners, requestedFeatureNumber, foundCorners);
		if (selectBest != null) {
			selectBest.process(intensity.getIntensity(), foundCorners);
		}
	}
}
