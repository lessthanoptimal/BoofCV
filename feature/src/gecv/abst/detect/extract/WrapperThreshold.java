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

package gecv.abst.detect.extract;

import gecv.alg.feature.detect.extract.ThresholdCornerExtractor;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageFloat32;

/**
 * Wrapper around the {@link gecv.alg.feature.detect.extract.ThresholdCornerExtractor} class.
 *
 * @author Peter Abeles
 */
public class WrapperThreshold implements FeatureExtractor {

	ThresholdCornerExtractor extractor;

	public WrapperThreshold(ThresholdCornerExtractor extractor) {
		this.extractor = extractor;
	}

	@Override
	public void process(ImageFloat32 intensity, QueueCorner candidate, int requestedNumber,
					 QueueCorner excludeCorners, QueueCorner foundFeature) {
		extractor.process(intensity, foundFeature);
	}

	@Override
	public float getThreshold() {
		return extractor.getThreshold();
	}

	@Override
	public void setIgnoreBorder(int border) {

	}

	@Override
	public void setThreshold(float threshold) {
		extractor.setThreshold(threshold);
	}

	@Override
	public boolean getUsesCandidates() {
		return false;
	}

	@Override
	public boolean getCanExclude() {
		return false;
	}

	@Override
	public boolean getAcceptRequest() {
		return false;
	}
}
