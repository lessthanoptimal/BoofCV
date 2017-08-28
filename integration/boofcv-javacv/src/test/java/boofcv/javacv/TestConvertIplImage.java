/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.javacv;

import boofcv.struct.image.*;
import org.junit.Test;

import java.nio.*;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestConvertIplImage {
	@Test
	public void convertFrom_1U8() {
		IplImage ipl = IplImage.create(4, 5, IPL_DEPTH_8U, 1);

		ByteBuffer buff = ipl.createBuffer();
		for (int i = 0; i < 20; i++) {
			buff.put(i,(byte)i);
		}

		GrayU8 found = ConvertIplImage.convertFrom(ipl);

		assertEquals(4, found.width);
		assertEquals(5, found.height);
		for (int i = 0; i < 20; i++) {
			assertEquals(i,found.data[i]);
		}
	}

	@Test
	public void convertFrom_1S16() {
		IplImage ipl = IplImage.create(4, 5, IPL_DEPTH_16S, 1);

		ShortBuffer buff = ipl.createBuffer();
		for (int i = 0; i < 20; i++) {
			buff.put(i,(byte)i);
		}

		GrayS16 found = ConvertIplImage.convertFrom(ipl);

		assertEquals(4, found.width);
		assertEquals(5, found.height);
		for (int i = 0; i < 20; i++) {
			assertEquals(i,found.data[i]);
		}
	}

	@Test
	public void convertFrom_1S32() {
		IplImage ipl = IplImage.create(4, 5, IPL_DEPTH_32S, 1);

		IntBuffer buff = ipl.createBuffer();
		for (int i = 0; i < 20; i++) {
			buff.put(i,(byte)i);
		}

		GrayS32 found = ConvertIplImage.convertFrom(ipl);

		assertEquals(4, found.width);
		assertEquals(5, found.height);
		for (int i = 0; i < 20; i++) {
			assertEquals(i,found.data[i]);
		}
	}

	@Test
	public void convertFrom_1F32() {
		IplImage ipl = IplImage.create(4, 5, IPL_DEPTH_32F, 1);

		FloatBuffer buff = ipl.createBuffer();
		for (int i = 0; i < 20; i++) {
			buff.put(i,(byte)i);
		}

		GrayF32 found = ConvertIplImage.convertFrom(ipl);

		assertEquals(4, found.width);
		assertEquals(5, found.height);
		for (int i = 0; i < 20; i++) {
			assertEquals(i,found.data[i],1e-4f);
		}
	}

	@Test
	public void convertFrom_1F64() {
		IplImage ipl = IplImage.create(4, 5, IPL_DEPTH_64F, 1);

		DoubleBuffer buff = ipl.createBuffer();
		for (int i = 0; i < 20; i++) {
			buff.put(i,(byte)i);
		}

		GrayF64 found = ConvertIplImage.convertFrom(ipl);

		assertEquals(4, found.width);
		assertEquals(5, found.height);

		for (int i = 0; i < 20; i++) {
			assertEquals(i,found.data[i],1e-8);
		}
	}

	@Test
	public void convertFrom_2U8() {
		IplImage ipl = IplImage.create(4, 5, IPL_DEPTH_8U, 2);

		ByteBuffer buff = ipl.createBuffer();
		for (int i = 0; i < 40; i++) {
			buff.put(i,(byte)i);
		}

		InterleavedU8 found = ConvertIplImage.convertFrom(ipl);

		assertEquals(4, found.width);
		assertEquals(5, found.height);
		assertEquals(2, found.numBands);

		for (int i = 0; i < 40; i++) {
			assertEquals(i,found.data[i]);
		}
	}

	@Test
	public void convertFrom_2S16() {
		IplImage ipl = IplImage.create(4, 5, IPL_DEPTH_16S, 2);

		ShortBuffer buff = ipl.createBuffer();
		for (int i = 0; i < 40; i++) {
			buff.put(i,(byte)i);
		}

		InterleavedS16 found = ConvertIplImage.convertFrom(ipl);

		assertEquals(4, found.width);
		assertEquals(5, found.height);
		assertEquals(2, found.numBands);

		for (int i = 0; i < 40; i++) {
			assertEquals(i,found.data[i]);
		}
	}

	@Test
	public void convertFrom_2S32() {
		IplImage ipl = IplImage.create(4, 5, IPL_DEPTH_32S, 2);

		IntBuffer buff = ipl.createBuffer();
		for (int i = 0; i < 40; i++) {
			buff.put(i,(byte)i);
		}

		InterleavedS32 found = ConvertIplImage.convertFrom(ipl);

		assertEquals(4, found.width);
		assertEquals(5, found.height);
		assertEquals(2, found.numBands);

		for (int i = 0; i < 40; i++) {
			assertEquals(i,found.data[i]);
		}
	}

	@Test
	public void convertFrom_2F32() {
		IplImage ipl = IplImage.create(4, 5, IPL_DEPTH_32F, 2);

		FloatBuffer buff = ipl.createBuffer();
		for (int i = 0; i < 40; i++) {
			buff.put(i,(byte)i);
		}

		InterleavedF32 found = ConvertIplImage.convertFrom(ipl);

		assertEquals(4, found.width);
		assertEquals(5, found.height);
		assertEquals(2, found.numBands);

		for (int i = 0; i < 40; i++) {
			assertEquals(i,found.data[i],1e-4f);
		}
	}

	@Test
	public void convertFrom_2F64() {
		IplImage ipl = IplImage.create(4, 5, IPL_DEPTH_64F, 2);

		DoubleBuffer buff = ipl.createBuffer();
		for (int i = 0; i < 40; i++) {
			buff.put(i,(byte)i);
		}

		InterleavedF64 found = ConvertIplImage.convertFrom(ipl);

		assertEquals(4, found.width);
		assertEquals(5, found.height);
		assertEquals(2, found.numBands);

		for (int i = 0; i < 40; i++) {
			assertEquals(i,found.data[i],1e-8);
		}
	}
}