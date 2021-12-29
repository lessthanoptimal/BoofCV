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

import boofcv.core.image.GeneralizedImageOps;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TestGImageMiscOps extends BaseGClassChecksInMisc {

	long randomSeed = 2345;

	public TestGImageMiscOps() {
		super(GImageMiscOps.class, ImageMiscOps.class);
	}

	@Test void compareToPixelMath() {
		performTests(21);
	}

	@Override
	protected Object[][] createInputParam( Method candidate, Method validation ) {
		Class<?>[] param = validation.getParameterTypes();
		String name = candidate.getName();

		ImageBase inputA = null;
		ImageBase inputSquare = null;

		for (int i = 0; i < param.length; i++) {
			if (ImageBase.class.isAssignableFrom(param[i])) {
				if (ImageGray.class.isAssignableFrom(param[i])) {
					inputA = GeneralizedImageOps.createSingleBand((Class)param[i], width, height);
					inputSquare = GeneralizedImageOps.createSingleBand((Class)param[i], width, width);
				} else {
					inputA = GeneralizedImageOps.createInterleaved((Class)param[i], width, height, numBands);
					inputSquare = GeneralizedImageOps.createInterleaved((Class)param[i], width, width, numBands);
				}
			}
		}
		if (inputA == null)
			throw new RuntimeException("Invalid funciton");

		Object[][] ret = new Object[1][param.length];

		if (name.equals("copy")) {
			ImageBase inputB = inputA.createNew(width, height);

			GImageMiscOps.fillUniform(inputA, rand, 0, 10);
			GImageMiscOps.fillUniform(inputB, rand, 0, 10);

			ret[0][0] = 10;
			ret[0][1] = 15;
			ret[0][2] = 12;
			ret[0][3] = 8;
			ret[0][4] = 5;
			ret[0][5] = 6;

			if (param.length == 9) {
				ret[0][6] = inputA;
				ret[0][7] = FactoryImageBorder.generic(BorderType.WRAP, inputA.getImageType());
				ret[0][8] = inputB;
			} else if (param.length == 8) {
				ret[0][6] = inputA;
				ret[0][7] = inputB;
			} else {
				throw new RuntimeException("Unexpected parameter length");
			}
		} else if (name.equals("fillBand")) {
			ret[0][0] = inputA;
			ret[0][1] = 1;
			ret[0][2] = 3;
		} else if (name.equals("fill")) {
			if (param[1].isArray()) {
				Object array = Array.newInstance(param[1].getComponentType(), numBands);
				for (int i = 0; i < numBands; i++) {
					Array.set(array, i, 2*i + 1);
				}
				ret[0][0] = inputA;
				ret[0][1] = array;
			} else {
				ret[0][0] = inputA;
				ret[0][1] = 3;
			}
		} else if (name.equals("insertBand")) {
			ImageBase input = GeneralizedImageOps.createSingleBand((Class)param[0], width, height);
			ImageBase output = GeneralizedImageOps.createInterleaved((Class)param[2], width, height, numBands);

			GImageMiscOps.fillUniform(input, rand, 0, 10);
			GImageMiscOps.fillUniform(output, rand, 0, 10);

			ret[0][0] = input;
			ret[0][1] = 1;
			ret[0][2] = output;
		} else if (name.equals("fillBorder")) {
			if (param.length == 3) {
				ret[0][0] = inputA;
				ret[0][1] = 3;
				ret[0][2] = 2;
			} else {
				ret[0][0] = inputA;
				ret[0][1] = 3;
				ret[0][2] = 1;
				ret[0][3] = 3;
				ret[0][4] = 2;
				ret[0][5] = 4;
			}
		} else if (name.equals("fillRectangle")) {
			ret[0][0] = inputA;
			ret[0][1] = 3;
			ret[0][2] = 5;
			ret[0][3] = 6;
			ret[0][4] = 3;
			ret[0][5] = 4;
		} else if (name.equals("fillGaussian")) {
			ret[0][0] = inputA;
			ret[0][1] = new Random(randomSeed);
			ret[0][2] = 5;
			ret[0][3] = 3;
			ret[0][4] = 1;
			ret[0][5] = 12;
		} else if (name.equals("fillUniform")) {
			ret[0][0] = inputA;
			ret[0][1] = new Random(randomSeed);
			ret[0][2] = 5;
			ret[0][3] = 30;
		} else if (name.equals("addGaussian")) {
			ret[0][0] = inputA;
			ret[0][1] = new Random(randomSeed);
			ret[0][2] = 5;
			ret[0][3] = 1;
			ret[0][4] = 10;
		} else if (name.equals("addUniform")) {
			ret[0][0] = inputA;
			ret[0][1] = new Random(randomSeed);
			ret[0][2] = 1;
			ret[0][3] = 10;
		} else if (name.equals("flipVertical")) {
			ret[0][0] = inputA;
		} else if (name.equals("flipHorizontal")) {
			ret[0][0] = inputA;
		} else if (name.equals("rotateCW") || name.equals("transpose") || name.equals("rotateCCW")) {
			if (param.length == 1) {
				ret[0][0] = inputSquare;
			} else {
				ret[0][0] = inputA;
				ret[0][1] = inputA.createNew(height, width);
			}
		} else if (name.equals("growBorder")) {
			ignoreSubimage[6] = true; // this has to be resized
			ret[0][0] = inputA;
			ret[0][1] = FactoryImageBorder.generic(BorderType.EXTENDED, inputA.getImageType());
			ret[0][2] = 2;
			ret[0][3] = 3;
			ret[0][4] = 3;
			ret[0][5] = 4;
			ret[0][6] = inputA.createNew(width, height);
		} else {
			throw new RuntimeException("Unknown function: " + name);
		}

		fillRandom(inputA);

		return ret;
	}

	@Override
	protected Object[] reformatForValidation( Method m, Object[] targetParam ) {
		Object[] ret = new Object[targetParam.length];

		for (int i = 0; i < ret.length; i++) {
			if (targetParam[i] instanceof ImageBase) {
				ImageBase img = (ImageBase)targetParam[i];
				ret[i] = ((ImageBase)targetParam[i]).createNew(img.width, img.height);
				((ImageBase)ret[i]).setTo((ImageBase)targetParam[i]);
			} else if (targetParam[i] instanceof Random) {
				ret[i] = new Random(randomSeed);
			} else {
				ret[i] = targetParam[i];
			}
		}

		return ret;
	}

	/**
	 * See if it is inclusive for the upper limit
	 */
	@Test void fillUniform_Int_Inclusive() {
		Class[] types = new Class[]{GrayU8.class, GrayS8.class, GrayU16.class, GrayS32.class, GrayS16.class, GrayS64.class};

		for (Class type : types) {
			ImageGray image = GeneralizedImageOps.createSingleBand(type, width, height);

			GImageMiscOps.fillUniform(image, rand, 0, 10);

			boolean foundMax = false;
			for (int i = 0; i < image.height; i++) {
				for (int j = 0; j < image.width; j++) {
					int found = (int)GeneralizedImageOps.get(image, j, i);
					if (found == 10) {
						foundMax = true;
					}
					if (found < 0 || found > 10) {
						fail("out of range");
					}
				}
			}
			assertTrue(foundMax);
		}
	}

	@Override
	protected void compareResults( Object targetResult, Object[] targetParam, Object validationResult, Object[] validationParam ) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
