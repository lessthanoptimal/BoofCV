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

package boofcv.alg.filter.convolve.down;

import boofcv.alg.filter.convolve.ConvolutionTestHelper;
import boofcv.alg.filter.convolve.ConvolveDownNoBorder;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.struct.image.ImageGray;
import boofcv.testing.CompareIdenticalFunctions;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * Compares the target class to functions in {@link ConvolveDownNoBorder}.
 *
 * @author Peter Abeles
 */
public class CompareToStandardConvolveDownNoBorder extends CompareIdenticalFunctions
{
	protected Random rand = new Random(0xFF);

	protected int width = 20;
	protected int height = 30;
	protected int kernelRadius = 1;
	protected int skip = 2;

	public CompareToStandardConvolveDownNoBorder( Class<?> targetClass ) {
		super(targetClass, ConvolveDownNoBorderStandard.class);
	}

	public void compareMethod( Method target , String validationName , int radius ) {
		this.kernelRadius = radius;
		super.compareMethod(target,validationName);
	}

	public void setImageDimention( int width , int height ) {
		this.width = width;
		this.height = height;
	}

	public void setKernelRadius(int kernelRadius) {
		this.kernelRadius = kernelRadius;
	}

	public void setSkip(int skip) {
		this.skip = skip;
	}

	@Override
	protected Object[][] createInputParam(Method candidate, Method validation) {
		Class<?> paramTypes[] = candidate.getParameterTypes();

		Object kernel = FactoryKernel.random(paramTypes[0],kernelRadius,0,10,rand);

		int divW,divH;
		if( candidate.getName().compareTo("horizontal") == 0) {
			divW = skip; divH = 1;
		} else if( candidate.getName().compareTo("vertical") == 0) {
			divW = 1; divH = skip;
		} else {
			divW = divH = skip;
		}

		ImageGray src = ConvolutionTestHelper.createImage(paramTypes[1], width, height);
		GImageMiscOps.fillUniform(src, rand, 0, 130);
		ImageGray dst = ConvolutionTestHelper.createImage(paramTypes[2], width/divW, height/divH);

		Object[][] ret = new Object[1][paramTypes.length];
		ret[0][0] = kernel;
		ret[0][1] = src;
		ret[0][2] = dst;
		ret[0][3] = skip;
		if( paramTypes.length == 5) {
			ret[0][4] = 11;
		}

		return ret;
	}
}
