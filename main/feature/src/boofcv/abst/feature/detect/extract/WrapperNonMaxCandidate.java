/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.detect.extract.NonMaxCandidate;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;

/**
 * Wrapper around the {@link boofcv.alg.feature.detect.extract.NonMaxCandidateStrict} class.
 *
 * @author Peter Abeles
 */
public class WrapperNonMaxCandidate implements NonMaxSuppression {
	NonMaxCandidate extractor;
	boolean minimums,maximums;

	public WrapperNonMaxCandidate(NonMaxCandidate extractor, boolean minimums, boolean maximums ) {
		this.extractor = extractor;
		this.minimums = minimums;
		this.maximums = maximums;
	}

	@Override
	public float getThresholdMinimum() {
		return extractor.getThresholdMin();
	}

	@Override
	public float getThresholdMaximum() {
		return extractor.getThresholdMax();
	}

	@Override
	public void setThresholdMinimum(float threshold) {
		extractor.setThresholdMin(threshold);
	}

	@Override
	public void setThresholdMaximum(float threshold) {
		extractor.setThresholdMax(threshold);
	}

	@Override
	public void setIgnoreBorder(int border) {
		extractor.setBorder(border);
	}

	@Override
	public int getIgnoreBorder() {
		return extractor.getBorder();
	}

	@Override
	public void process(GrayF32 intensity,
						QueueCorner candidateMin, QueueCorner candidateMax,
						QueueCorner foundMin, QueueCorner foundMax) {
		extractor.process(intensity, candidateMin, candidateMax, foundMin,foundMax);
	}

	@Override
	public boolean getUsesCandidates() {
		return true;
	}

	@Override
	public void setSearchRadius(int radius) {
		extractor.setSearchRadius(radius);
	}

	@Override
	public int getSearchRadius() {
		return extractor.getSearchRadius();
	}

	@Override
	public boolean canDetectMinimums() {
		return minimums;
	}

	@Override
	public boolean canDetectMaximums() {
		return maximums;
	}
}
