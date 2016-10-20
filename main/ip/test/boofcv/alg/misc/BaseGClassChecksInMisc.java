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

package boofcv.alg.misc;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageInterleaved;
import boofcv.struct.image.Planar;
import boofcv.testing.CompareEquivalentFunctions;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public abstract class BaseGClassChecksInMisc extends CompareEquivalentFunctions {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;
	int numBands = 3;

	protected BaseGClassChecksInMisc(Class<?> testClass, Class<?> validationClass) {
		super(testClass, validationClass);
	}

	@Override
	protected boolean isTestMethod(Method m) {
		Class<?> param[] = m.getParameterTypes();

		if( param.length < 1 )
			return false;

		for( int i = 0; i < param.length; i++ ) {
			if( ImageBase.class.isAssignableFrom(param[i]) )
				return true;
		}
		return false;
	}

	@Override
	protected boolean isEquivalent(Method candidate, Method validation) {
		Class<?> c[] = candidate.getParameterTypes();
		Class<?> v[] = validation.getParameterTypes();

		if( c.length != v.length)
			return false;

		if( candidate.getName().compareTo(validation.getName()) != 0 )
			return false;

		for( int i = 0; i < v.length; i++ ) {
			if( !v[i].isAssignableFrom(c[i]))
				return false;
		}
		return true;
	}

	@Override
	protected Object[] reformatForValidation(Method m, Object[] targetParam) {
		Object[] ret = new Object[targetParam.length];

		for( int i = 0; i < ret.length; i++ ) {
			if( targetParam[i] instanceof ImageBase ) {
				ret[i] = ((ImageBase)targetParam[i]).createNew(width,height);
				((ImageBase)ret[i]).setTo((ImageBase)targetParam[i]);
			} else {
				ret[i] = targetParam[i];
			}
		}

		return ret;
	}

	protected ImageBase createImage( Class imageType , Class bandType) {
		if( ImageGray.class.isAssignableFrom(imageType) ) {
			return GeneralizedImageOps.createSingleBand(imageType, width, height);
		} else if( ImageInterleaved.class.isAssignableFrom(imageType) ) {
			return GeneralizedImageOps.createInterleaved(imageType, width, height, 3);
		} else if( bandType != null ) {
			return new Planar(bandType,width,height,3);
		}
		return null;
	}

	protected void fillRandom( ImageBase img ) {
		if( img == null )
			return;

		boolean isSigned = false;
		if( img instanceof ImageGray) {
			if( ((ImageGray)img).getDataType().isSigned() ) {
				isSigned = true;
			}
		} else if( img instanceof ImageInterleaved ) {
			if( ((ImageInterleaved)img).getDataType().isSigned() ) {
				isSigned = true;
			}
		} else {
			if( ((Planar)img).getBand(0).getDataType().isSigned() ) {
				isSigned = true;
			}
		}

		if( isSigned ) {
			GImageMiscOps.fillUniform(img,rand,-10,10);
		} else {
			GImageMiscOps.fillUniform(img,rand,1,10);
		}
	}
}
