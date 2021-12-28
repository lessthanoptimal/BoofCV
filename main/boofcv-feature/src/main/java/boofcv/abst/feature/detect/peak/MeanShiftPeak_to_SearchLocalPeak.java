/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.peak;

import boofcv.alg.feature.detect.peak.MeanShiftPeak;
import boofcv.struct.image.ImageGray;

/**
 * Wrapper around {@link boofcv.alg.feature.detect.peak.MeanShiftPeak} for {@link SearchLocalPeak}
 *
 * @author Peter Abeles
 */
public class MeanShiftPeak_to_SearchLocalPeak<T extends ImageGray<T>> implements SearchLocalPeak<T> {

	MeanShiftPeak<T> search;
	boolean positive;

	public MeanShiftPeak_to_SearchLocalPeak( MeanShiftPeak<T> search, boolean positive ) {
		this.search = search;
		this.positive = positive;
	}

	@Override
	public void setImage( T image ) {
		search.setImage(image);
	}

	@Override
	public void setSearchRadius( int radius ) {
		search.setRadius(radius);
	}

	@Override
	public void search( float x, float y ) {
		if (positive) {
			search.searchPositive(x, y);
		} else {
			search.search(x, y);
		}
	}

	@Override
	public float getPeakX() {
		return search.getPeakX();
	}

	@Override
	public float getPeakY() {
		return search.getPeakY();
	}
}
