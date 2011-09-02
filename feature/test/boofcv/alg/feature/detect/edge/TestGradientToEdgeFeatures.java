/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.edge;

import boofcv.alg.feature.detect.edge.impl.ImplEdgeNonMaxSuppression;
import boofcv.alg.misc.ImageTestingOps;
import boofcv.core.image.FactorySingleBandImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.SingleBandImage;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
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

		int total = BoofTesting.findMethodThenCall(this,"intensityE",GradientToEdgeFeatures.class,"intensityE");

		assertEquals(numExpected,total);
	}

	public void intensityE( Method m )
	{
		Class<?> params[] = m.getParameterTypes();

		ImageBase derivX = GeneralizedImageOps.createImage(params[0],width,height);
		ImageBase derivY = GeneralizedImageOps.createImage(params[0],width,height);

		GeneralizedImageOps.randomize(derivX,rand,0,10);
		GeneralizedImageOps.randomize(derivY,rand,0,10);

		BoofTesting.checkSubImage(this,"intensityE",true,m,derivX,derivY,intensity);
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

		int total = BoofTesting.findMethodThenCall(this,"intensityAbs",GradientToEdgeFeatures.class,"intensityAbs");

		assertEquals(numExpected,total);
	}

	public void intensityAbs( Method m )
	{
		Class<?> params[] = m.getParameterTypes();

		ImageBase derivX = GeneralizedImageOps.createImage(params[0],width,height);
		ImageBase derivY = GeneralizedImageOps.createImage(params[0],width,height);

		GeneralizedImageOps.randomize(derivX,rand,0,10);
		GeneralizedImageOps.randomize(derivY,rand,0,10);

		BoofTesting.checkSubImage(this,"intensityAbs",true,m,derivX,derivY,intensity);
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

		int total = BoofTesting.findMethodThenCall(this,"direction",GradientToEdgeFeatures.class,"direction");

		assertEquals(numExpected,total);
	}

	public void direction( Method m )
	{
		Class<?> params[] = m.getParameterTypes();

		ImageBase derivX = GeneralizedImageOps.createImage(params[0],width,height);
		ImageBase derivY = GeneralizedImageOps.createImage(params[0],width,height);

		GeneralizedImageOps.randomize(derivX,rand,0,10);
		GeneralizedImageOps.randomize(derivY,rand,0,10);

		BoofTesting.checkSubImage(this,"direction",true,m,derivX,derivY,intensity);
	}

	public void direction( Method m , ImageBase derivX , ImageBase derivY , ImageFloat32 direction )
			throws InvocationTargetException, IllegalAccessException {
		m.invoke(null,derivX,derivY,direction);

		SingleBandImage a = FactorySingleBandImage.wrap(derivX);
		SingleBandImage b = FactorySingleBandImage.wrap(derivY);


		float expected = (float)Math.atan(b.get(1,2).floatValue()/a.get(1,2).floatValue());

		assertEquals(expected,direction.get(1,2),1e-4);
	}

	@Test
	public void discretizeDirection() {
		ImageFloat32 angle = new ImageFloat32(5,5);
		angle.set(0,0,(float)(3*Math.PI/8+0.01));
		angle.set(1,0,(float)(3*Math.PI/8-0.01));
		angle.set(2,0,(float)(Math.PI/4));
		angle.set(3,0,(float)(Math.PI/8+0.01));
		angle.set(4,0,(float)(Math.PI/8-0.01));
		angle.set(0,1,(float)(-3*Math.PI/8+0.01));
		angle.set(1,1,(float)(-3*Math.PI/8-0.01));

		ImageUInt8 d = new ImageUInt8(5,5);

		GradientToEdgeFeatures.discretizeDirection(angle,d);

		assertEquals(0,d.get(0,0));
		assertEquals(3,d.get(1,0));
		assertEquals(3,d.get(2,0));
		assertEquals(3,d.get(3,0));
		assertEquals(2,d.get(4,0));
		assertEquals(1,d.get(0,1));
		assertEquals(0,d.get(1,1));
	}

	@Test
	public void nonMaxSuppression() {
		ImageFloat32 intensity = new ImageFloat32(width,height);
		ImageUInt8 direction = new ImageUInt8(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);

		ImageTestingOps.randomize(intensity,rand,0,100);
		ImageTestingOps.randomize(direction,rand,0,4);

		BoofTesting.checkSubImage(this,"nonMaxSuppression",true,intensity, direction, expected, found);
	}

	public void nonMaxSuppression(ImageFloat32 intensity, ImageUInt8 direction, ImageFloat32 expected, ImageFloat32 found) {
		ImplEdgeNonMaxSuppression.naive(intensity,direction,expected);
		GradientToEdgeFeatures.nonMaxSuppression(intensity,direction,found);

		BoofTesting.assertEquals(expected,found,0,1e-4);
	}
}
