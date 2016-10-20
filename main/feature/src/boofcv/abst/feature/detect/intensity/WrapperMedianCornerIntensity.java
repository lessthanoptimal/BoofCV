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

import boofcv.abst.filter.blur.BlurStorageFilter;
import boofcv.alg.feature.detect.intensity.MedianCornerIntensity;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper around children of {@link boofcv.alg.feature.detect.intensity.MedianCornerIntensity}.  This is a bit of a hack since
 * the median image is not provided as a standard input so it has to compute it internally
 * 
 * @author Peter Abeles
 */
public class WrapperMedianCornerIntensity<I extends ImageGray, D extends ImageGray>
		extends BaseGeneralFeatureIntensity<I,D>  {

	Method m;
	BlurStorageFilter<I> medianFilter;
	I medianImage;

	public WrapperMedianCornerIntensity(BlurStorageFilter<I> medianFilter ,
										Class<I> imageType ) {
		this.medianFilter = medianFilter;
		try {
			m = MedianCornerIntensity.class.getMethod("process",GrayF32.class,imageType,imageType);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void process(I input, D derivX , D derivY , D derivXX , D derivYY , D derivXY ) {
		init(input.width,input.height);

		if( medianImage == null ) {
			medianImage = (I)input.createNew(input.width,input.height);
		} else {
			medianImage.reshape(input.width,input.height);
		}
		
		medianFilter.process(input,medianImage);
		try {
			m.invoke(null,intensity,input,medianImage);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public QueueCorner getCandidatesMin() {
		return null;
	}

	@Override
	public QueueCorner getCandidatesMax() {
		return null;
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
		return false;
	}

	@Override
	public int getIgnoreBorder() {
		return 0;
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
