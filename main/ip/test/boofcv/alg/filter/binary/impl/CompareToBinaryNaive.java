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

package boofcv.alg.filter.binary.impl;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.testing.CompareIdenticalFunctions;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class CompareToBinaryNaive extends CompareIdenticalFunctions {

	protected Random rand = new Random(0xFF);

	protected int width = 20;
	protected int height = 30;

	boolean hasNumTimes;

	public CompareToBinaryNaive( boolean hasNumTimes , Class<?> testClass) {
		super(testClass, ImplBinaryNaiveOps.class);
		this.hasNumTimes = hasNumTimes;
	}

	@Override
	protected Object[] reformatForValidation(Method m, Object[] targetParam) {
		if( targetParam.length == 3 ) {
			Object[] ret = new Object[2];
			ret[0] = targetParam[0];
			ret[1] = targetParam[2];
			return ret;
		}
		return targetParam;
	}

	@Override
	protected boolean isEquivalent(Method candidate, Method evaluation) {
		if(isSpecialFunction(candidate)) {
			if( evaluation.getName().compareTo(candidate.getName()) != 0 )
				return false;

			return candidate.getParameterTypes().length == 2;
		} else {
			return super.isEquivalent(candidate,evaluation);
		}
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {

		GrayU8 input = new GrayU8(width, height);
		GrayU8 output = new GrayU8(width, height);

		ImageMiscOps.fillUniform(input, rand, 0, 1);

		if(isSpecialFunction(candidate)) {
			return new Object[][]{{input,1, output}};
		} else {
			return new Object[][]{{input, output}};
		}
	}

	@Override
	protected void compareResults(Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam) {

		if( targetParam.length == 3 ) {
			Object []tmp = new Object[2];
			tmp[0] = targetParam[0];
			tmp[1] = targetParam[2];
			targetParam = tmp;
		}

		super.compareResults(targetResult,targetParam,validationResult,validationParam);
	}

	private boolean isSpecialFunction(Method candidate) {
		return hasNumTimes && (candidate.getName().contains("erode") || candidate.getName().contains("dilate"));
	}
}
