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
import boofcv.alg.geo.pose.CompatibleProjectiveHomography;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestProjectiveReconstructionFromPairwiseGraph {

	/**
	 * Random scene with fairly linear motion. Everything is randomized and physical constraints on camera are enforced
	 */
	@Test
	void process_perfect() {
		// NOTE: Accuracy degrades as the number of views increases. The tolerance is also hard coded at 1e-5 and 1e-7 or 1e-8
		//       would be more reasonable upper limit
		//       Additional work should be put into this so that accuracy with perfect data is independent of the number
		//       of views.

		var alg = new ProjectiveReconstructionFromPairwiseGraph();
		for (int numViews = 3; numViews <= 20; numViews++) {
			System.out.println("numViews = "+numViews);
			var db = new MockLookupSimilarImagesRealistic().setLoop(false).setSeed(numViews).setFeatures(450).pathLine(numViews,0.30,6.0,2);
			PairwiseImageGraph2 graph = db.createPairwise();
			assertTrue(alg.process(db,graph));
			checkCameraMatrices(alg,db);
		}
	}

	/**
	 * Compare found camera matrices against truth by converting them into the same projective scale
	 */
	private void checkCameraMatrices(ProjectiveReconstructionFromPairwiseGraph alg, MockLookupSimilarImagesRealistic db) {
		List<SceneWorkingGraph.View> foundViews = alg.workGraph.getAllViews();
		assertEquals(db.views.size(), foundViews.size());

		// Undo apply and undo the shift in pixel coordinates
		DMatrixRMaj M_inv = CommonOps_DDRM.identity(3);
		M_inv.set(0,2,db.intrinsic.width/2);
		M_inv.set(1,2,db.intrinsic.height/2);

		var tmp = new DMatrixRMaj(3,4);

		CompatibleProjectiveHomography compatible = new CompatibleProjectiveHomography();
		List<DMatrixRMaj> listA = new ArrayList<>();
		List<DMatrixRMaj> listB = new ArrayList<>();

		for( MockLookupSimilarImagesRealistic.View mv : db.views ) {
			DMatrixRMaj found = alg.workGraph.lookupView( mv.id ).projective;
//			found.print();
			CommonOps_DDRM.mult(M_inv,found, tmp);
			listA.add( tmp.copy() );
			listB.add( mv.camera );
		}

		DMatrixRMaj H = new DMatrixRMaj(4,4);
		assertTrue(compatible.fitCameras(listA,listB,H));

		DMatrixRMaj found = new DMatrixRMaj(3,4);
		for (int i = 0; i < listA.size(); i++) {
			CommonOps_DDRM.mult(listA.get(i),H,found);
			DMatrixRMaj expected = listB.get(i);
			double scale = MultiViewOps.findScale(found,expected);
			CommonOps_DDRM.scale(scale,found);
			double tol = CommonOps_DDRM.elementMaxAbs(found)*1e-6; // TODO change to 1e-7
			assertTrue(MatrixFeatures_DDRM.isIdentical(expected,found, tol));
		}
	}
}