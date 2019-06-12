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

package boofcv.alg.sfm.structure2;

import boofcv.abst.geo.bundle.SceneStructureProjective;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.util.PrimitiveArrays;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
@Disabled
class TestProjectiveInitializeAllCommon {
	@Test
	void process_2() {
		fail("implement");
	}

	@Test
	void process_3() {
		fail("implement");
	}

	@Test
	void process_3_to_5() {
		fail("implement");
	}

	@Test
	void selectInitialTriplet() {
		fail("implement");
	}

	@Test
	void scoreTripleView() {
		fail("implement");
	}

	@Test
	void findTripleMatches() {
		fail("implement");
	}

	@Test
	void createFeatureLookup() {
		fail("implement");
	}

	@Test
	void triangulateFeatures() {
		fail("implement");
	}

	@Test
	void convertAssociatedTriple() {
		fail("implement");
	}

	@Test
	void initializeProjective3() {
		fail("implement");
	}

	@Test
	void findRemainingCameraMatrices() {
		MockLookupSimilarImages db = new MockLookupSimilarImages(5,0xDEADBEEF);

		int numViews = db.viewIds.size();
		PairwiseImageGraph2 graph = db.graph;

		// which edges in the first view should be considered
		GrowQueue_I32 motions = new GrowQueue_I32();
		motions.add(0);
		motions.add(2);
		motions.add(3);


		ProjectiveInitializeAllCommon alg = new ProjectiveInitializeAllCommon();

		//---------------- Initialize a bunch of stuff that is handled by other functions
		// The structure has already been initialized
		alg.structure.initialize(numViews,db.feats3D.size());
		for (int i = 0; i < db.feats3D.size(); i++) {
			Point3D_F64 p = db.feats3D.get(i);
			alg.structure.points.data[i].set(p.x,p.y,p.z,1);
		}
		alg.seedToStructure.resize(db.featToView.size());
		alg.seedToStructure.setTo(db.viewToFeat.get(0),0,db.numFeaturse);
		PrimitiveArrays.fillCounting(alg.seedToStructure.data,0,alg.seedToStructure.size);
		alg.inlierToSeed.resize(db.numFeaturse);
		PrimitiveArrays.fillCounting(alg.inlierToSeed.data,0,alg.inlierToSeed.size);
		// This data structure is used to avoid another lookup from the db
		for (int i = 0; i < db.feats3D.size(); i++) {
			alg.matchesTriple.grow().p1.set(db.viewObs.get(0).get(i));
		}
		// Skip over edge 1 in the motions list
		alg.selectedTriple[0] = alg.selectedTriple[1] = 1;

		// Run the function being tested
		View view0 = graph.nodes.get(0);
		assertTrue(alg.findRemainingCameraMatrices(db,view0,motions));
		assertEquals(4,view0.connections.size); // sanity check

		// See if the camera matrices are correct
		for (int edgeIdx = 0; edgeIdx < view0.connections.size; edgeIdx++) {
			if( motions.contains(edgeIdx) )
				continue;

			View dst = view0.connections.get(edgeIdx).other(view0);
			int index = graph.nodes.indexOf(dst);

			// check camera matrix by projecting points
			checkCameraMatrix(alg.structure,index,db.featToView.get(index),db.viewObs.get(index));
		}
	}

	/**
	 * Checks camera matrix by projection
	 *
	 * @param structure computed structure
	 * @param viewIdx which view is being tested
	 * @param worldToView look up table to go from feature indexes in world to observed feature indexes in a view
	 * @param pixels observed features in a view
	 */
	private void checkCameraMatrix(SceneStructureProjective structure , int viewIdx,
								   int[] worldToView, List<Point2D_F64> pixels ) {
		DMatrixRMaj P = structure.views.data[viewIdx].worldToView;

		Point4D_F64 X = new Point4D_F64();
		Point2D_F64 x = new Point2D_F64();
		for (int i = 0; i < structure.points.size; i++) {
			structure.points.data[i].get(X);
			PerspectiveOps.renderPixel(P,X,x);
			Point2D_F64 obs = pixels.get( worldToView[i]);
			assertTrue(x.distance(obs) <= UtilEjml.TEST_F64);
		}
	}
}