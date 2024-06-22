/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.cloud.PointCloudReader;
import boofcv.alg.cloud.PointCloudWriter;
import boofcv.struct.mesh.VertexMesh;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestObjFileCodec extends BoofStandardJUnit {
	@Test void encode_decode_cloud() throws IOException {
		List<Point3D_F64> expected = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			expected.add(new Point3D_F64(i*123.45, i - 1.01, i + 2.34));
		}

		var found = new DogArray<>(Point3D_F64::new);

		var output = new StringWriter();
		ObjFileCodec.save(PointCloudReader.wrapF64(expected), output);
		var input = new ByteArrayInputStream(output.toString().getBytes(UTF_8));
		ObjFileCodec.load(input, PointCloudWriter.wrapF64(found));

		assertEquals(expected.size(), found.size);
		for (int i = 0; i < found.size; i++) {
			assertEquals(0.0, found.get(i).distance(expected.get(i)), UtilEjml.TEST_F64);
		}
	}

	@Test void encode_decode_mesh() throws IOException {
		var mesh = new VertexMesh();
		for (int i = 0; i < 10; i++) {
			mesh.vertexes.append(i, 2, 3);
			for (int foo = 0; foo < 3; foo++) {
				int vertIndex = (i*3 + foo)%10;
				mesh.faceVertexes.add(vertIndex);
			}
			mesh.faceOffsets.add(mesh.faceVertexes.size);
		}
		var output = new StringWriter();
		ObjFileCodec.save(mesh, output);

		var foundMesh = new VertexMesh();

		var input = new ByteArrayInputStream(output.toString().getBytes(UTF_8));
		ObjFileCodec.load(input, foundMesh);

		assertEquals(mesh.vertexes.size(), mesh.vertexes.size());
		assertTrue(mesh.faceVertexes.isEquals(foundMesh.faceVertexes));
		assertTrue(mesh.faceOffsets.isEquals(foundMesh.faceOffsets));

		for (int i = 0; i < mesh.vertexes.size(); i++) {
			Point3D_F64 expected = mesh.vertexes.getTemp(i);
			Point3D_F64 found = foundMesh.vertexes.getTemp(i);
			assertEquals(0.0, expected.distance(found));
		}

		for (int i = 0; i < mesh.vertexes.size(); i++) {
			assertEquals(mesh.faceVertexes.get(i), foundMesh.faceVertexes.get(i));
		}
	}
}
