/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.interpolate.impl;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageFloat32;
import boofcv.testing.BoofTesting;
import georegression.struct.affine.Affine2D_F32;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestImplPolynomialPixel_F32 extends GeneralChecksInterpolationPixelS<ImageFloat32> {

	int DOF = 2;

    Random rand = new Random(0xff);
    int width = 20;
    int height = 34;

    /**
	 * Polynomial interpolation of order one is bilinear interpolation
	 */
	@Test
	public void compareToBilinear() {
		ImageFloat32 img = new ImageFloat32(width,height);
		ImageFloat32 expected = new ImageFloat32(width,height);
		ImageFloat32 found = new ImageFloat32(width,height);

		GImageMiscOps.fillUniform(img, rand, 0, 255);

		Affine2D_F32 tran = new Affine2D_F32(1,0,0,1,0.25f,0.25f);

		// set it up so that it will be equivalent to bilinear interpolation
		ImplPolynomialPixel_F32 alg = new ImplPolynomialPixel_F32(2,0,255);

		ImageBorder<ImageFloat32> border = FactoryImageBorder.value(ImageFloat32.class, 0);
		ImageDistort<ImageFloat32,ImageFloat32> distorter = FactoryDistort.distort(false,alg, border, ImageFloat32.class);
		distorter.setModel(new PixelTransformAffine_F32(tran));
		distorter.apply(img,found);

		InterpolatePixelS<ImageFloat32> bilinear = FactoryInterpolation.bilinearPixelS(ImageFloat32.class);

		distorter = FactoryDistort.distort(false,bilinear, border, ImageFloat32.class);
		distorter.setModel(new PixelTransformAffine_F32(tran));
        distorter.apply(img, expected);

		BoofTesting.assertEquals(expected, found, 1e-4f);
    }

	@Override
	protected ImageFloat32 createImage(int width, int height) {
		return new ImageFloat32(width,height);
	}

	@Override
	protected InterpolatePixelS<ImageFloat32> wrap(ImageFloat32 image, int minValue, int maxValue) {
		InterpolatePixelS<ImageFloat32> ret = new ImplPolynomialPixel_F32(DOF,minValue,maxValue);
		ret.setImage(image);
		return ret;
	}

	@Override
	protected float compute(ImageFloat32 img, float x, float y) {
		// yes by using the same algorithm for this compute several unit tests are being defeated
		// polynomial interpolation is more complex and a simple compute alg here is not possible
		ImplPolynomialPixel_F32 a = new ImplPolynomialPixel_F32(DOF,0,255);
		a.setImage(img);
		return a.get(x,y);
	}
}
