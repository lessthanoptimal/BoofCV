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

package gecv.alg.filter.basic.impl;

import gecv.struct.image.ImageInt8;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static junit.framework.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestBinaryNaiveOps {

	@Test
	public void erode4() {
		ImageInt8 input;

		input = createInput(0, 1, 0, 1, 1, 1, 0, 1, 0);
		checkOutput("erode4", input, 1);

		input = createInput(0, 1, 0, 1, 0, 1, 0, 1, 0);
		checkOutput("erode4", input, 0);
		input = createInput(0, 1, 0, 1, 1, 1, 0, 0, 0);
		checkOutput("erode4", input, 0);
		input = createInput(0, 1, 0, 1, 1, 1, 0, 0, 1);
		checkOutput("erode4", input, 0);
	}

	@Test
	public void dilate4() {
		ImageInt8 input;

		input = createInput(0, 1, 0, 1, 1, 1, 0, 1, 0);
		checkOutput("dilate4", input, 1);
		input = createInput(0, 0, 0, 0, 1, 0, 0, 0, 0);
		checkOutput("dilate4", input, 1);
		input = createInput(0, 0, 0, 1, 0, 0, 0, 0, 0);
		checkOutput("dilate4", input, 1);
		input = createInput(0, 1, 0, 0, 0, 0, 0, 1, 0);
		checkOutput("dilate4", input, 1);

		input = createInput(0, 0, 0, 0, 0, 0, 0, 0, 0);
		checkOutput("dilate4", input, 0);
		input = createInput(1, 0, 1, 0, 0, 0, 1, 0, 1);
		checkOutput("dilate4", input, 0);
	}

	@Test
	public void edge4() {
		ImageInt8 input;

		input = createInput(0, 0, 0, 0, 1, 0, 0, 0, 0);
		checkOutput("edge4", input, 1);
		input = createInput(0, 1, 0, 0, 1, 0, 0, 0, 0);
		checkOutput("edge4", input, 1);
		input = createInput(0, 0, 0, 1, 1, 1, 0, 0, 0);
		checkOutput("edge4", input, 1);
		input = createInput(0, 0, 0, 0, 1, 0, 0, 1, 0);
		checkOutput("edge4", input, 1);

		input = createInput(0, 0, 0, 0, 0, 0, 0, 0, 0);
		checkOutput("edge4", input, 0);
		input = createInput(1, 1, 1, 1, 1, 1, 1, 1, 1);
		checkOutput("edge4", input, 0);
		input = createInput(0, 1, 0, 1, 1, 1, 0, 1, 0);
		checkOutput("edge4", input, 0);
		input = createInput(0, 0, 0, 1, 0, 1, 0, 0, 0);
		checkOutput("edge4", input, 0);
	}

	@Test
	public void erode8() {
		ImageInt8 input;

		input = createInput(1, 1, 1, 1, 1, 1, 1, 1, 1);
		checkOutput("erode8", input, 1);

		input = createInput(0, 1, 0, 1, 1, 1, 0, 1, 0);
		checkOutput("erode8", input, 0);
		input = createInput(1, 1, 1, 1, 1, 1, 0, 1, 1);
		checkOutput("erode8", input, 0);
		input = createInput(1, 1, 0, 1, 1, 1, 1, 1, 1);
		checkOutput("erode8", input, 0);
	}

	@Test
	public void dilate8() {
		ImageInt8 input;

		input = createInput(1, 1, 1, 1, 1, 1, 1, 1, 1);
		checkOutput("dilate8", input, 1);
		input = createInput(1, 1, 1, 1, 0, 1, 1, 1, 1);
		checkOutput("dilate8", input, 1);
		input = createInput(1, 1, 1, 1, 1, 1, 1, 1, 0);
		checkOutput("dilate8", input, 1);
		for( int i = 0; i < 9; i++ ) {
			int image[] = new int[9];
			image[i] = 1;
			input = createInput(image);
			checkOutput("dilate8", input, 1);
		}

		input = createInput(0, 0, 0, 0, 0, 0, 0, 0, 0);
		checkOutput("dilate8", input, 0);
	}

	@Test
	public void edge8() {
		ImageInt8 input;

		input = createInput(0, 0, 0, 0, 1, 0, 0, 0, 0);
		checkOutput("edge8", input, 1);
		input = createInput(0, 1, 0, 0, 1, 0, 0, 0, 0);
		checkOutput("edge8", input, 1);
		input = createInput(1, 0, 1, 0, 1, 0, 1, 0, 1);
		checkOutput("edge8", input, 1);
		input = createInput(1, 1, 0, 1, 1, 1, 1, 1, 1);
		checkOutput("edge8", input, 1);

		input = createInput(0, 0, 0, 0, 0, 0, 0, 0, 0);
		checkOutput("edge8", input, 0);
		input = createInput(1, 1, 1, 1, 1, 1, 1, 1, 1);
		checkOutput("edge8", input, 0);
	}

	@Test
	public void removePointNoise() {
		ImageInt8 input;

		input = createInput(1, 1, 1, 1, 1, 1, 1, 1, 1);
		checkOutput("removePointNoise", input, 1);
		input = createInput(1, 1, 1, 1, 0, 1, 1, 1, 1);
		checkOutput("removePointNoise", input, 1);
		input = createInput(1, 1, 1, 1, 0, 1, 0, 1, 1);
		checkOutput("removePointNoise", input, 1);
		input = createInput(0, 1, 1, 1, 1, 0, 0, 1, 0);
		checkOutput("removePointNoise", input, 1);
		input = createInput(0, 1, 0, 1, 1, 0, 1, 1, 1);
		checkOutput("removePointNoise", input, 1);

		input = createInput(0, 1, 0, 1, 0, 0, 0, 1, 0);
		checkOutput("removePointNoise", input, 0);
		input = createInput(0, 1, 0, 1, 0, 0, 0, 1, 1);
		checkOutput("removePointNoise", input, 0);
		input = createInput(0, 1, 0, 0, 1, 0, 0, 0, 0);
		checkOutput("removePointNoise", input, 0);
		input = createInput(0, 0, 0, 1, 1, 0, 0, 0, 0);
		checkOutput("removePointNoise", input, 0);
	}

	private void checkOutput(String methodName, ImageInt8 input, int expected) {
		ImageInt8 output = createOutput(methodName, input);

		assertEquals(expected, output.getU(1, 1));

		// check it against sub-images
		ImageInt8 temp = new ImageInt8(3 + output.width, 4 + output.height);
		temp = temp.subimage(0, 0, output.width, output.height);
		temp.setTo(input);

		output = createOutput(methodName, temp);
		assertEquals(output.getU(1, 1), expected);
	}

	private ImageInt8 createInput(int... image) {
		ImageInt8 input = new ImageInt8(3, 3);
		for (int i = 0; i < 9; i++) {
			input.data[i] = (byte) image[i];
		}
		return input;
	}

	private ImageInt8 createOutput(String methodName, ImageInt8 input) {
		try {
			Method m = BinaryNaiveOps.class.getMethod(methodName, ImageInt8.class, ImageInt8.class);
			return (ImageInt8) m.invoke(null, input, (ImageInt8) null);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
