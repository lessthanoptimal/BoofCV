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

package boofcv.abst.filter.derivative;

import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.*;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class TestImageGradient_PL {

	Class types[] = new Class[]{GrayU8.class, GrayF32.class};

	Random rand = new Random(234);
	int width = 50,height=60;

	@Test
	public void compareToSingleBand() {
		for( Class inputType : types ) {

			ImageGradient gradientSB = FactoryDerivative.gradient(DerivativeType.PREWITT, ImageType.single(inputType),null);

			Class derivType = gradientSB.getDerivativeType().getImageClass();

			Planar input  = new Planar(inputType,width,height,3);
			Planar derivX = new Planar(derivType,width,height,3);
			Planar derivY = new Planar(derivType,width,height,3);

			GImageMiscOps.fillUniform(input,rand,0,100);

			ImageGradient_PL gradientPL = new ImageGradient_PL(gradientSB,3);

			gradientPL.process(input,derivX,derivY);

			ImageGray sbDerivX = GeneralizedImageOps.createSingleBand(derivType,width,height);
			ImageGray sbDerivY = GeneralizedImageOps.createSingleBand(derivType,width,height);

			for (int i = 0; i < 3; i++) {
				gradientSB.process(input.getBand(i),sbDerivX,sbDerivY);

				BoofTesting.assertEquals(sbDerivX,derivX.getBand(i),1e-8);
				BoofTesting.assertEquals(sbDerivY,derivY.getBand(i),1e-8);
			}

		}
	}
}