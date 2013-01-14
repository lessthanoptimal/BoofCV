/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.FactoryGImageSingleBand;
import boofcv.core.image.GImageSingleBand;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImplHessianBlobIntensity {
	Random rand = new Random(123);
	int width = 20;
	int height = 30;

	int numExpected = 2;

	@Test
	public void testDeterminant() {

		int total = 0;
		Method[] list = ImplHessianBlobIntensity.class.getMethods();

		for( Method m : list ) {
			if( !m.getName().equals("determinant"))
				continue;

			Class param[] = m.getParameterTypes();

			ImageSingleBand derivXX = GeneralizedImageOps.createSingleBand(param[1], width, height);
			ImageSingleBand derivYY = GeneralizedImageOps.createSingleBand(param[1], width, height);
			ImageSingleBand derivXY = GeneralizedImageOps.createSingleBand(param[1], width, height);
			ImageFloat32 intensity = new ImageFloat32(width,height);

			GImageMiscOps.fillUniform(derivXX, rand, -10, 10);
			GImageMiscOps.fillUniform(derivYY, rand, -10, 10);
			GImageMiscOps.fillUniform(derivXY, rand, -10, 10);

			BoofTesting.checkSubImage(this,"performDeterminant",true,m,intensity,derivXX,derivYY,derivXY);
			total++;
		}

		assertEquals(numExpected,total);
	}

	public void performDeterminant( Method m , ImageFloat32 intensity,
									ImageSingleBand derivXX, ImageSingleBand derivYY, ImageSingleBand derivXY )
			throws InvocationTargetException, IllegalAccessException
	{
		m.invoke(null,intensity,derivXX,derivYY,derivXY);

		GImageSingleBand xx = FactoryGImageSingleBand.wrap(derivXX);
		GImageSingleBand yy = FactoryGImageSingleBand.wrap(derivYY);
		GImageSingleBand xy = FactoryGImageSingleBand.wrap(derivXY);

		float expected =xx.get(5,6).floatValue()*yy.get(5,6).floatValue() - xy.get(5,6).floatValue()*xy.get(5,6).floatValue();
		assertEquals(expected,intensity.get(5,6),1e-4);
	}

	@Test
	public void testTrace() {

		int total = 0;
		Method[] list = ImplHessianBlobIntensity.class.getMethods();

		for( Method m : list ) {
			if( !m.getName().equals("trace"))
				continue;

			Class param[] = m.getParameterTypes();

			ImageSingleBand derivXX = GeneralizedImageOps.createSingleBand(param[1], width, height);
			ImageSingleBand derivYY = GeneralizedImageOps.createSingleBand(param[1], width, height);
			ImageFloat32 intensity = new ImageFloat32(width,height);

			GImageMiscOps.fillUniform(derivXX, rand, -10, 10);
			GImageMiscOps.fillUniform(derivYY, rand, -10, 10);

			BoofTesting.checkSubImage(this,"performTrace",true,m,intensity,derivXX,derivYY);
			total++;
		}

		assertEquals(numExpected,total);
	}

	public void performTrace( Method m , ImageFloat32 intensity,
									ImageSingleBand derivXX, ImageSingleBand derivYY )
			throws InvocationTargetException, IllegalAccessException
	{
		m.invoke(null,intensity,derivXX,derivYY);

		GImageSingleBand xx = FactoryGImageSingleBand.wrap(derivXX);
		GImageSingleBand yy = FactoryGImageSingleBand.wrap(derivYY);


		float expected = xx.get(5,6).floatValue() + yy.get(5,6).floatValue();
		assertEquals(expected,intensity.get(5,6),1e-4);
	}
}
