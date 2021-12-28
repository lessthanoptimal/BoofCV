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

package boofcv.abst.filter.derivative;

import boofcv.BoofDefaults;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Generic implementation which uses reflections to call hessian functions
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ImageHessian_Reflection<Output extends ImageGray<Output>>
		implements ImageHessian<Output> {
	// How the image border should be handled
	BorderType borderType = BoofDefaults.DERIV_BORDER_TYPE;
	ImageBorder<Output> border;
	// the image hessian function
	private Method m;

	public ImageHessian_Reflection( Method m ) {
		this.m = m;
		setBorderType(borderType);
	}

	@Override
	public void process( Output inputDerivX, Output inputDerivY, Output derivXX, Output derivYY, Output derivXY ) {
		try {
			m.invoke(null, inputDerivX, inputDerivY, derivXX, derivYY, derivXY, border);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setBorderType( BorderType type ) {
		this.borderType = type;
		Class imageType = m.getParameterTypes()[0];
		border = FactoryImageBorder.single(borderType, imageType);
	}

	@Override
	public BorderType getBorderType() {
		return borderType;
	}

	@Override
	public int getBorder() {
		if (borderType != BorderType.SKIP)
			return 0;
		else
			return 1;
	}

	@Override
	public ImageType<Output> getDerivativeType() {
		return ImageType.single((Class)m.getParameterTypes()[2]);
	}
}
