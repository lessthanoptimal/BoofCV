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

import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper around {@link boofcv.alg.feature.detect.intensity.HessianBlobIntensity} for {@link GeneralFeatureIntensity}.
 *
 * @author Peter Abeles
 */
public class WrapperHessianBlobIntensity<I extends ImageGray, D extends ImageGray>
		extends BaseGeneralFeatureIntensity<I,D>
{

	HessianBlobIntensity.Type type;
	Method m;
	boolean minimum;

	public WrapperHessianBlobIntensity(HessianBlobIntensity.Type type, Class<D> derivType) {
		this.type = type;
		try {
			switch( type ) {
				case DETERMINANT:
					minimum = false;
					m = HessianBlobIntensity.class.getMethod("determinant",GrayF32.class,derivType,derivType,derivType);
					break;

				case TRACE:
					minimum = true;
					m = HessianBlobIntensity.class.getMethod("trace",GrayF32.class,derivType,derivType);
					break;

				default:
					throw new RuntimeException("Not supported yet");
			}
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void process(I image, D derivX, D derivY, D derivXX, D derivYY, D derivXY) {
		init(image.width,image.height);

		try {
			switch( type ) {
				case DETERMINANT:
					m.invoke(null,intensity,derivXX,derivYY,derivXY);
					break;

				case TRACE:
					m.invoke(null,intensity,derivXX,derivYY);
					break;
			}
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
		return( true );
	}

	@Override
	public boolean hasCandidates() {
		return false;
	}

	/**
	 * No ignore border unless the derivative has an ignore border
	 */
	@Override
	public int getIgnoreBorder() {
		return 0;
	}

	@Override
	public boolean localMinimums() {
		return minimum;
	}

	@Override
	public boolean localMaximums() {
		return true;
	}
}
