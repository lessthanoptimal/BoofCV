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

package boofcv.abst.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.FastCornerIntensity;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageGray;

/**
 * Wrapper around children of {@link boofcv.alg.feature.detect.intensity.FastCornerIntensity}.
 * 
 * @author Peter Abeles
 */
public class WrapperFastCornerIntensity<I extends ImageGray, D extends ImageGray>
		extends BaseGeneralFeatureIntensity<I,D>
{
	FastCornerIntensity<I> alg;

	public WrapperFastCornerIntensity(FastCornerIntensity<I> alg) {
		this.alg = alg;
	}

	@Override
	public void process(I input, D derivX , D derivY , D derivXX , D derivYY , D derivXY ) {
		init(input.width,input.height);
		alg.process(input,intensity);
	}

	@Override
	public QueueCorner getCandidatesMin() {
		return null;
	}

	@Override
	public QueueCorner getCandidatesMax() {
		return alg.getCandidates();
	}

	@Override
	public boolean getRequiresGradient() {
		return false;
	}

	@Override
	public boolean getRequiresHessian() {
		return false;
	}

	@Override
	public boolean hasCandidates() {
		return true;
	}

	@Override
	public int getIgnoreBorder() {
		return alg.getIgnoreBorder();
	}

	@Override
	public boolean localMinimums() {
		return false;
	}

	@Override
	public boolean localMaximums() {
		return true;
	}
}
