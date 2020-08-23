/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure2;

import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import boofcv.testing.BoofTesting;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.RandomMatrices_DDRM;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestProjectiveExpandByOneView {
	private final Random rand = BoofTesting.createRandom(0);
	private final static double MATRIX_TOL = 1e-4;

	@Test
	void perfect() {
		var db = new MockLookupSimilarImagesRealistic().pathLine(5,0.3,1.5,2);
		var alg = new ProjectiveExpandByOneView();
		PairwiseImageGraph2 graph = db.createPairwise();

		// Undo apply and undo the shift in pixel coordinates
		DMatrixRMaj M = CommonOps_DDRM.identity(3);
		M.set(0,2,-db.intrinsic.width/2);
		M.set(1,2,-db.intrinsic.height/2);
		DMatrixRMaj M_inv = CommonOps_DDRM.identity(3);
		M_inv.set(0,2,db.intrinsic.width/2);
		M_inv.set(1,2,db.intrinsic.height/2);

		var tmp = new DMatrixRMaj(3,4);
		var found = new DMatrixRMaj(3,4);
		for (int targetIdx : new int[]{3,4} ) {

			// Catch more issues by including/excluding the first node
			// First node has zeros in it's columns (edge case that once caused issues) and mixes up the indexes
			for (int startNode = 0; startNode < 2; startNode++) {
				var working = new SceneWorkingGraph();
				for (int i = startNode; i < 3; i++) {
					CommonOps_DDRM.mult(M,db.views.get(i).camera,working.addView(graph.nodes.get(i)).projective);
//					working.addView(graph.nodes.get(i)).projective.set(db.views.get(i).camera);
				}

				View target = graph.nodes.get(targetIdx);
				assertTrue(alg.process(db,working,target,tmp));
				CommonOps_DDRM.mult(M_inv,tmp,found);

				// they should now be the same up to a scale factor
				DMatrixRMaj expected = db.views.get(targetIdx).camera;
				double scale = MultiViewOps.findScale(found,expected);
				CommonOps_DDRM.scale(scale,found);
				assertTrue(MatrixFeatures_DDRM.isEquals(expected,found, MATRIX_TOL));
			}
		}
	}

	@Test
	void computeConversionHomography() {
		var working = new SceneWorkingGraph();
		var graph = new PairwiseImageGraph2();
		var alg = new ProjectiveExpandByOneView();
		alg.workGraph = working;

		// Known transform between the two views
		DMatrixRMaj H = RandomMatrices_DDRM.rectangle(4,4,rand);
		RandomMatrices_DDRM.fillUniform(alg.utils.P2,rand);
		RandomMatrices_DDRM.fillUniform(alg.utils.P3,rand);

		alg.utils.viewB = graph.createNode("B");
		alg.utils.viewC = graph.createNode("C");
		SceneWorkingGraph.View workB = working.addView(alg.utils.viewB);
		SceneWorkingGraph.View workC = working.addView(alg.utils.viewC);

		CommonOps_DDRM.mult(alg.utils.P2,H,workB.projective);
		CommonOps_DDRM.mult(alg.utils.P3,H,workC.projective);

		assertTrue(alg.computeConversionHomography());
		// check results
		assertTrue(MatrixFeatures_DDRM.isEquals(H,alg.localToGlobal, UtilEjml.TEST_F64));
	}
}