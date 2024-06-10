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
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
		var shape = new DogArray<>(Point3D_F64::new);

		var alg = new VertexMesh();
		alg.addShape(shape.resize(3).toList());
		alg.addShape(shape.resize(4).toList());
		alg.addShape(shape.resize(5).toList());

		alg.addTexture(3, new float[]{1,2,3,4,5,6,7,8,9,10});
		alg.addTexture(4, new float[12]);
		alg.addTexture(5, new float[10]);

		var found = new DogArray<>(Point2D_F32::new);
		alg.getTexture(0, found);
		assertEquals(3, found.size);
		assertTrue(found.get(0).isIdentical(1,2));
		assertTrue(found.get(1).isIdentical(3,4));
		assertTrue(found.get(2).isIdentical(5,6));
		alg.getTexture(1, found);
		assertEquals(4, found.size);
		alg.getTexture(2, found);
		assertEquals(5, found.size);
	}

	@Test void computeFaceNormals() {
		var shape = new DogArray<>(Point3D_F64::new);

		shape.grow().setTo(0,0,0);
		shape.grow().setTo(0,1,0);
		shape.grow().setTo(1,1,0);

		// create triangles that will have normals +1 and -1
		var alg = new VertexMesh();
		alg.addShape(shape.toList());
		shape.reverse();
		alg.addShape(shape.toList());

		alg.computeFaceNormals();
		assertEquals(0.0, alg.faceNormals.getTemp(0).distance(0,0,-1), UtilEjml.TEST_F64);
		assertEquals(0.0, alg.faceNormals.getTemp(1).distance(0,0,1), UtilEjml.TEST_F64);

		// try it with a non-triangle
		shape.reset();
		shape.grow().setTo(0,0,0);
		shape.grow().setTo(0,2,0);
		shape.grow().setTo(2,2,0);
		shape.grow().setTo(2,9,0);
		alg.reset();
		alg.addShape(shape.toList());
		assertEquals(0.0, alg.faceNormals.getTemp(0).distance(0,0,-1), UtilEjml.TEST_F64);
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
