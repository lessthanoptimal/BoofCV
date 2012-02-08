/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.extract;

import boofcv.alg.feature.detect.extract.NonMaxBorderExtractor;
import boofcv.alg.feature.detect.extract.NonMaxExtractor;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;

/**
 * Wrapper around the {@link boofcv.alg.feature.detect.extract.NonMaxExtractorNaive} class.
 *
 * @author Peter Abeles
 */
public class WrapperNonMax implements FeatureExtractor {

	NonMaxExtractor extractor;
	NonMaxBorderExtractor extractorBorder;

	public WrapperNonMax(NonMaxExtractor extractor,
						 NonMaxBorderExtractor extractorBorder) {
		this.extractor = extractor;
		this.extractorBorder = extractorBorder;
	}

	@Override
	public void process(ImageFloat32 intensity, QueueCorner candidate, int requestedNumber,
					QueueCorner foundFeature) {
		extractor.process(intensity, foundFeature);
		if( extractorBorder != null )
			extractorBorder.process(intensity,foundFeature);
	}

	@Override
	public float getThreshold() {
		return extractor.getThresh();
	}

	@Override
	public void setInputBorder(int border) {
		extractor.setInputBorder(border);
		if( extractorBorder != null )
			extractorBorder.setInputBorder(border);
	}

	@Override
	public void setThreshold(float threshold) {
		extractor.setThresh(threshold);
	}

	@Override
	public boolean getUsesCandidates() {
		return false;
	}

	@Override
	public boolean getAcceptRequest() {
		return false;
	}

	@Override
	public int getInputBorder() {
		return extractor.getInputBorder();
	}

	@Override
	public boolean canDetectBorder() {
		return extractorBorder!=null;
	}
}
