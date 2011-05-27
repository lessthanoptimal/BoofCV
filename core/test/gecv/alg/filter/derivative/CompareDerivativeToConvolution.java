/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.filter.derivative;

import gecv.abst.filter.FilterInterface;
import gecv.abst.filter.FilterSequence;
import gecv.abst.filter.convolve.BorderType;
import gecv.abst.filter.convolve.FactoryConvolve;
import gecv.struct.convolve.Kernel1D;
import gecv.struct.convolve.Kernel2D;
import gecv.struct.image.ImageBase;
import gecv.testing.GecvTesting;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Compares a specialized image derivative function to the equivalent convolution
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class CompareDerivativeToConvolution {

	Method m;
	FilterInterface outputFilters[];

	Class<ImageBase> inputType;
	Class<ImageBase> outputType;

	boolean processBorder;
	int borderSize = 0;

	public void setTarget( Method m )  {
		this.m = m;
		Class<?> []param = m.getParameterTypes();
		outputFilters = new FilterInterface<?,?>[ param.length ];

		inputType = (Class<ImageBase>)param[0];
		outputType = (Class<ImageBase>)param[1];
	}

	public void setKernel( int which , Kernel1D horizontal , Kernel1D vertical ) {
		FilterInterface<?,?> f1 = FactoryConvolve.convolve(horizontal,inputType,outputType, BorderType.EXTENDED,true);
		FilterInterface<?,?> f2 = FactoryConvolve.convolve(vertical,outputType,outputType, BorderType.EXTENDED,false);

		outputFilters[which] = new FilterSequence(f1,f2);

		if( borderSize < horizontal.getRadius() )
			borderSize = horizontal.getRadius();
		if( borderSize < vertical.getRadius() )
			borderSize = vertical.getRadius();
	}

	public void setKernel( int which , Kernel1D kernel , boolean isHorizontal) {
		outputFilters[which] =
				FactoryConvolve.convolve(kernel,inputType,outputType, BorderType.EXTENDED,isHorizontal);
		if( borderSize < kernel.getRadius() )
			borderSize = kernel.getRadius();
	}

	public void setKernel( int which , Kernel2D kernel ) {
		outputFilters[which] =
				FactoryConvolve.convolve(kernel,inputType,outputType, BorderType.EXTENDED);
		if( borderSize < kernel.getRadius() )
			borderSize = kernel.getRadius();
	}
	public void compare( ImageBase inputImage , ImageBase ...outputImages)  {
		compare(false,inputImage,outputImages);
		compare(true,inputImage,outputImages);
	}

	public void compare( boolean processBorder , ImageBase inputImage , ImageBase ...outputImages)  {
		this.processBorder = processBorder;
		innerCompare(inputImage,outputImages);

		inputImage = GecvTesting.createSubImageOf(inputImage);
		ImageBase subOut[] = new ImageBase[ outputImages.length ];
		for( int i = 0; i < outputImages.length; i++ )
			subOut[i] = GecvTesting.createSubImageOf(outputImages[i]);
		innerCompare(inputImage,subOut);
	}

	protected void innerCompare( ImageBase inputImage , ImageBase ...outputImages)  {
		Class<?> []param = m.getParameterTypes();
		int numImageOutputs = countImageOutputs(param);
		if( outputImages.length != numImageOutputs )
			throw new RuntimeException("Unexpected number of outputImages passed in");

		// declare and compute the validation results
		ImageBase expectedOutput[] = new ImageBase[param.length-2];
		for( int i = 0; i < expectedOutput.length; i++ ) {
			expectedOutput[i] = outputImages[i]._createNew(inputImage.width,inputImage.height);
			outputFilters[i].process(inputImage,expectedOutput[i]);
		}

		// compute results from the test function
		Object testInputs[] = new Object[ param.length ];
		testInputs[0] = inputImage;
		for( int i = 1; i < numImageOutputs+1; i++ ) {
			testInputs[i] = outputImages[i-1];
		}
		if( param.length == numImageOutputs + 2 )
			testInputs[param.length-1] = processBorder;
		try {
			m.invoke(null,testInputs);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		// compare the results
		for( int i = 0; i < expectedOutput.length; i++ ) {
			int border = processBorder ? 0 : borderSize;
			GecvTesting.assertEqualsGeneric(expectedOutput[i],outputImages[i],0,1e-4f,border);

			if( !processBorder )
				GecvTesting.checkBorderZero(outputImages[i],border);
		}
	}

	/**
	 * Counts the number of derivative images it takes in as an output.
	 * @param param
	 * @return
	 */
	private int countImageOutputs(Class<?>[] param) {

		int count = 0;

		for( int i = 1; i < param.length; i++ ) {
			if( ImageBase.class.isAssignableFrom(param[i])) {
				count++;
			}
		}

		return count;
	}
}
