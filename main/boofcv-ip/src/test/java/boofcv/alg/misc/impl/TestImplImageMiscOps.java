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

package boofcv.alg.misc.impl;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestImplImageMiscOps {

	Random rand = new Random(234);
	int width=20,height=15;

	@Test
	public void checkAll() {
		int numExpected = 6;
		Method methods[] = ImplImageMiscOps.class.getMethods();

		// sanity check to make sure the functions are being found
		int numFound = 0;
		for (Method m : methods) {
			if( !isTestMethod(m)) {
				continue;
			}
			try {
				if( m.getName().compareTo("growBorder") == 0 ) {
					growBorder(m);
				} else {
					throw new RuntimeException("Unknown function: "+m.getName());
				}
				numFound++;
			} catch (InvocationTargetException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
		assertEquals(numExpected, numFound);
	}

	private void growBorder(Method m) throws InvocationTargetException, IllegalAccessException {
		Class[] paramTypes = m.getParameterTypes();
		ImageGray src = GeneralizedImageOps.createSingleBand(paramTypes[0],width,height);
		ImageGray dst = GeneralizedImageOps.createSingleBand(paramTypes[4],1,1);
		ImageBorder extend = FactoryImageBorder.generic(BorderType.EXTENDED,src.getImageType());
		int radiusX = 2;
		int radiusY = 3;

		GImageMiscOps.fillUniform(src,rand,0,100);

		m.invoke(null,src,extend, radiusX,radiusY, dst);

		assertEquals(width+2*radiusX,dst.width);
		assertEquals(height+2*radiusY,dst.height);

		for (int y = 0; y < dst.height; y++) {
			int yy = Math.min(src.height-1,Math.max(0,y-radiusY));

			for (int x = 0; x < dst.width; x++) {
				int xx = Math.min(src.width-1,Math.max(0,x-radiusX));

				// manually do the extend border
				double expected = GeneralizedImageOps.get(src,xx,yy);
				double found = GeneralizedImageOps.get(dst,x,y);

				assertEquals(expected,found, UtilEjml.TEST_F64);
			}
		}

	}

	private boolean isTestMethod(Method m ) {
		Class param[] = m.getParameterTypes();

		if( param.length < 1 ) {
			return false;
		}

		for (int i = 0; i < param.length; i++) {
			if( ImageBase.class.isAssignableFrom(param[i]) ) {
				return true;
			}
		}
		return false;
	}
}
