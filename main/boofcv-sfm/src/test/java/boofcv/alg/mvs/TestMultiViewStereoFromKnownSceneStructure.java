/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.mvs;

import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.geo.bundle.cameras.BundlePinhole;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.mvs.MultiViewStereoFromKnownSceneStructure.ViewInfo;
import boofcv.core.image.GConvertImage;
import boofcv.factory.disparity.ConfigDisparityBM;
import boofcv.factory.disparity.DisparityError;
import boofcv.factory.disparity.FactoryStereoDisparity;
import boofcv.gui.image.ShowImages;
import boofcv.misc.LookUpImages;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import boofcv.visualize.PointCloudViewer;
import boofcv.visualize.TwoAxisRgbPlane;
import boofcv.visualize.VisualizeData;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.SpecialEuclideanOps_F64;
import org.ddogleg.struct.DogArray_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static georegression.struct.se.SpecialEuclideanOps_F64.eulerXyz;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("ConstantConditions")
public class TestMultiViewStereoFromKnownSceneStructure extends BoofStandardJUnit {

	int width = 120, height = 100;

	double planeZ = 3.0;
	double planeWidth = 3.0;

	SceneStructureMetric scene;
	StereoPairGraph pairs;

	boolean visualize = false;

	/**
	 * Two views which are disconnected and it should be impossible to compute a point cloud
	 */
	@Test void two_views_disconnected() {
		createScene(2);
		// By settings the quality to zero this effectively disconnects the pair
		pairs.vertexes.get("id=0").pairs.get(0).quality3D = 0;

		MultiViewStereoFromKnownSceneStructure<GrayF32> alg = createAlg();

		alg.process(scene, pairs);

		assertEquals(0, alg.getCloud().size());
	}

	/**
	 * Cloud from a fully connected scene with variable number of views
	 */
	@Test void single_cluster() {
		MultiViewStereoFromKnownSceneStructure<GrayF32> alg = createAlg();

		// This also checks calling it multiple times
		for (int numViews = 2; numViews <= 4; numViews++) {
			createScene(numViews);
			alg.process(scene, pairs);
			if (visualize) visualizeResults(alg);
			checkCloudPlane(alg.getCloud());
			// sanity checks
			assertTrue(numViews >= alg.getDisparityCloud().viewPointIdx.size);
		}
	}

	/**
	 * There are two clusters in this situation
	 */
	@Test void multiple_clusters() {
		createScene(4);

		// Disconnect the pairs and create two clusters
		prunePairs(pairs.vertexes.get("id=0").pairs, 0, 1);
		prunePairs(pairs.vertexes.get("id=1").pairs, 0, 1);
		prunePairs(pairs.vertexes.get("id=2").pairs, 2, 3);
		prunePairs(pairs.vertexes.get("id=3").pairs, 2, 3);

		MultiViewStereoFromKnownSceneStructure<GrayF32> alg = createAlg();
		alg.process(scene, pairs);

		if (visualize) visualizeResults(alg);

		checkCloudPlane(alg.getCloud());

		// There should be 2 clusters which means there will be 3 elements. [0] = start of view 1, [1] = start view 2,
		// [2] = last point index
		assertEquals(3, alg.getDisparityCloud().viewPointIdx.size);
	}

	void prunePairs( List<StereoPairGraph.Edge> pairs , int keep0 , int keep1 ) {
		for (int i = pairs.size()-1; i >= 0; i--) {
			StereoPairGraph.Edge e = pairs.get(i);
			if (e.va.indexSba == keep0 || e.va.indexSba == keep1) {
				if (e.vb.indexSba == keep0 || e.vb.indexSba == keep1) {
					continue;
				}
			}
			pairs.remove(i);
		}
	}

	/**
	 * Checks to see if a score is computed for each view and is "qualitatively" correct. This does not check
	 * ti see if all the geometry is handled correctly since the rectified and unrectified views are the same.
	 */
	@Test void scoreViewsSelectStereoPairs() {
		createScene(4);

		double threshold = 0.25;

		// make things more interesting by degrading the score for some pairs
		pairs.vertexes.get("id=" + 2).pairs.get(1).quality3D = 0.3;
		pairs.vertexes.get("id=" + 3).pairs.get(2).quality3D = 0.7;
		for (var e : pairs.vertexes.get("id=" + 0).pairs) {
			e.quality3D = threshold - 0.01;
		}

		var dummy = new DummyLookUp();
		var alg = new MultiViewStereoFromKnownSceneStructure<>(dummy, ImageType.SB_U8);
		alg.minimumQuality3D = threshold;

		// Initialize internal data structures
		alg.initializeScores(scene, pairs);
		assertEquals(4, dummy.requestShapes.size());
		assertEquals(0, dummy.requestImage.size());

		// Call function being tested
		alg.scoreViewsSelectStereoPairs(scene);

		// First one should not have a score since every connection was below the threshold
		assertEquals(0.0, alg.arrayScores.find(( a ) -> a.relations.indexSba == 0).score);

		// relative score for the other views is known
		ViewInfo v1 = alg.arrayScores.find(( a ) -> a.relations.indexSba == 1);
		ViewInfo v2 = alg.arrayScores.find(( a ) -> a.relations.indexSba == 2);
		ViewInfo v3 = alg.arrayScores.find(( a ) -> a.relations.indexSba == 3);

		assertTrue(v3.score > v1.score);
		assertTrue(v3.score > v2.score);
		assertTrue(v1.score > v2.score);
	}

