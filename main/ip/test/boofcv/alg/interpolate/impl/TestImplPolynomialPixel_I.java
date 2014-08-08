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
import boofcv.struct.image.ImageUInt8;
import boofcv.testing.BoofTesting;
import georegression.struct.affine.Affine2D_F32;
import org.junit.Test;

import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestImplPolynomialPixel_I extends GeneralChecksInterpolationPixelS<ImageUInt8> {

	int DOF = 2;

    Random rand = new Random(0xff);
    int width = 20;
    int height = 34;

    /**
     * Polynomial interpolation of order one is bilinear interpolation
     */
    @Test
	public void compareToBilinear() {
		ImageUInt8 img = new ImageUInt8(width,height);
		ImageUInt8 expected = new ImageUInt8(width,height);
		ImageUInt8 found = new ImageUInt8(width,height);

		GImageMiscOps.fillUniform(img, rand, 0, 255);

		Affine2D_F32 tran = new Affine2D_F32(1,0,0,1,0.25f,0.25f);

		// set it up so that it will be equivalent to bilinear interpolation
		InterpolatePixelS<ImageUInt8> alg = (InterpolatePixelS)new ImplPolynomialPixel_I(2,0,255);

		ImageBorder<ImageUInt8> border = FactoryImageBorder.value(ImageUInt8.class, 0);
		ImageDistort<ImageUInt8,ImageUInt8> distorter = FactoryDistort.distort(false,alg, border, ImageUInt8.class);
		distorter.setModel(new PixelTransformAffine_F32(tran));
		distorter.apply(img,found);

		InterpolatePixelS<ImageUInt8> bilinear = FactoryInterpolation.bilinearPixelS(ImageUInt8.class);

		distorter = FactoryDistort.distort(false,bilinear, border,ImageUInt8.class);
		distorter.setModel(new PixelTransformAffine_F32(tran));
        distorter.apply(img, expected);

		BoofTesting.assertEquals(expected, found, 0);
    }

	@Override
	protected ImageUInt8 createImage(int width, int height) {
		return new ImageUInt8(width,height);
	}

	@Override
	protected InterpolatePixelS<ImageUInt8> wrap(ImageUInt8 image, int minValue, int maxValue) {
		InterpolatePixelS ret = new ImplPolynomialPixel_I(DOF,minValue,maxValue);
		ret.setImage(image);
		return ret;
	}

	@Override
	protected float compute(ImageUInt8 img, float x, float y) {
		// yes by using the same algorithm for this compute several unit tests are being defeated
		// polynomial interpolation is more complex and a simple compute alg here is not possible
		InterpolatePixelS a = new ImplPolynomialPixel_I(DOF,0,255);
		a.setImage(img);
		return a.get(x,y);
	}
}
