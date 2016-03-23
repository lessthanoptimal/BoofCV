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
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestImplGradientToEdgeFeatures {

	Random rand = new Random(234);
	int width = 30;
	int height = 40;

	int NUM_FUNCTIONS = 3;
	
	@Test
	public void intensityE() {
		int numFound = BoofTesting.findMethodThenCall(this,"intensityE",ImplGradientToEdgeFeatures.class,"intensityE");
		assertEquals(NUM_FUNCTIONS,numFound);
	}

	public void intensityE( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class derivType = m.getParameterTypes()[0];

		ImageGray derivX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		ImageGray derivY = GeneralizedImageOps.createSingleBand(derivType, width, height);

		GImageMiscOps.fillUniform(derivX, rand, -20, 20);
	    GImageMiscOps.fillUniform(derivY, rand, -20, 20);

		GrayF32 intensity = new GrayF32(width,height);

		BoofTesting.checkSubImage(this,"intensityE",false,m, derivX, derivY, intensity);
	}

	public void intensityE(Method m, ImageGray derivX, ImageGray derivY, GrayF32 intensity) throws IllegalAccessException, InvocationTargetException {
		m.invoke(null,derivX,derivY,intensity);

		double x = GeneralizedImageOps.get(derivX,5,10);
		double y = GeneralizedImageOps.get(derivY,5,10);
		double found = GeneralizedImageOps.get(intensity,5,10);
		double expected = Math.sqrt(x*x + y*y);

		assertEquals(expected,found,1);
	}

	@Test
	public void intensityAbs() {
		int numFound = BoofTesting.findMethodThenCall(this,"intensityAbs",ImplGradientToEdgeFeatures.class,"intensityAbs");
		assertEquals(NUM_FUNCTIONS,numFound);
	}

	public void intensityAbs( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class derivType = m.getParameterTypes()[0];

		ImageGray derivX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		ImageGray derivY = GeneralizedImageOps.createSingleBand(derivType, width, height);

		GImageMiscOps.fillUniform(derivX, rand, -20, 20);
	    GImageMiscOps.fillUniform(derivY, rand, -20, 20);

		GrayF32 intensity = new GrayF32(width,height);

		BoofTesting.checkSubImage(this,"intensityAbs",false,m, derivX, derivY, intensity);
	}

	public void intensityAbs(Method m, ImageGray derivX, ImageGray derivY, GrayF32 intensity) throws IllegalAccessException, InvocationTargetException {
		m.invoke(null,derivX,derivY,intensity);

		double x = GeneralizedImageOps.get(derivX,5,10);
		double y = GeneralizedImageOps.get(derivY,5,10);
		double found = GeneralizedImageOps.get(intensity,5,10);
		double expected = Math.abs(x)+ Math.abs(y);

		assertEquals(expected,found,1);
	}

	@Test
	public void direction() {
		int numFound = BoofTesting.findMethodThenCall(this,"direction",ImplGradientToEdgeFeatures.class,"direction");
		assertEquals(NUM_FUNCTIONS,numFound);
	}

	public void direction( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class derivType = m.getParameterTypes()[0];

		ImageGray derivX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		ImageGray derivY = GeneralizedImageOps.createSingleBand(derivType, width, height);

		GImageMiscOps.fillUniform(derivX, rand, -20, 20);
	    GImageMiscOps.fillUniform(derivY, rand, -20, 20);

		GrayF32 angle = new GrayF32(width,height);

		BoofTesting.checkSubImage(this,"direction",false,m, derivX, derivY, angle);
	}

	public void direction(Method m, ImageGray derivX, ImageGray derivY, GrayF32 angle) throws IllegalAccessException, InvocationTargetException {
		m.invoke(null,derivX,derivY,angle);

		double x = GeneralizedImageOps.get(derivX,5,10);
		double y = GeneralizedImageOps.get(derivY,5,10);
		double found = GeneralizedImageOps.get(angle,5,10);
		double expected = Math.atan(y/x);

		assertEquals(expected,found,0.01);
	}

	@Test
	public void direction2() {
		int numFound = BoofTesting.findMethodThenCall(this,"direction2",ImplGradientToEdgeFeatures.class,"direction2");
		assertEquals(NUM_FUNCTIONS,numFound);
	}

	public void direction2( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class derivType = m.getParameterTypes()[0];

		ImageGray derivX = GeneralizedImageOps.createSingleBand(derivType, width, height);
		ImageGray derivY = GeneralizedImageOps.createSingleBand(derivType, width, height);

		GImageMiscOps.fillUniform(derivX, rand, -20, 20);
	    GImageMiscOps.fillUniform(derivY, rand, -20, 20);

		GrayF32 angle = new GrayF32(width,height);

		BoofTesting.checkSubImage(this,"direction2",false,m, derivX, derivY, angle);
	}

	public void direction2(Method m, ImageGray derivX, ImageGray derivY, GrayF32 angle) throws IllegalAccessException, InvocationTargetException {
		m.invoke(null,derivX,derivY,angle);

		double x = GeneralizedImageOps.get(derivX,5,10);
		double y = GeneralizedImageOps.get(derivY,5,10);
		double found = GeneralizedImageOps.get(angle,5,10);
		double expected = Math.atan2(y,x);

		assertEquals(expected,found,0.01);
	}
}