	@Test void selectAndLoadConnectedImages() {
		createScene(3);
		var dummy = new DummyLookUp();
		var alg = new MultiViewStereoFromKnownSceneStructure<>(dummy, ImageType.SB_U8);

		// Initialize internal data structures
		alg.initializeScores(scene, pairs);
		assertEquals(3, dummy.requestShapes.size());
		assertEquals(0, dummy.requestImage.size());

		// All views are connected to each other. Every view but the "center" should be added here
		alg.selectAndLoadConnectedImages(pairs, pairs.vertexes.get("id=" + 0));

		assertEquals(2, alg.indexSbaToViewID.size());
		assertEquals(2, alg.imagePairIndexesSba.size);
		assertEquals(3, dummy.requestShapes.size());
		assertEquals(0, dummy.requestImage.size());

		// mark was connection as being below the threshold
		pairs.vertexes.get("id=" + 0).pairs.get(0).quality3D = 0;
		alg.selectAndLoadConnectedImages(pairs, pairs.vertexes.get("id=" + 0));

		assertEquals(1, alg.indexSbaToViewID.size());
		assertEquals(1, alg.imagePairIndexesSba.size);

		// See if the correct two views were added
		assertTrue(alg.imagePairIndexesSba.contains(2));
	}

	/**
	 * Checks to see if it obeys the threshold constraint
	 */
	@Test void pruneViewsThatAreSimilarByNeighbors() {
		createScene(4);

		// All views will always intersect by the same amount
		var alg = new MultiViewStereoFromKnownSceneStructure<>(new DummyLookUp(), ImageType.SB_U8) {
			final double fraction = 0.6;
			@Override protected double computeIntersection( SceneStructureMetric scene, ViewInfo connected ) {
				return fraction;
			}
		};

		// set it to the same value as "fraction" above. This will test to see if it's inclusive
		alg.maximumCenterOverlap = 0.6;

		// Initialize data structures
		pairs.vertexes.values().forEach(v->alg.arrayScores.grow().relations=v);
		alg.arrayScores.forIdx((idx,v)->v.index=idx);
		alg.arrayScores.forEach(v->v.metric=scene.views.get(v.relations.indexSba));
		alg.arrayScores.forEach(v->alg.mapScores.put(v.relations.id,v));
		alg.arrayScores.forEach(v->v.dimension.setTo(width, height));

		// everything should be connected
		alg.pruneViewsThatAreSimilarByNeighbors(scene);
		alg.arrayScores.forEach(v->assertFalse(v.used));

		// only one view should be unused since all views are connected to each other and overlap with each other
		alg.maximumCenterOverlap = 0.5999999;
		alg.pruneViewsThatAreSimilarByNeighbors(scene);
		int totalUnused = 0;
		for( var v : alg.arrayScores.toList() ) {
			if (!v.used)
				totalUnused++;
		}
		assertEquals(1, totalUnused);
	}

	private void createScene( int numViews ) {
		scene = new SceneStructureMetric(true);
		scene.initialize(numViews, numViews, 0);
		pairs = new StereoPairGraph();

		for (int i = 0; i < numViews; i++) {
			// give it a reasonable camera
			double cx = width/2.0;
			double cy = height/2.0;
			scene.setCamera(i, true, new CameraPinhole(cx, cx, 0, cx, cy, width, height));
			scene.setView(i, i, true, SpecialEuclideanOps_F64.eulerXyz((i - 1)*0.3, 0, 0, 0, 0, 0, null));
			pairs.addVertex("id=" + i, i);
		}

		// connect all views to each other with high quality geometric info
		for (int i = 0; i < numViews; i++) {
			for (int j = i + 1; j < numViews; j++) {
				pairs.connect("id=" + i, "id=" + j, 1.0);
			}
		}
	}

