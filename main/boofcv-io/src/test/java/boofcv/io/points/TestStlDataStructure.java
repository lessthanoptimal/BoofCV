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

package boofcv.io.points;

import boofcv.struct.mesh.VertexMesh;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestStlDataStructure extends BoofStandardJUnit {
	@Test void setTo() {checkSetTo(StlDataStructure.class, true);}

	@Test void reset() throws Exception {checkReset(StlDataStructure.class, "reset");}

	@Test void addFacetWithNormal() {
		var alg = new StlDataStructure();
		alg.addFacet(
				new Point3D_F64(0, 0, 0),
				new Point3D_F64(1, 0, 0),
				new Point3D_F64(0, 1, 0),
				new Vector3D_F64(0, 0, 1));

		assertEquals(1, alg.facetCount());

		var foundNormal = new Vector3D_F64();
		var vertexes = new DogArray<>(Point3D_F64::new, Point3D_F64::zero);

		alg.getFacet(0, foundNormal, vertexes);

		assertEquals(0.0, foundNormal.distance(0, 0, 1), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(0).distance(0, 0, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(1).distance(1, 0, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(2).distance(0, 1, 0), UtilEjml.TEST_F64);
	}

	/**
	 * There are 3 points but 2 facets. See if it correctly maps a facet's vertex to a point.
	 */
	@Test void getFacet_2to1_Mapping() {
		var alg = new StlDataStructure();

		alg.addFacet(
				new Point3D_F64(0, 0, 0),
				new Point3D_F64(1, 0, 0),
				new Point3D_F64(0, 1, 0),
				new Vector3D_F64(0, 0, 1));
		alg.addFacet(2, 1, 0, new Vector3D_F64(0, 0, -1));

		assertEquals(2, alg.facetCount());
		assertEquals(3, alg.vertexes.size());

		var foundNormal = new Vector3D_F64();
		var vertexes = new DogArray<>(Point3D_F64::new, Point3D_F64::zero);

		alg.getFacet(0, foundNormal, vertexes);
		assertEquals(0.0, foundNormal.distance(0, 0, 1), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(0).distance(0, 0, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(1).distance(1, 0, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(2).distance(0, 1, 0), UtilEjml.TEST_F64);

		alg.getFacet(1, foundNormal, vertexes);
		assertEquals(0.0, foundNormal.distance(0, 0, -1), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(0).distance(0, 1, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(1).distance(1, 0, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, vertexes.get(2).distance(0, 0, 0), UtilEjml.TEST_F64);
	}

	@Test void addFacet_3Points() {
		var alg = new StlDataStructure();
		alg.addFacet(
				new Point3D_F64(0, 0, 0),
				new Point3D_F64(1, 0, 0),
				new Point3D_F64(0, 1, 0));

		assertEquals(0.0, alg.normals.getTemp(0).distance(0, 0, 1), UtilEjml.TEST_F64);

		alg.addFacet(
				new Point3D_F64(0, 1, 0),
				new Point3D_F64(1, 0, 0),
				new Point3D_F64(0, 0, 0));

		assertEquals(0.0, alg.normals.getTemp(1).distance(0, 0, -1), UtilEjml.TEST_F64);
	}

	/**
	 * Convert it into the VertexMesh structure. It should be two triangles that share some common points.
	 */
	@Test void toMesh() {
		var alg = new StlDataStructure();

		alg.addFacet(
				new Point3D_F64(0, 0, 0),
				new Point3D_F64(1, 0, 0),
				new Point3D_F64(0, 1, 0),
				new Vector3D_F64(0, 0, 1));
		alg.addFacet(2, 1, 0, new Vector3D_F64(0, 0, -1));

		VertexMesh mesh = alg.toMesh(null);

		assertEquals(2, mesh.size());
		assertEquals(0.0, mesh.getShapeVertex(0, null).distance(0, 0, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, mesh.getShapeVertex(1, null).distance(1, 0, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, mesh.getShapeVertex(2, null).distance(0, 1, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, mesh.getShapeVertex(3, null).distance(0, 1, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, mesh.getShapeVertex(4, null).distance(1, 0, 0), UtilEjml.TEST_F64);
		assertEquals(0.0, mesh.getShapeVertex(5, null).distance(0, 0, 0), UtilEjml.TEST_F64);

		assertEquals(3, mesh.vertexes.size());
		assertEquals(3, mesh.faceOffsets.get(1));
		assertEquals(6, mesh.faceOffsets.get(2));
	}
}
