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

package boofcv.alg.misc;

import boofcv.BoofTesting;
import boofcv.alg.misc.impl.ImplImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.misc.BoofLambdas;
import boofcv.struct.border.BorderType;
import boofcv.struct.border.ImageBorder;
import boofcv.struct.image.*;
import boofcv.testing.CompareIdenticalFunctions;
import joptsimple.internal.Objects;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Random;

/**
 * Compares function to the code in ImplImageBandMath
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "SuspiciousNameCombination", "unchecked"})
public abstract class CompareToImplImageMiscOps extends CompareIdenticalFunctions {

	private final int width;
	private final int height;
//	private int numBands = 3;

	protected CompareToImplImageMiscOps( Class testClass, int width, int height ) {
		super(testClass, ImplImageMiscOps.class);
		this.width = width;
		this.height = height;
	}

	@Test
	void compareFunctions() {
		super.performTests(29*6 + 4*8);
	}

	@Override
	protected boolean isTestMethod( Method m ) {
		return super.isTestMethod(m);
	}

	@Override
	protected Object[] reformatForValidation( Method m, Object[] targetParam ) {
		Object[] ret = super.reformatForValidation(m, targetParam);

		for (int i = 0; i < targetParam.length; i++) {
			if (ret[i] == null)
				continue;
			if (Random.class.isAssignableFrom(targetParam[i].getClass())) {
				ret[i] = new Random(345);
			}
		}

		return ret;
	}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {

//		System.out.println(candidate.getName()+" "+candidate.getParameterTypes().length);

		Class[] types = candidate.getParameterTypes();

		// declare all images
		Object[][] output = new Object[1][types.length];
		ImageType imageType = null;
		for (int i = 0; i < types.length; i++) {
			if (ImageBase.class.isAssignableFrom(types[i])) {
				output[0][i] = GeneralizedImageOps.createImage(types[i], width, height, 2);
				GImageMiscOps.fillUniform((ImageBase)output[0][i], rand, 0, 100);
				imageType = ((ImageBase)output[0][i]).getImageType();
			} else if (ImageBorder.class.isAssignableFrom(types[i])) {
				Objects.ensureNotNull(imageType);
				output[0][i] = FactoryImageBorder.generic(BorderType.EXTENDED, imageType);
			} else if (Random.class.isAssignableFrom(types[i])) {
				output[0][i] = new Random(345);
			}
		}

		Object[] p = output[0];
		switch( candidate.getName() ) {
			case "copy":p[0] = 1;p[1] = 2;p[2] = 1;p[3] = 2;p[4] = width-1;p[5] = height-2; break;
			case "fill":
				if (!types[1].isArray())
					p[1] = BoofTesting.primitive(2, types[1]);
				else
					p[1] = BoofTesting.randomArray(types[1], 2, rand);
				break;
			case "fillBand":p[1]=0;p[2]=BoofTesting.primitive(8,types[1]);break;
			case "insertBand":p[1]=0;break;
			case "extractBand":p[1]=0;break;
			case "fillBorder":
				p[1] = BoofTesting.primitive(2, types[1]);
				if (types.length == 3) {
					p[2] = 4;
				} else {
					p[2] = 1;p[3] = 2;p[4] = 3;p[5] = 4;
				} break;
			case "fillRectangle": p[1]=BoofTesting.primitive(5,types[1]);p[2] = 1;p[3] = 2;p[4] = width-1;p[5] = height-2; break;
			case "fillUniform": p[2]=BoofTesting.primitive(2,types[2]);p[3]=BoofTesting.primitive(60,types[3]);break;
			case "fillGaussian":
				p[2]=BoofTesting.primitive(2,types[2]);p[3]=BoofTesting.primitive(20,types[3]);
				p[4]=BoofTesting.primitive(2,types[4]);p[5]=BoofTesting.primitive(20,types[5]);
				break;
			case "flipVertical":
			case "flipHorizontal":
				break;
			case "transpose":
			case "rotateCW":
			case "rotateCCW":
				if (p.length == 1)
					((ImageBase)p[0]).reshape(width, width);
				else
					((ImageBase)p[1]).reshape(height, width);
				break;
			case "growBorder":
				((ImageBase)p[6]).reshape(width+4,height+6);
				p[2] = 1;p[3] = 2;p[4] = 3;p[5] = 4; break;
			case "addUniform": p[2]=BoofTesting.primitive(2,types[2]);p[3]=BoofTesting.primitive(60,types[3]);break;
			case "addGaussian":
				p[2]=BoofTesting.primitive(2,types[2]);p[3]=BoofTesting.primitive(1,types[3]);
				p[4]=BoofTesting.primitive(60,types[4]);;
				break;
			case "findAndProcess":
				// this isn't a great test since it does nothing. At least you can tell if it blows up or not
				p[1]=createMatchLambda(types[0]);
				p[2]=(BoofLambdas.ProcessIIB)(x,y)-> false;
				break;

			default:
				throw new RuntimeException("Implement " + candidate.getName());
		}
		return output;
	}

	public static Object createMatchLambda( Class type ) {
		if (GrayI8.class.isAssignableFrom(type)) {
			return (BoofLambdas.Match_I8)value -> value%2==0;
		} else if (GrayI16.class.isAssignableFrom(type)) {
			return (BoofLambdas.Match_I16)value -> value%2==0;
		} else if (GrayS32.class.isAssignableFrom(type)) {
			return (BoofLambdas.Match_S32)value -> value%2==0;
		} else if (GrayS64.class.isAssignableFrom(type)) {
			return (BoofLambdas.Match_S64)value -> value%2==0;
		} else if (GrayF32.class.isAssignableFrom(type)) {
			return (BoofLambdas.Match_F32)value -> ((int)value)%2==0;
		} else if (GrayF64.class.isAssignableFrom(type)) {
			return (BoofLambdas.Match_F64)value -> ((int)value)%2==0;
		} else {
			throw new RuntimeException("Unknown");
		}
	}
}
