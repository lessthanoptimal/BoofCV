/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.derivative.impl;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.ImageGray;
import boofcv.struct.sparse.GradientValue;
import boofcv.struct.sparse.SparseImageGradient;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public abstract class GeneralGradientSparse extends BoofStandardJUnit {

	Class imageType,derivType;
	ImageGray image;
	ImageGray derivX,derivY;

	protected int lower=-1;
	protected int upper=1;

	protected GeneralGradientSparse(Class imageType, Class derivType) {
		this.imageType = imageType;
		this.derivType = derivType;
	}

	public abstract SparseImageGradient createAlg( ImageBorder border );

	public abstract ImageGradient createGradient();

	@BeforeEach
	public void initialize() {
		image = GeneralizedImageOps.createSingleBand(imageType,20,15);
		derivX = GeneralizedImageOps.createSingleBand(derivType,20,15);
		derivY = GeneralizedImageOps.createSingleBand(derivType,20,15);
		GImageMiscOps.fillUniform(image, rand, 0, 255);
	}


	@Test void compareToFullImage_noBorder() {

		createGradient().process(image,derivX,derivY);
		SparseImageGradient alg = createAlg(null);

		alg.setImage(image);

		for (int i = 0; i < image.height; i++) {
			for (int j = 0; j < image.width; j++) {
				if( i >= -lower && j >= -lower && i < image.height-upper && j < image.width-upper ) {
					assertTrue(image.isInBounds(j, i),j + " " + i);
					GradientValue g = alg.compute(j, i);
					double expectedX = GeneralizedImageOps.get(derivX,j,i);
					double expectedY = GeneralizedImageOps.get(derivY,j,i);

					assertEquals(expectedX, g.getX(), 1e-4f);
					assertEquals(expectedY, g.getY(), 1e-4f,j+" "+i);
				} else {
					assertFalse(alg.isInBounds(j, i),j+" "+i);
				}
			}
		}
	}

	@Test void compareToFullImage_Border() {

		ImageBorder border = FactoryImageBorder.single(BorderType.EXTENDED, imageType);

		ImageGradient gradient = createGradient();
		gradient.setBorderType(BorderType.EXTENDED);
		gradient.process(image, derivX, derivY);

		SparseImageGradient alg = createAlg(border);

		alg.setImage(image);

		for (int i = 0; i < image.height; i++) {
			for (int j = 0; j < image.width; j++) {
				assertTrue( image.isInBounds(j, i) , j + " " + i);
				GradientValue g = alg.compute(j, i);
				double expectedX = GeneralizedImageOps.get(derivX,j,i);
				double expectedY = GeneralizedImageOps.get(derivY, j, i);

				assertEquals(expectedX, g.getX(), 1e-4f);
				assertEquals(expectedY, g.getY(), 1e-4f);
			}
		}
	}
}
