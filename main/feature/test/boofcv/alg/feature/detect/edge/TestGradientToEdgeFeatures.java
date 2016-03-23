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

package boofcv.alg.feature.detect.edge;

import boofcv.alg.feature.detect.edge.impl.ImplEdgeNonMaxSuppression;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.FactoryGImageGray;
import boofcv.core.image.GImageGray;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import georegression.metric.UtilAngle;
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

	GrayF32 intensity = new GrayF32(width,height);

	@Test
	public void intensityE()  {

		int total = BoofTesting.findMethodThenCall(this,"intensityE",GradientToEdgeFeatures.class,"intensityE");

		assertEquals(numExpected,total);
	}

	public void intensityE( Method m )
	{
		Class params[] = m.getParameterTypes();

		ImageGray derivX = GeneralizedImageOps.createSingleBand(params[0], width, height);
		ImageGray derivY = GeneralizedImageOps.createSingleBand(params[0], width, height);

		GImageMiscOps.fillUniform(derivX, rand, 0, 10);
		GImageMiscOps.fillUniform(derivY, rand, 0, 10);

		BoofTesting.checkSubImage(this,"intensityE",true,m,derivX,derivY,intensity);
	}

	public void intensityE(Method m , ImageGray derivX , ImageGray derivY , GrayF32 intensity )
			throws InvocationTargetException, IllegalAccessException {
		m.invoke(null,derivX,derivY,intensity);

		GImageGray a = FactoryGImageGray.wrap(derivX);
		GImageGray b = FactoryGImageGray.wrap(derivY);

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
		Class params[] = m.getParameterTypes();

		ImageGray derivX = GeneralizedImageOps.createSingleBand(params[0], width, height);
		ImageGray derivY = GeneralizedImageOps.createSingleBand(params[0], width, height);

		GImageMiscOps.fillUniform(derivX, rand, 0, 10);
		GImageMiscOps.fillUniform(derivY, rand, 0, 10);

		BoofTesting.checkSubImage(this,"intensityAbs",true,m,derivX,derivY,intensity);
	}

	public void intensityAbs(Method m , ImageGray derivX , ImageGray derivY , GrayF32 intensity )
			throws InvocationTargetException, IllegalAccessException {
		m.invoke(null,derivX,derivY,intensity);

		GImageGray a = FactoryGImageGray.wrap(derivX);
		GImageGray b = FactoryGImageGray.wrap(derivY);


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
		Class params[] = m.getParameterTypes();

		ImageGray derivX = GeneralizedImageOps.createSingleBand(params[0], width, height);
		ImageGray derivY = GeneralizedImageOps.createSingleBand(params[0], width, height);

		GImageMiscOps.fillUniform(derivX, rand, 0, 10);
		GImageMiscOps.fillUniform(derivY, rand, 0, 10);

		BoofTesting.checkSubImage(this,"direction",true,m,derivX,derivY,intensity);
	}

	public void direction(Method m , ImageGray derivX , ImageGray derivY , GrayF32 direction )
			throws InvocationTargetException, IllegalAccessException {
		m.invoke(null,derivX,derivY,direction);

		GImageGray a = FactoryGImageGray.wrap(derivX);
		GImageGray b = FactoryGImageGray.wrap(derivY);


		float expected = (float)Math.atan(b.get(1,2).floatValue()/a.get(1,2).floatValue());

		assertEquals(expected,direction.get(1,2),1e-4);
	}

	@Test
	public void discretizeDirection4() {
		GrayF32 angle = new GrayF32(5,5);
		angle.set(0,0,(float)(3*Math.PI/8+0.01));
		angle.set(1,0,(float)(3*Math.PI/8-0.01));
		angle.set(2,0,(float)(Math.PI/4));
		angle.set(3,0,(float)(Math.PI/8+0.01));
		angle.set(4,0,(float)(Math.PI/8-0.01));
		angle.set(0,1,(float)(-3*Math.PI/8+0.01));
		angle.set(1,1,(float)(-3*Math.PI/8-0.01));

		GrayS8 d = new GrayS8(5,5);

		GradientToEdgeFeatures.discretizeDirection4(angle,d);

		assertEquals(2,d.get(0,0));
		assertEquals(1,d.get(1,0));
		assertEquals(1,d.get(2,0));
		assertEquals(1,d.get(3,0));
		assertEquals(0,d.get(4,0));
		assertEquals(-1,d.get(0,1));
		assertEquals(2,d.get(1,1));
	}

	@Test
	public void discretizeDirection8() {
		GrayF32 angle = new GrayF32(5,5);
		for( int i = 0; i < 8; i++ ) {
			angle.data[i] = (float) UtilAngle.bound(i*Math.PI/4.0);
		}

		GrayS8 d = new GrayS8(5,5);

		GradientToEdgeFeatures.discretizeDirection8(angle,d);

		for( int i = 0; i < 8; i++ ) {
			int expected = i > 4 ? i-8 : i;
			assertEquals(expected,d.data[i]);
		}
	}

	@Test
	public void nonMaxSuppression4() {
		GrayF32 intensity = new GrayF32(width,height);
		GrayS8 direction = new GrayS8(width,height);
		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		BoofTesting.checkSubImage(this,"nonMaxSuppression4",true,intensity, direction, expected, found);
	}

	public void nonMaxSuppression4(GrayF32 intensity, GrayS8 direction, GrayF32 expected, GrayF32 found) {

		// compare to naive
		ImageMiscOps.fillUniform(intensity, rand, 0, 100);
		ImageMiscOps.fillUniform(direction, rand, 0, 4);

		ImplEdgeNonMaxSuppression.naive4(intensity,direction,expected);
		GradientToEdgeFeatures.nonMaxSuppression4(intensity,direction,found);

		BoofTesting.assertEquals(expected,found, 1e-4);

		// make sure it does not suppresses values which are equal.
		ImageMiscOps.fill(intensity, 2);

		ImplEdgeNonMaxSuppression.naive4(intensity,direction,expected);
		GradientToEdgeFeatures.nonMaxSuppression4(intensity,direction,found);

		BoofTesting.assertEquals(expected,found, 1e-4);
		for( int y = 0; y < found.height; y++ ) {
			for( int x = 0; x < found.width; x++ ) {
				assertEquals(2,found.unsafe_get(x,y),1e-4f);
			}
		}
	}

	@Test
	public void nonMaxSuppression8() {
		GrayF32 intensity = new GrayF32(width,height);
		GrayS8 direction = new GrayS8(width,height);
		GrayF32 expected = new GrayF32(width,height);
		GrayF32 found = new GrayF32(width,height);

		BoofTesting.checkSubImage(this,"nonMaxSuppression4",true,intensity, direction, expected, found);
	}

	public void nonMaxSuppression8(GrayF32 intensity, GrayS8 direction, GrayF32 expected, GrayF32 found) {

		// compare to naive
		ImageMiscOps.fillUniform(intensity, rand, 0, 100);
		ImageMiscOps.fillUniform(direction, rand, 0, 4);

		ImplEdgeNonMaxSuppression.naive8(intensity, direction, expected);
		GradientToEdgeFeatures.nonMaxSuppression8(intensity, direction, found);

		BoofTesting.assertEquals(expected,found, 1e-4);

		// make sure it does not suppresses values which are equal.
		ImageMiscOps.fill(intensity, 2);

		ImplEdgeNonMaxSuppression.naive4(intensity,direction,expected);
		GradientToEdgeFeatures.nonMaxSuppression4(intensity,direction,found);

		BoofTesting.assertEquals(expected,found, 1e-4);
		for( int y = 0; y < found.height; y++ ) {
			for( int x = 0; x < found.width; x++ ) {
				assertEquals(2,found.unsafe_get(x,y),1e-4f);
			}
		}
	}
}