	private void visualizeResults( MultiViewStereoFromKnownSceneStructure<GrayF32> alg ) {
		for (int i = 0; i < pairs.vertexes.size(); i++) {
			GrayF32 gray = new GrayF32(1, 1);
			new SimulatedLookUp().loadImage("id=" + i, gray);
			ShowImages.showWindow(gray, "ID=" + i);
		}

		System.out.println("Cloud.size=" + alg.getCloud().size());
		PointCloudViewer pcv = VisualizeData.createPointCloudViewer();
		pcv.setCameraHFov(UtilAngle.radian(90));
		pcv.setTranslationStep(0.2);
		pcv.addCloud(alg.getCloud());
		pcv.setColorizer(new TwoAxisRgbPlane.Z_XY(1.0).fperiod(1.0));
		JComponent component = pcv.getComponent();
		component.setPreferredSize(new Dimension(400, 400));
		ShowImages.showBlocking(component, "Cloud", 60_000);
	}

	/**
	 * Checks to see if the point cloud is as expected. A 2D square planar object at a known distance and size. This
	 * takes in account noise
	 */
	private void checkCloudPlane( List<Point3D_F64> cloud ) {
		assertTrue(cloud.size() > 100);

		DogArray_F64 arrayZ = new DogArray_F64();
		arrayZ.resize(cloud.size());
		arrayZ.reset();
		double error = 0.0;
		double x0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE;
		double y0 = Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
		for (int i = 0; i < cloud.size(); i++) {
			Point3D_F64 p = cloud.get(i);
			double z = p.z;
			if (UtilEjml.isUncountable(z)) // skip points at infinity
				continue;
			arrayZ.add(z);
			error += Math.abs(z - planeZ);

			// Only consider points which are very good fits
			if (Math.abs(z - planeZ) > 0.01)
				continue;
			x0 = Math.min(x0, p.x);
			x1 = Math.max(x1, p.x);
			y0 = Math.min(y0, p.y);
			y1 = Math.max(y1, p.y);
		}
		arrayZ.sort();
		error /= cloud.size();

		// Use the median depth to check distance
		assertEquals(planeZ, arrayZ.getFraction(0.5), 0.005);
		assertEquals(0.0, error, 0.04);

		// The accuracy here is largely dependent on the image resolution, which sucks.
		// 800x600 is within 0.04
		assertEquals(-1.5, x0, 0.3);
		assertEquals(1.5, x1, 0.3);
		assertEquals(-1.5, y0, 0.3);
		assertEquals(1.5, y1, 0.3);
	}

	private MultiViewStereoFromKnownSceneStructure<GrayF32> createAlg() {

		var alg = new MultiViewStereoFromKnownSceneStructure<>(new SimulatedLookUp(), ImageType.SB_F32);

		// It would be very difficult to mock the disparity for each view so we just use real disparity
		var configDisp = new ConfigDisparityBM();
		configDisp.errorType = DisparityError.CENSUS;
		configDisp.texture = 1.0;
		configDisp.validateRtoL = 0;
		configDisp.disparityMin = 0;
		configDisp.disparityRange = 10;
		configDisp.disparityRange = 100;
		configDisp.regionRadiusX = 3;
		configDisp.regionRadiusY = 3;
		configDisp.border = BorderType.EXTENDED;
		alg.setStereoDisparity(FactoryStereoDisparity.blockMatch(configDisp, GrayF32.class, GrayF32.class));

		return alg;
	}

	private class DummyLookUp implements LookUpImages {
		private final List<String> requestShapes = new ArrayList<>();
		private final List<String> requestImage = new ArrayList<>();

		@Override public boolean loadShape( String name, ImageDimension shape ) {
			requestShapes.add(name);
			shape.setTo(width, height);
			return true;
		}

		@Override public <LT extends ImageBase<LT>> boolean loadImage( String name, LT output ) {
			requestImage.add(name);
			output.reshape(width, height);
			return true;
		}
	}

	/**
	 * Renders images as requested with a simulated target
	 */
	private class SimulatedLookUp implements LookUpImages {
		SimulatePlanarWorld sim = new SimulatePlanarWorld();

		public SimulatedLookUp() {
			// Textured target that stereo will work well on
			var texture = new GrayF32(50, 50);
			ImageMiscOps.fillUniform(texture, rand, 50, 255);

			sim.addSurface(eulerXyz(0, 0, planeZ, 0, Math.PI, 0, null), planeWidth, texture);
		}

		@Override public boolean loadShape( String name, ImageDimension shape ) {
			shape.setTo(width, height);
			return true;
		}

		@Override public <LT extends ImageBase<LT>> boolean loadImage( String name, LT output ) {
			int indexSba = Integer.parseInt(name.substring(3));
			var pinhole = new CameraPinhole();
			BundleAdjustmentOps.convert((BundlePinhole)scene.cameras.get(indexSba).model, 0, 0, pinhole);
			pinhole.width = width;
			pinhole.height = height;

			sim.setCamera(pinhole);
			sim.setWorldToCamera(scene.motions.get(indexSba).motion);
			GConvertImage.convert(sim.render(), output);
			return true;
		}
	}
}
