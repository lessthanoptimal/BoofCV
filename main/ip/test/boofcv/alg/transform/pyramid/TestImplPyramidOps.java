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

package boofcv.alg.transform.pyramid;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.pyramid.impl.ImplPyramidOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImplPyramidOps {

	Random rand = new Random(234);

	@Test
	public void scaleImageUp() {
		int numFound = 0;
		Method methods[] = ImplPyramidOps.class.getMethods();
		for( Method m : methods ) {
			if( m.getName().compareTo("scaleImageUp") != 0 )
				continue;

			Class params[] = m.getParameterTypes();

			scaleImageUp(params[0],m);
			numFound++;
		}
		assertEquals(2,numFound);
	}

	private <T extends ImageGray>
	void scaleImageUp(Class<T> imageType , Method m ) {
		T input = GeneralizedImageOps.createSingleBand(imageType,15, 8);
		T output = GeneralizedImageOps.createSingleBand(imageType,1, 1);
		GImageMiscOps.fillUniform(input, rand, -10, 10);

		InterpolatePixelS<T> interp = FactoryInterpolation.
				createPixelS(0,255, InterpolationType.BILINEAR, BorderType.EXTENDED,imageType);

		try {
			m.invoke(null,input,output,2,interp);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}

		assertEquals(30,output.width );
		assertEquals(16,output.height);


		interp.setImage(input);

		for (int y = 0; y < output.height; y++) {
			for (int x = 0; x < output.width; x++) {
				float inputY = y/2.0f;
				float inputX = x/2.0f;

				float expected = interp.get(inputX,inputY);
				double found = GeneralizedImageOps.get(output,x,y);

				assertEquals(expected,found,1);
			}
		}
	}

	@Test
	public void scaleDown2() {
		int numFound = 0;
		Method methods[] = ImplPyramidOps.class.getMethods();
		for( Method m : methods ) {
			if( m.getName().compareTo("scaleDown2") != 0 )
				continue;

			Class params[] = m.getParameterTypes();

			scaleDown2(params[0],m);
			numFound++;
		}
		assertEquals(2,numFound);
	}

	private <T extends ImageGray>
	void scaleDown2(Class<T> imageType , Method m ) {

		int sizes [] = new int[]{30,31};

		for( int width : sizes ) {
			int height = width*2/3 + width%2;
			T input = GeneralizedImageOps.createSingleBand(imageType,width, height);
			T output = GeneralizedImageOps.createSingleBand(imageType,1, 1);

			GImageMiscOps.fillUniform(input, rand, -10, 10);
			try {
				m.invoke(null,input,output);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			assertEquals(width/2, output.width);
			assertEquals(height/2, output.height);

			for (int y = 0; y < output.height; y++) {
				for (int x = 0; x < output.width; x++) {
					double expected = GeneralizedImageOps.get(input,x*2,y*2);
					double found = GeneralizedImageOps.get(output,x,y);

					assertEquals(expected,found, 1e-4);
				}
			}
		}
	}

}