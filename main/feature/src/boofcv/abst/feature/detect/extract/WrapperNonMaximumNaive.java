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

import boofcv.alg.feature.detect.extract.NonMaxExtractorNaive;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;

/**
 * Wrapper around the {@link boofcv.alg.feature.detect.extract.NonMaxExtractorNaive} class.
 *
 * @author Peter Abeles
 */
public class WrapperNonMaximumNaive implements NonMaxSuppression {

	NonMaxExtractorNaive alg;

	public WrapperNonMaximumNaive(NonMaxExtractorNaive alg) {
		this.alg = alg;
	}

	@Override
	public int getIgnoreBorder() {
		return alg.getBorder();
	}

	@Override
	public void setIgnoreBorder(int border) {
		alg.setBorder(border);
	}

	@Override
	public void process(GrayF32 intensity,
						QueueCorner candidateMin, QueueCorner candidateMax,
						QueueCorner foundMin, QueueCorner foundMax) {
		alg.process(intensity, foundMax);
	}

	@Override
	public boolean getUsesCandidates() {
		return false;
	}

	@Override
	public float getThresholdMinimum() {
		return Float.NaN;
	}

	@Override
	public float getThresholdMaximum() {
		return alg.getThreshold();
	}

	@Override
	public void setThresholdMinimum(float threshold) {
	}

	@Override
	public void setThresholdMaximum(float threshold) {
		alg.setThreshold(threshold);
	}

	@Override
	public void setSearchRadius(int radius) {
		alg.setSearchRadius(radius);
	}

	@Override
	public int getSearchRadius() {
		return alg.getSearchRadius();
	}

	@Override
	public boolean canDetectMaximums() {
		return true;
	}

	@Override
	public boolean canDetectMinimums() {
		return false;
	}
}
