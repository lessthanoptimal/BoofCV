/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.transform.wavelet;

import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.transform.wavelet.FactoryWaveletDaub;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.wavelet.WaveletDescription;
import boofcv.testing.BoofTesting;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Random;


/**
 * @author Peter Abeles
 */
public class TestWaveletTransformOps {

	Random rand = new Random(234);
	int width = 250;
	int height = 300;

   Class<?> typeInput;

	Class<?> types[]={ImageFloat32.class,ImageSInt32.class};

	/**
	 * See if images which are the smallest possible can be transformed.
	 */
	@Test
	public void smallImage(){
		for( Class t : types ) {
			testSmallImage(t);
		}
	}

	public <T extends ImageSingleBand> void testSmallImage( Class<T> typeInput ) {
		this.typeInput = typeInput;
		WaveletDescription<?> desc = createDesc(typeInput);

		int length = Math.max(desc.getForward().getScalingLength(),desc.getForward().getWaveletLength());

		for( int i = length; i < 20; i++ ) {

			T input = GeneralizedImageOps.createSingleBand(typeInput, i, i);
			T output = GeneralizedImageOps.createSingleBand(typeInput, input.width + (input.width % 2), input.height + (input.height % 2));
			T found = GeneralizedImageOps.createSingleBand(typeInput, input.width, input.height);

			GeneralizedImageOps.randomize(input,rand,0,50);

			invokeTransform("transform1","inverse1",desc, input, output, found);

			BoofTesting.assertEqualsGeneric(input,found,0,1e-4f);
		}
	}

	private WaveletDescription<?> createDesc(Class<?> typeInput) {
		boolean isFloat = GeneralizedImageOps.isFloatingPoint(typeInput);

		WaveletDescription<?> desc;

		if( isFloat )
			desc = FactoryWaveletDaub.daubJ_F32(4);
		else
			desc = FactoryWaveletDaub.biorthogonal_I32(5, BorderType.WRAP);
		return desc;
	}


	@Test
	public void multipleLevel() {
		for( Class<?> t : types ) {
			testMultipleLEvels(t);
		}
	}

	private void testMultipleLEvels( Class typeInput ) {
		this.typeInput = typeInput;

		WaveletDescription<?> desc = createDesc(typeInput);

		// try different sized images
		for( int adjust = 0; adjust < 5; adjust++ ) {
			int w = width+adjust;
			int h = height+adjust;
			ImageSingleBand input = GeneralizedImageOps.createSingleBand(typeInput, w, h);
			ImageSingleBand found = GeneralizedImageOps.createSingleBand(typeInput, w, h);

			GeneralizedImageOps.randomize(input,rand,0,50);

			for( int level = 1; level <= 5; level++ ) {
				ImageDimension dim = UtilWavelet.transformDimension(w,h,level);
				ImageSingleBand output = GeneralizedImageOps.createSingleBand(typeInput, dim.width, dim.height);
//				System.out.println("adjust "+adjust+" level "+level+" scale "+ div);

				invokeTransform("transformN","inverseN",desc, input.clone(), output, found,level);

				BoofTesting.assertEqualsGeneric(input,found,0,1e-4f);
			}
		}
	}

	private void invokeTransform(String nameTran , String nameInv , WaveletDescription<?> desc, ImageSingleBand input, ImageSingleBand output, ImageSingleBand found) {
		try {
			Method m = WaveletTransformOps.class.getMethod(nameTran,desc.getClass(),typeInput,typeInput,typeInput);
			m.invoke(null,desc,input,output,null);
			m = WaveletTransformOps.class.getMethod(nameInv,desc.getClass(),typeInput,typeInput,typeInput);
			m.invoke(null,desc,output,found,null);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private void invokeTransform(String nameTran , String nameInv ,
								 WaveletDescription<?> desc,
								 ImageSingleBand input, ImageSingleBand output, ImageSingleBand found,
								 int level) {
		try {
			Method m = WaveletTransformOps.class.getMethod(nameTran,desc.getClass(),typeInput,typeInput,typeInput,int.class);
			m.invoke(null,desc,input,output,null,level);
			m = WaveletTransformOps.class.getMethod(nameInv,desc.getClass(),typeInput,typeInput,typeInput,int.class);
			m.invoke(null,desc,output,found,null,level);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
