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

package gecv.alg.detect.edge;

import gecv.core.image.FactorySingleBandImage;
import gecv.core.image.GeneralizedImageOps;
import gecv.core.image.SingleBandImage;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestGradientToEdgeFeatures {
	int numExpected = 3;
	int width = 20;
	int height = 30;

	Random rand = new Random(4378847);

	ImageFloat32 intensity = new ImageFloat32(width,height);

	@Test
	public void intensityE()  {

		int total = GecvTesting.findMethodThenCall(this,"intensityE",GradientToEdgeFeatures.class,"intensityE");

		assertEquals(numExpected,total);
	}

	public void intensityE( Method m )
	{
		Class<?> params[] = m.getParameterTypes();

		ImageBase derivX = GeneralizedImageOps.createImage(params[0],width,height);
		ImageBase derivY = GeneralizedImageOps.createImage(params[0],width,height);

		GeneralizedImageOps.randomize(derivX,rand,0,10);
		GeneralizedImageOps.randomize(derivY,rand,0,10);

		GecvTesting.checkSubImage(this,"intensityE",true,m,derivX,derivY,intensity);
	}

	public void intensityE( Method m , ImageBase derivX , ImageBase derivY , ImageFloat32 intensity )
			throws InvocationTargetException, IllegalAccessException {
		m.invoke(null,derivX,derivY,intensity);

		SingleBandImage a = FactorySingleBandImage.wrap(derivX);
		SingleBandImage b = FactorySingleBandImage.wrap(derivY);


		float expected = (float)Math.sqrt( Math.pow(a.get(1,2).doubleValue(),2) + Math.pow(b.get(1,2).doubleValue(),2));

		assertEquals(expected,intensity.get(1,2),1e-4);
	}

	@Test
	public void intensityAbs()  {

		int total = GecvTesting.findMethodThenCall(this,"intensityAbs",GradientToEdgeFeatures.class,"intensityAbs");

		assertEquals(numExpected,total);
	}

	public void intensityAbs( Method m )
	{
		Class<?> params[] = m.getParameterTypes();

		ImageBase derivX = GeneralizedImageOps.createImage(params[0],width,height);
		ImageBase derivY = GeneralizedImageOps.createImage(params[0],width,height);

		GeneralizedImageOps.randomize(derivX,rand,0,10);
		GeneralizedImageOps.randomize(derivY,rand,0,10);

		GecvTesting.checkSubImage(this,"intensityAbs",true,m,derivX,derivY,intensity);
	}

	public void intensityAbs( Method m , ImageBase derivX , ImageBase derivY , ImageFloat32 intensity )
			throws InvocationTargetException, IllegalAccessException {
		m.invoke(null,derivX,derivY,intensity);

		SingleBandImage a = FactorySingleBandImage.wrap(derivX);
		SingleBandImage b = FactorySingleBandImage.wrap(derivY);


		float expected = Math.abs(a.get(1,2).floatValue()) + Math.abs(b.get(1,2).floatValue());

		assertEquals(expected,intensity.get(1,2),1e-4);
	}

	@Test
	public void direction()  {

		int total = GecvTesting.findMethodThenCall(this,"direction",GradientToEdgeFeatures.class,"direction");

		assertEquals(numExpected,total);
	}

	public void direction( Method m )
	{
		Class<?> params[] = m.getParameterTypes();

		ImageBase derivX = GeneralizedImageOps.createImage(params[0],width,height);
		ImageBase derivY = GeneralizedImageOps.createImage(params[0],width,height);

		GeneralizedImageOps.randomize(derivX,rand,0,10);
		GeneralizedImageOps.randomize(derivY,rand,0,10);

		GecvTesting.checkSubImage(this,"direction",true,m,derivX,derivY,intensity);
	}

	public void direction( Method m , ImageBase derivX , ImageBase derivY , ImageFloat32 direction )
			throws InvocationTargetException, IllegalAccessException {
		m.invoke(null,derivX,derivY,direction);

		SingleBandImage a = FactorySingleBandImage.wrap(derivX);
		SingleBandImage b = FactorySingleBandImage.wrap(derivY);


		float expected = (float)Math.atan(b.get(1,2).floatValue()/a.get(1,2).floatValue());

		assertEquals(expected,direction.get(1,2),1e-4);
	}
}
