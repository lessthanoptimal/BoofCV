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

package boofcv.alg.feature.detect.edge.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorderAlgs;
import boofcv.core.image.border.ImageBorder_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestImplEdgeNonMaxSuppressionCrude {

	static int width = 20;
	static int height = 30;

	@Test
	public void inner() {

		int numFound = BoofTesting.findMethodThenCall(this, "inner",
				ImplEdgeNonMaxSuppressionCrude.class,"inner4");

		assertEquals(3,numFound);
	}

	public void inner( Method m )
	{
		Class derivType = m.getParameterTypes()[1];

		GrayF32 input = new GrayF32(width,height);
		GrayF32 output = new GrayF32(width,height);
		ImageGray derivX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		ImageGray derivY = GeneralizedImageOps.createSingleBand(derivType, width, height);

		input.set(2,2,20);
		input.set(1,1,30);
		input.set(1,3,30);
		input.set(3,1,30);
		input.set(3,3,30);

		// should not be suppressed
		for( int i = 0; i < 2; i++ ) {
			for( int j = 0; j < 2; j++ ) {
				// set the direction of the gradient
				GeneralizedImageOps.set(derivX,2,2,i*2-1);
				GeneralizedImageOps.set(derivY,2,2,j*2-1);
				// adjust intensity values so that they will not suppress, but any errors will cause an error
				int dx = i==0? -1 : 1;
				int dy = j==0? -1 : 1;

				GeneralizedImageOps.set(input,2-dx,2-dy,10);
				GeneralizedImageOps.set(input,2+dx,2+dy,10);
				BoofTesting.checkSubImage(this,"inner",true,m,input,derivX,derivY,output,false);
				GeneralizedImageOps.set(input,2-dx,2-dy,30);
				GeneralizedImageOps.set(input,2+dx,2+dy,30);
			}
		}

		// should be suppressed
		input.set(1,1,10);
		input.set(1,3,10);
		input.set(3,1,10);
		input.set(3,3,10);
		for( int i = 0; i < 2; i++ ) {
			for( int j = 0; j < 2; j++ ) {
				// set the direction of the gradient
				GeneralizedImageOps.set(derivX,2,2,i);
				GeneralizedImageOps.set(derivY, 2, 2, j);

				// adjust intensity values so that they will suppress, but any errors will cause an error
				int dx = i==0? -1 : 1;
				int dy = j==0? -1 : 1;

				GeneralizedImageOps.set(input,2-dx,2-dy,30);
				GeneralizedImageOps.set(input,2+dx,2+dy,30);
				BoofTesting.checkSubImage(this, "inner", true, m, input, derivX, derivY, output, true);
				GeneralizedImageOps.set(input,2-dx,2-dy,10);
				GeneralizedImageOps.set(input,2+dx,2+dy,10);
			}
		}

		// uniform case should be NOT suppressed
		GImageMiscOps.fill(input, 2);
		BoofTesting.checkSubImage(this, "inner", true, m, input, derivX, derivY, output, false);
	}

	public static <D extends ImageGray>
	void inner(Method m , GrayF32 input , D derivX , D derivY , GrayF32 output , Boolean suppressed )
	{
		try {
			m.invoke(null,input,derivX,derivY,output);
			assertTrue((output.get(2,2)==0)==suppressed);
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void border() {

		int numFound = BoofTesting.findMethodThenCall(this, "border",
				ImplEdgeNonMaxSuppressionCrude.class,"border4");

		assertEquals(2,numFound);
	}

	public void border( Method m )
	{
		Class derivType = m.getParameterTypes()[1];

		GrayF32 input = new GrayF32(width,height);
		GrayF32 output = new GrayF32(width,height);
		ImageGray derivX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		ImageGray derivY = GeneralizedImageOps.createSingleBand(derivType, width, height);

		BoofTesting.checkSubImage(this, "border", true, m, input, derivX, derivY, output );
	}

	public static <D extends ImageGray>
	void border(Method m , GrayF32 input , D derivX , D derivY , GrayF32 output )
	{
		Random rand = new Random(123);
		GImageMiscOps.fillUniform(input, rand, 0, 30);
		GImageMiscOps.fillUniform(derivX, rand, -30, 30);
		GImageMiscOps.fillUniform(derivY, rand, -30, 30);

		try {
			m.invoke(null,input,derivX,derivY,output);

			ImageBorder_F32 intensity = (ImageBorder_F32)FactoryImageBorderAlgs.value(input, 0);

			for( int y = 0; y < input.height; y++ ) {
				if( y != 0 && y != input.height-1 )
					continue;

				for( int x = 0; x < input.width; x++ ) {
					if( x != 0 && x != input.width-1 )
						continue;

					int dx = GeneralizedImageOps.get(derivX,x,y) > 0 ? 1 : -1;
					int dy = GeneralizedImageOps.get(derivY,x,y) > 0 ? 1 : -1;

					float left = intensity.get(x-dx,y-dy);
					float middle = intensity.get(x,y);
					float right = intensity.get(x+dx,y+dy);

					assertEquals( (left < middle && right < middle), output.get(x,y) != 0);
				}
			}

			// uniform case should be NOT suppressed
			GImageMiscOps.fill(input,2);
			m.invoke(null, input, derivX, derivY, output);
			for( int y = 0; y < input.height; y++ ) {
				if( y != 0 && y != input.height-1 )
					continue;

				for( int x = 0; x < input.width; x++ ) {
					if( x != 0 && x != input.width-1 )
						continue;

					// skip the corners too since it's poorly defined how that should be handled
					// the two adjacent pixels could be outside the image, depending on gradient direction
					if((x == 0 && y == 0) || (x ==input.width-1 && y == 0) )
						continue;
					if((x ==input.width-1 && y == input.height-1) || (x ==0 && y == input.height-1) )
						continue;


					assertTrue( output.get(x,y) == 2);
				}
			}


		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
