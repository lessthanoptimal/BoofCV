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

package boofcv.alg.distort.impl;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.ImageDistortCache_SB;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import georegression.struct.affine.Affine2D_F32;
import org.junit.Test;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public abstract class CommonImageDistortCacheTests<T extends ImageGray> {

	Class<T> imageType;
	
	Random rand = new Random(234234);

	Affine2D_F32 affine = new Affine2D_F32(1,2,3,4,5,6);
	PixelTransformAffine_F32 tran = new PixelTransformAffine_F32(affine);

	InterpolatePixelS<T> interp;

	T src;
	T dst0;
	T dst1;

	protected CommonImageDistortCacheTests(Class<T> imageType) {
		this.imageType = imageType;
		interp = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);
		interp.setBorder(FactoryImageBorder.singleValue(imageType, 1));

		src = GeneralizedImageOps.createSingleBand(imageType,200,300);
		dst0 = GeneralizedImageOps.createSingleBand(imageType,200,300);
		dst1 = GeneralizedImageOps.createSingleBand(imageType,200,300);

		GImageMiscOps.addGaussian(src, rand, 10, 0, 255);
	}

	@Test
	public void compareNoCrop() {

		ImageDistort<T,T> standard = FactoryDistort.distortSB(false, interp, imageType);
		ImageDistortCache_SB<T,T> alg = create(interp,imageType);
		
		standard.setModel(tran);
		alg.setModel(tran);
		
		standard.apply(src,dst0);
		alg.apply(src,dst1);

		BoofTesting.assertEquals(dst0, dst1, 1e-4);
	}

	@Test
	public void compareCrop() {

		ImageDistort<T,T> standard = FactoryDistort.distortSB(false, interp, imageType);
		ImageDistortCache_SB<T,T> alg = create(interp,imageType);

		standard.setModel(tran);
		alg.setModel(tran);

		standard.apply(src,dst0,10,30,80,60);
		alg.apply(src,dst1,10,30,80,60);

		BoofTesting.assertEquals(dst0, dst1, 1e-4);
	}
	
	public abstract ImageDistortCache_SB<T,T>
	create(InterpolatePixelS<T> interp, Class<T> imageType );
}
