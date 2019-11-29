/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.io.points.impl;

import boofcv.struct.Point3dRgbI_F32;
import georegression.struct.point.Point3D_F32;
import org.ddogleg.struct.FastQueue;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestPlyCodec_F32 {
	@Test
	void encode_decode_3D_ascii() throws IOException {
		List<Point3D_F32> expected = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			expected.add( new Point3D_F32(i*123.45f,i-1.01f,i+2.34f));
		}

		FastQueue<Point3D_F32> found = new FastQueue<>(Point3D_F32.class,true);

		Writer writer = new StringWriter();
		PlyCodec_F32.saveAscii(expected,writer);
		Reader reader = new StringReader(writer.toString());
		PlyCodec_F32.read(reader,found);

		assertEquals(expected.size(),found.size);
		for (int i = 0; i < found.size; i++) {
			assertEquals(0.0f,found.get(i).distance(expected.get(i)), UtilEjml.TEST_F32);
		}
	}

	@Test
	void encode_decode_3DRGB_ascii() throws IOException {
		List<Point3dRgbI_F32> expected = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			int r = (10*i)&0xFF;
			int g = (28*i)&0xFF;
			int b = (58*i)&0xFF;

			int rgb = r << 16 | g << 16 | b;

			expected.add( new Point3dRgbI_F32(i*123.45f,i-1.01f,i+2.34f,rgb));
		}

		FastQueue<Point3dRgbI_F32> found = new FastQueue<>(Point3dRgbI_F32.class,true);

		Writer writer = new StringWriter();
		PlyCodec_F32.saveAsciiRgbI(expected,writer);
		Reader reader = new StringReader(writer.toString());
		PlyCodec_F32.readRgbI(reader,found);

		assertEquals(expected.size(),found.size);
		for (int i = 0; i < found.size; i++) {
			assertEquals(0.0f,found.get(i).distance(expected.get(i)), UtilEjml.TEST_F32);
		}
	}

}