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

package gecv.alg.distort;

import gecv.alg.interpolate.FactoryInterpolation;
import gecv.alg.interpolate.InterpolatePixel;
import gecv.core.image.FactorySingleBandImage;
import gecv.core.image.GeneralizedImageOps;
import gecv.core.image.SingleBandImage;
import gecv.struct.image.ImageBase;
import gecv.testing.GecvTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class TestDistortImageOps {

	Random rand = new Random(234);
	int width = 20;
	int height = 30;

	int numExpected = 3;

	@Test
	public void scale() {
		int total = 0;
		Method[] list = DistortImageOps.class.getMethods();

		for( Method m : list ) {
			if( !m.getName().equals("scale"))
				continue;

			Class<?> param[] = m.getParameterTypes();

			ImageBase input = GeneralizedImageOps.createImage(param[0],width,height);
			ImageBase output = GeneralizedImageOps.createImage(param[1],width/3,height/3);

			GeneralizedImageOps.randomize(input,rand,0,50);

			InterpolatePixel interp = FactoryInterpolation.bilinearPixel((Class<ImageBase>)param[0]);

			GecvTesting.checkSubImage(this,"performScale",true,m,input,output,interp);
			total++;
		}

		assertEquals(numExpected,total);
	}

	public void performScale( Method m , ImageBase input , ImageBase output, InterpolatePixel interp )
			throws InvocationTargetException, IllegalAccessException {
		m.invoke(null,input,output,interp);

		SingleBandImage a = FactorySingleBandImage.wrap(output);

		interp.setImage(input);

		float scaleX = (float)input.width/(float)output.width;
		float scaleY = (float)input.height/(float)output.height;

		if( input.getTypeInfo().isInteger() ) {
			for( int i = 0; i < output.height; i++ ) {
				for( int j = 0; j < output.width; j++ ) {
					float val = interp.get(j*scaleX,i*scaleY);
					assertEquals((int)val,a.get(j,i).intValue());
				}
			}
		} else {
			for( int i = 0; i < output.height; i++ ) {
				for( int j = 0; j < output.width; j++ ) {
					float val = interp.get(j*scaleX,i*scaleY);
					assertEquals(val,a.get(j,i).floatValue(),1e-4);
				}
			}
		}
	}
}
