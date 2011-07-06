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

package gecv.core.image;

import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageInteger;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestConvertImage {

	Random rand = new Random(34);
	int imgWidth = 10;
	int imgHeight = 20;

	@Test
	public void checkAllConvert() {
		int count = 0;
		Method methods[] = ConvertImage.class.getMethods();

		for (Method m : methods) {
			if( !m.getName().contains("convert"))
				continue;

			Class<?> inputType = m.getParameterTypes()[0];
			Class<?> outputType = m.getParameterTypes()[1];

//			System.out.println(m.getName()+" "+inputType.getSimpleName()+" "+outputType.getSimpleName()+" "+m.getReturnType());
			
			// make sure the return type equals the output type
			assertTrue( outputType == m.getReturnType() );

			checkConvert(m,inputType,outputType);
			count++;
		}

		assertEquals(42,count);
	}

	private void checkConvert( Method m , Class<?> inputType , Class<?> outputType ) {
		ImageBase<?> input = GecvTesting.createImage(inputType,imgWidth,imgHeight);
		ImageBase<?> output = GecvTesting.createImage(outputType,imgWidth,imgHeight);

		boolean inputSigned = true;
		boolean outputSigned = true;

		if( ImageInteger.class.isAssignableFrom(inputType) )
			inputSigned = ((ImageInteger)input).isSigned();
		if( ImageInteger.class.isAssignableFrom(outputType) )
			outputSigned = ((ImageInteger)output).isSigned();

	   // only provide signed numbers of both data types can handle them
		if( inputSigned && outputSigned ) {
			GeneralizedImageOps.randomize(input, rand, -10,10);
		} else {
			GeneralizedImageOps.randomize(input, rand, 0,20);
		}

		GecvTesting.checkSubImage(this,"checkConvert",true,m,input,output);
	}

	public void checkConvert( Method m , ImageBase<?> input , ImageBase<?> output ) {
		try {
			// check it with a non-null output
			ImageBase<?> ret = (ImageBase<?>)m.invoke(null,input,output);
			GecvTesting.assertEqualsGeneric(input,ret,0,1e-4);

			// check it with a null output
			ret = (ImageBase<?>)m.invoke(null,input,null);
			GecvTesting.assertEqualsGeneric(input,ret,0,1e-4);

		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
