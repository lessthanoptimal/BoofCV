/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.census.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.transform.census.CensusNaive;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("rawtypes")
public class TestImplCensusTransformInner {
	final private int w = 25, h = 40;
	Random rand = new Random(234);

	@Test
	void checkAll() {
		int numExpected = 2*4;
		Method methods[] = ImplCensusTransformInner.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m))
				continue;
			try {
//				System.out.println(m.getName());
				if( m.getName().compareTo("dense3x3") == 0 ) {
					dense3x3(m);
				} else if( m.getName().compareTo("dense5x5") == 0 ) {
					dense5x5(m);
				} else if( m.getName().compareTo("sample_S64") == 0 ) {
					sample_S64(m);
				} else if( m.getName().compareTo("sample_IU16") == 0 ) {
					sample_IU16(m);
				} else {
					throw new RuntimeException("Unknown function. "+m.getName());
				}
			} catch (InvocationTargetException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}

			numFound++;
		}

		// update this as needed when new functions are added
		if(numExpected != numFound)
			throw new RuntimeException("Unexpected number of methods: Found "+numFound+"  expected "+numExpected);
	}

	private boolean isTestMethod(Method m ) {
		Class<?> param[] = m.getParameterTypes();

		if( param.length < 1 )
			return false;

		for( int i = 0; i < param.length; i++ ) {
			if( ImageBase.class.isAssignableFrom(param[i] ))
				return true;
		}
		return false;
	}

	void dense3x3(Method m ) throws InvocationTargetException, IllegalAccessException {
		Class inputType = m.getParameterTypes()[0];

		ImageGray input = GeneralizedImageOps.createSingleBand(inputType,w,h);
		GrayU8 found = new GrayU8(w,h);
		GrayU8 expected = new GrayU8(w,h);

		int maxValue = (int)input.getDataType().getMaxValue();
		GImageMiscOps.fillUniform(input,rand,0,maxValue);

		m.invoke(null,input,found);
		CensusNaive.region3x3(input,expected);

		BoofTesting.assertEqualsInner(expected,found,0,1,1,1,1,false);
	}

	void dense5x5( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class inputType = m.getParameterTypes()[0];

		ImageGray input = GeneralizedImageOps.createSingleBand(inputType,w,h);
		GrayS32 found = new GrayS32(w,h);
		GrayS32 expected = new GrayS32(w,h);

		int maxValue = (int)input.getDataType().getMaxValue();
		GImageMiscOps.fillUniform(input,rand,0,maxValue);

		m.invoke(null,input,found);
		CensusNaive.region5x5(input,expected);

		BoofTesting.assertEqualsInner(expected,found,0,2,2,2,2,false);
	}

	void sample_S64( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class inputType = m.getParameterTypes()[0];

		ImageGray input = GeneralizedImageOps.createSingleBand(inputType,w,h);
		GrayS64 found = new GrayS64(w,h);
		GrayS64 expected = new GrayS64(w,h);

		int maxValue = (int)input.getDataType().getMaxValue();
		GImageMiscOps.fillUniform(input,rand,0,maxValue);

		int r = 3;
		FastQueue<Point2D_I32> samples = createSamples(r);
		GrowQueue_I32 indexes = samplesToIndexes(input,samples);

		m.invoke(null,input,r,indexes,found);
		CensusNaive.sample(input,samples,expected);

		BoofTesting.assertEqualsInner(expected,found,0,r,r,r,r,false);
	}

	void sample_IU16( Method m ) throws InvocationTargetException, IllegalAccessException {
		Class inputType = m.getParameterTypes()[0];

		ImageGray input = GeneralizedImageOps.createSingleBand(inputType,w,h);
		InterleavedU16 found = new InterleavedU16(w,h,2);
		InterleavedU16 expected = new InterleavedU16(w,h,2);

		int maxValue = (int)input.getDataType().getMaxValue();
		GImageMiscOps.fillUniform(input,rand,0,maxValue);

		FastQueue<Point2D_I32> samples5x5 = createSamples(2);
		GrowQueue_I32 indexes = samplesToIndexes(input,samples5x5);

		m.invoke(null,input,2,indexes,found);
		CensusNaive.sample(input,samples5x5,expected);

		BoofTesting.assertEqualsInner(expected,found,0,2,2,2,2,false);
	}

	public static GrowQueue_I32 samplesToIndexes( ImageGray input , FastQueue<Point2D_I32> samples ) {
		GrowQueue_I32 indexes = new GrowQueue_I32();
		for (int i = 0; i < samples.size; i++) {
			Point2D_I32 p = samples.get(i);
			indexes.add(p.y*input.stride+p.x);
		}
		return indexes;
	}

	public static FastQueue<Point2D_I32> createSamples( int r ) {
		FastQueue<Point2D_I32> samples = new FastQueue<>(Point2D_I32.class,true);
		for (int y = -r; y <= r; y++) {
			for (int x = -r; x <= r; x++) {
				samples.grow().set(x,y);
			}
		}
		return samples;
	}
}