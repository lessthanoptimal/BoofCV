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

package boofcv.struct.mesh;

import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestVertexMesh extends BoofStandardJUnit {
	@Test void setTo() {checkSetTo(VertexMesh.class, true);}

	@Test void reset() throws Exception {checkReset(VertexMesh.class, "reset");}

	@Test void addShape() {
		List<Point3D_F64> shapeA = createRandomShape(3);
		List<Point3D_F64> shapeB = createRandomShape(4);

		var alg = new VertexMesh();
		alg.addShape(shapeA);
		alg.addShape(shapeB);

		assertEquals(2, alg.size());
		assertEquals(shapeA.size() + shapeB.size(), alg.vertexes.size());
		assertEquals(alg.vertexes.size(), alg.indexes.size());

		var found = new DogArray<>(Point3D_F64::new);
		alg.getShape(0, found);
		assertIdentical(shapeA, found.toList());
		alg.getShape(1, found);
		assertIdentical(shapeB, found.toList());
	}

	@Test void getTexture() {
		fail("implement");
	}

	private List<Point3D_F64> createRandomShape( int count ) {
		var shape = new ArrayList<Point3D_F64>();
		for (int i = 0; i < count; i++) {
			shape.add(new Point3D_F64(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()));
		}
		return shape;
	}

	private void assertIdentical( List<Point3D_F64> a, List<Point3D_F64> b ) {
		assertEquals(a.size(), b.size());
		for (int i = 0; i < a.size(); i++) {
			assertEquals(0.0, a.get(i).distance(b.get(i)));
		}
	}
}
