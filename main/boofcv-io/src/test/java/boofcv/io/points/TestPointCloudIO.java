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

package boofcv.io.points;

import boofcv.io.points.PointCloudIO.Format;
import georegression.struct.point.Point3D_F32;
import georegression.struct.point.Point3D_F64;
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
class TestPointCloudIO {
	@Test
	void encode_decode_3D32F() throws IOException {
		List<Point3D_F32> expected = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			expected.add( new Point3D_F32(i*123.45f,i-1.01f,i+2.34f));
		}

		Format[] formats = new Format[]{Format.PLY_ASCII};
		for( Format f : formats ) {
			FastQueue<Point3D_F32> found = new FastQueue<>(Point3D_F32.class,true);
			found.grow().set(1,1,1);

			Writer writer = new StringWriter();
			PointCloudIO.save3D32F(f,expected,writer);
			Reader reader = new StringReader(writer.toString());
			PointCloudIO.load3D32F(f,reader,found);

			// make sure it didn't clear the points
			assertEquals(expected.size()+1,found.size);
			for (int i = 0; i < expected.size(); i++) {
				assertEquals(0.0,found.get(i+1).distance(expected.get(i)), UtilEjml.TEST_F32);
			}
		}
	}

	@Test
	void encode_decode_3D64F() throws IOException {
		List<Point3D_F64> expected = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			expected.add( new Point3D_F64(i*123.45,i-1.01,i+2.34));
		}

		Format[] formats = new Format[]{Format.PLY_ASCII};
		for( Format f : formats ) {
			FastQueue<Point3D_F64> found = new FastQueue<>(Point3D_F64.class,true);
			found.grow().set(1,1,1);

			Writer writer = new StringWriter();
			PointCloudIO.save3D64F(f,expected,writer);
			Reader reader = new StringReader(writer.toString());
			PointCloudIO.load3D64F(f,reader,found);

			// make sure it didn't clear the points
			assertEquals(expected.size()+1,found.size);
			for (int i = 0; i < expected.size(); i++) {
				assertEquals(0.0,found.get(i+1).distance(expected.get(i)), UtilEjml.TEST_F64);
			}
		}
	}
}