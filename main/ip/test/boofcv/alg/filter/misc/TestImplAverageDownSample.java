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

package boofcv.alg.filter.misc;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageGray;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestImplAverageDownSample {


	public static List<Method> find( String name ) {
		List<Method> ret = new ArrayList<>();

		Method methods[] = ImplAverageDownSample.class.getMethods();

		for( Method m : methods ) {
			if( m.getName().equals(name) ) {
				ret.add(m);
			}
		}

		return ret;
	}

	/**
	 * easy case, should be a perfect copy
	 */
	@Test
	public void horizontal_1_to_1() throws InvocationTargetException, IllegalAccessException {

		List<Method> methods = find("horizontal");

		for( Method m : methods ) {
			Class typeSrc = m.getParameterTypes()[0];
			Class typeDst = m.getParameterTypes()[1];

			ImageGray src = GeneralizedImageOps.createSingleBand(typeSrc,6,3);
			ImageGray dst = GeneralizedImageOps.createSingleBand(typeDst,6,3);

			fillHorizontal(src);
			m.invoke(null,src,dst);

			for (int y = 0; y < src.height; y++) {
				for (int x = 0; x < src.width; x++) {
					double found = GeneralizedImageOps.get(dst,x,y);
					assertEquals(x,found,1e-4f);
				}
			}
		}

		assertEquals(4,methods.size());
	}

	/**
	 * Two pixels should be averaged together at a time
	 */
	@Test
	public void horizontal_2_to_1() throws InvocationTargetException, IllegalAccessException {
		List<Method> methods = find("horizontal");

		for( Method m : methods ) {
			Class typeSrc = m.getParameterTypes()[0];
			Class typeDst = m.getParameterTypes()[1];

			ImageGray src = GeneralizedImageOps.createSingleBand(typeSrc,6,3);
			ImageGray dst = GeneralizedImageOps.createSingleBand(typeDst,3,3);

			fillHorizontal(src);
			m.invoke(null,src,dst);

			for (int y = 0; y < src.height; y++) {
				assertEquals(0.5f,GeneralizedImageOps.get(dst,0,y),1e-4f);
				assertEquals(2.5f,GeneralizedImageOps.get(dst,1,y),1e-4f);
				assertEquals(4.5f,GeneralizedImageOps.get(dst,2,y),1e-4f);
			}
		}

		assertEquals(4,methods.size());
	}

	/**
	 * The division will not be along pixels and symmetries are avoided
	 */
	@Test
	public void horizontal_3_to_2() throws InvocationTargetException, IllegalAccessException {
		List<Method> methods = find("horizontal");

		for( Method m : methods ) {
			Class typeSrc = m.getParameterTypes()[0];
			Class typeDst = m.getParameterTypes()[1];

//			System.out.println(typeSrc+"   "+typeDst);

			ImageGray src = GeneralizedImageOps.createSingleBand(typeSrc,9,3);
			ImageGray dst = GeneralizedImageOps.createSingleBand(typeDst,4,3);

			fillHorizontal(src);
			m.invoke(null,src,dst);

			for (int y = 0; y < src.height; y++) {
				assertEquals(0.6666667f,GeneralizedImageOps.get(dst,0,y),1e-4f);
				assertEquals(2.8888889f,GeneralizedImageOps.get(dst,1,y),1e-4f);
				assertEquals(5.1111111f,GeneralizedImageOps.get(dst,2,y),1e-4f);
				assertEquals(7.3333333f,GeneralizedImageOps.get(dst,3,y),1e-4f);
			}
		}

		assertEquals(4,methods.size());
	}

	private void fillHorizontal(ImageGray src) {
		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++) {
				GeneralizedImageOps.set(src,x,y,x);
			}
		}
	}

	@Test
	public void vertical_1_to_1() throws InvocationTargetException, IllegalAccessException {
		List<Method> methods = find("vertical");

		for( Method m : methods ) {
			Class typeSrc = m.getParameterTypes()[0];
			Class typeDst = m.getParameterTypes()[1];

			ImageGray src = GeneralizedImageOps.createSingleBand(typeSrc,3,6);
			ImageGray dst = GeneralizedImageOps.createSingleBand(typeDst,3,6);

			fillVertical(src);
			m.invoke(null,src,dst);

			for (int y = 0; y < src.height; y++) {
				for (int x = 0; x < src.width; x++) {
					assertEquals(y,GeneralizedImageOps.get(dst,x,y),1e-4);
				}
			}
		}

		assertEquals(4,methods.size());
	}

	@Test
	public void vertical_2_to_1() throws InvocationTargetException, IllegalAccessException {
		List<Method> methods = find("vertical");

		for( Method m : methods ) {
			Class typeSrc = m.getParameterTypes()[0];
			Class typeDst = m.getParameterTypes()[1];

//			System.out.println(typeSrc+"   "+typeDst);

			ImageGray src = GeneralizedImageOps.createSingleBand(typeSrc,3,6);
			ImageGray dst = GeneralizedImageOps.createSingleBand(typeDst,3,3);

			fillVertical(src);
			m.invoke(null,src,dst);

			double expected[];
			if( dst.getDataType().isInteger() ) {
				expected = new double[]{1,3,5};
			} else {
				expected = new double[]{0.5,2.5,4.5};
			}

			for (int x = 0; x < src.width; x++) {
				for (int y = 0; y < dst.height; y++) {
					assertEquals(expected[y],GeneralizedImageOps.get(dst,x,y),1e-4);
				}
			}
		}

		assertEquals(4,methods.size());
	}

	@Test
	public void vertical_3_to_2() throws InvocationTargetException, IllegalAccessException {
		List<Method> methods = find("vertical");

		for( Method m : methods ) {
			Class typeSrc = m.getParameterTypes()[0];
			Class typeDst = m.getParameterTypes()[1];

//			System.out.println(typeSrc+"   "+typeDst);

			ImageGray src = GeneralizedImageOps.createSingleBand(typeSrc,3,9);
			ImageGray dst = GeneralizedImageOps.createSingleBand(typeDst,3,4);

			fillVertical(src);
			m.invoke(null,src,dst);

			double expected[];
			if( dst.getDataType().isInteger() ) {
				expected = new double[]{1,3,5,7};
			} else {
				expected = new double[]{0.6666667f,2.8888889f,5.1111111f,7.3333333f};
			}

			for (int x = 0; x < src.width; x++) {
				for (int y = 0; y < dst.height; y++) {
					assertEquals(expected[y],GeneralizedImageOps.get(dst,x,y),1e-4);
				}
			}
		}

		assertEquals(4,methods.size());
	}

	private void fillVertical(ImageGray src) {
		for (int x = 0; x < src.width; x++) {
			for (int y = 0; y < src.height; y++) {
				GeneralizedImageOps.set(src,x,y,y);
			}
		}
	}
}
