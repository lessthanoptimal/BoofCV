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

package gecv.abst.filter.derivative;

import gecv.alg.filter.derivative.*;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

import java.lang.reflect.Method;

/**
 * Factory for creating different types of {@link DerivativeXY}, which are used to compute
 * the image's derivative.
 *
 * @author Peter Abeles
 */
public class FactoryDerivative {

	public static DerivativeXY<ImageFloat32,ImageFloat32> gaussian_F32( int radius ) {
		return new DerivativeXY_Gaussian_F32(radius);
	}

	public static DerivativeXY<ImageFloat32,ImageFloat32> sobel_F32() {
		Method m = findDerivative_F(GradientSobel.class);
		return new DerivativeXY_Reflection<ImageFloat32,ImageFloat32>(m,true);
	}

	public static DerivativeXY<ImageFloat32,ImageFloat32> three_F32() {
		Method m = findDerivative_F(GradientThree.class);
		return new DerivativeXY_Reflection<ImageFloat32,ImageFloat32>(m,true);
	}

	public static HessianDirectXY<ImageFloat32,ImageFloat32> hessianDirectThree_F32() {
		Method m = findHessian_F(HessianThree.class);
		return new HessianDirectXY_Reflection<ImageFloat32,ImageFloat32>(m,true);
	}

	public static HessianDirectXY<ImageFloat32,ImageFloat32> hessianDirectSobel_F32() {
		Method m = findHessian_F(HessianSobel.class);
		return new HessianDirectXY_Reflection<ImageFloat32,ImageFloat32>(m,true);
	}

	public static DerivativeXY<ImageUInt8, ImageSInt16> sobel_I8() {
		Method m = findDerivative_I(GradientSobel.class);
		return new DerivativeXY_Reflection<ImageUInt8,ImageSInt16>(m,true);
	}

	public static DerivativeXY<ImageUInt8, ImageSInt16> three_I8() {
		Method m = findDerivative_I(GradientThree.class);
		return new DerivativeXY_Reflection<ImageUInt8,ImageSInt16>(m,true);
	}

	public static HessianDirectXY<ImageUInt8, ImageSInt16> hessianDirectThree_I8() {
		Method m = findHessian_I(HessianThree.class);
		return new HessianDirectXY_Reflection<ImageUInt8,ImageSInt16>(m,true);
	}

	public static HessianDirectXY<ImageUInt8, ImageSInt16> hessianDirectSobel_I8() {
		Method m = findHessian_I(HessianSobel.class);
		return new HessianDirectXY_Reflection<ImageUInt8,ImageSInt16>(m,true);
	}

	public static HessianXY<ImageSInt16> hessianThree_I16() {
		Method m = findHessianFromGradient(GradientThree.class,short.class);
		return new HessianXY_Reflection<ImageSInt16>(m,true);
	}

	public static HessianXY< ImageSInt16> hessianSobel_I16() {
		Method m = findHessianFromGradient(GradientSobel.class,short.class);
		return new HessianXY_Reflection<ImageSInt16>(m,true);
	}

	public static HessianXY<ImageFloat32> hessianThree_F32() {
		Method m = findHessianFromGradient(GradientThree.class,float.class);
		return new HessianXY_Reflection<ImageFloat32>(m,true);
	}

	public static HessianXY< ImageFloat32> hessianSobel_F32() {
		Method m = findHessianFromGradient(GradientSobel.class,float.class);
		return new HessianXY_Reflection<ImageFloat32>(m,true);
	}

	private static Method findDerivative_F(Class<?> derivativeClass) {
		Method m;
		try {
			m = derivativeClass.getDeclaredMethod("process", ImageFloat32.class,ImageFloat32.class,ImageFloat32.class,boolean.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return m;
	}

	private static Method findDerivative_I(Class<?> derivativeClass) {
		Method m;
		try {
			m = derivativeClass.getDeclaredMethod("process", ImageUInt8.class,ImageSInt16.class,ImageSInt16.class,boolean.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return m;
	}

	private static Method findHessian_F(Class<?> derivativeClass) {
		Method m;
		try {
			m = derivativeClass.getDeclaredMethod("process", ImageFloat32.class,ImageFloat32.class,ImageFloat32.class,ImageFloat32.class,boolean.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return m;
	}

	private static Method findHessian_I(Class<?> derivativeClass) {
		Method m;
		try {
			m = derivativeClass.getDeclaredMethod("process", ImageUInt8.class,ImageSInt16.class,ImageSInt16.class,ImageSInt16.class,boolean.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return m;
	}

	private static Method findHessianFromGradient(Class<?> derivativeClass, Class<?> dataType ) {
		String name = derivativeClass.getSimpleName().substring(8);
		Method m;
		Class<?> imageType = dataType == float.class ? ImageFloat32.class : ImageSInt16.class;
		try {
			m = HessianFromGradient.class.getDeclaredMethod("hessian"+name, imageType,imageType,imageType,imageType,imageType,boolean.class);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		return m;
	}
}
