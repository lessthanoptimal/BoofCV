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

package boofcv.abst.feature.detect.intensity;

import boofcv.alg.feature.detect.intensity.KitRosCornerIntensity;
import boofcv.struct.ListIntPoint2D;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper around children of {@link boofcv.alg.feature.detect.intensity.GradientCornerIntensity}.
 *
 * @author Peter Abeles
 */
public class WrapperKitRosCornerIntensity<I extends ImageGray<I>, D extends ImageGray<D>>
		extends BaseGeneralFeatureIntensity<I, D> {
	Method m;

	public WrapperKitRosCornerIntensity( Class<D> derivType ) {
		super(null, derivType);
		try {
			m = KitRosCornerIntensity.class.getMethod("process", GrayF32.class, derivType, derivType, derivType, derivType, derivType);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void process( I image, @Nullable D derivX, @Nullable D derivY,
						 @Nullable D derivXX, @Nullable D derivYY, @Nullable D derivXY ) {
		init(image.width, image.height);

		try {
			m.invoke(null, intensity, derivX, derivY, derivXX, derivYY, derivXY);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public @Nullable ListIntPoint2D getCandidatesMin() {
		return null;
	}

	@Override
	public @Nullable ListIntPoint2D getCandidatesMax() {
		return null;
	}

	@Override
	public boolean getRequiresGradient() {
		return true;
	}

	@Override
	public boolean getRequiresHessian() {
		return true;
	}

	@Override
	public boolean hasCandidates() {
		return false;
	}

	/**
	 * There is no ignore border, unless the derivative that it is computed from has an ignore border.
	 */
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
