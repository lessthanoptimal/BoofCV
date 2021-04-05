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

package boofcv.alg.sfm.d3.structure;

import boofcv.alg.distort.pinhole.LensDistortionPinhole;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BFrame;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BObservation;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestVisOdomBundleAdjustment extends BoofStandardJUnit {
	CameraPinholeBrown pinhole = new CameraPinholeBrown(400, 400, 0, 500, 500, 1000, 1000);

	/**
	 * Perfect input and very little should change
	 */
	@Test
	void optimize_perfect() {
		VisOdomBundleAdjustment<BTrack> alg = createAlgSingleCamera();

		createPerfectScene(alg);
		alg.optimize(null);
		assertEquals(0.0, alg.bundle.sba.getFitScore(), 1e-4);
	}

	/**
	 * Add a little bit of noise and see if things blow up
	 */
	@Test
	void optimize_noise() {
		VisOdomBundleAdjustment<BTrack> alg = createAlgSingleCamera();
		createPerfectScene(alg);
		// Perfect observations with less than perfect location estimates
		for (int i = 5; i < alg.tracks.size - 5; i++) {
			alg.tracks.get(i).worldLoc.x += rand.nextGaussian()*0.02;
			alg.tracks.get(i).worldLoc.y += rand.nextGaussian()*0.02;
			alg.tracks.get(i).worldLoc.z += rand.nextGaussian()*0.02;
		}

		// first test a negative. If should fail 1 iteration isn't enough to fully optimize
		alg.bundle.configConverge.setTo(1e-6, 1e-6, 1);
		alg.optimize(null);
		assertNotEquals(0.0, alg.bundle.sba.getFitScore(), 0.001);

		// This should do much better
		alg.bundle.configConverge.setTo(1e-6, 1e-6, 10);
//		alg.bundleAdjustment.setVerbose(System.out,0);
		alg.optimize(null);
		assertEquals(0.0, alg.bundle.sba.getFitScore(), 0.001);
	}

	@Test
	void addObservation() {
		VisOdomBundleAdjustment<BTrack> alg = createAlgSingleCamera();

		BFrame frameA = alg.addFrame(0);
		BTrack trackA = alg.addTrack(1, 2, 3, 3);

		alg.addObservation(frameA, trackA, 5, 10);
		assertEquals(1, frameA.tracks.size);
		assertEquals(1, trackA.observations.size);
		BObservation obs = trackA.observations.get(0);
		assertSame(frameA, obs.frame);
		assertEquals(0.0, obs.pixel.distance(5, 10), UtilEjml.TEST_F64);
	}

	@Test
	void addTrack() {
		VisOdomBundleAdjustment<BTrack> alg = createAlgSingleCamera();
		BTrack trackA = alg.addTrack(1, 2, 3, 3);
		BTrack trackB = alg.addTrack(1, 2, 3, 4);
		BTrack trackC = alg.addTrack(1, 2, 3, 5);

		assertEquals(3, alg.tracks.size);
		assertTrue(alg.tracks.contains(trackA));
		assertTrue(alg.tracks.contains(trackB));
		assertTrue(alg.tracks.contains(trackC));

		assertEquals(0.0, trackB.worldLoc.distance(new Point4D_F64(1, 2, 3, 4)), UtilEjml.TEST_F64);
		assertEquals(0.0, trackC.worldLoc.distance(new Point4D_F64(1, 2, 3, 5)), UtilEjml.TEST_F64);

		trackA.observations.grow();
		trackB.observations.grow();

		// Test recycle
		alg.reset();
		trackA = alg.addTrack(2, 2, 3, 3);
		trackB = alg.addTrack(2, 2, 3, 4);
		trackC = alg.addTrack(2, 2, 3, 5);

		assertTrue(alg.tracks.contains(trackA));
		assertTrue(alg.tracks.contains(trackB));
		assertTrue(alg.tracks.contains(trackC));

		assertEquals(0, trackB.observations.size);
		assertEquals(0.0, trackB.worldLoc.distance(new Point4D_F64(2, 2, 3, 4)), UtilEjml.TEST_F64);
		assertEquals(0, trackC.observations.size);
		assertEquals(0.0, trackC.worldLoc.distance(new Point4D_F64(2, 2, 3, 5)), UtilEjml.TEST_F64);
	}

	@Test
	void addFrame() {
		VisOdomBundleAdjustment<BTrack> alg = createAlgSingleCamera();
		BFrame frameA = alg.addFrame(0);
		frameA.frame_to_world.T.x = 10;

		// Very simple test
		assertEquals(1, alg.frames.size);
		assertEquals(0, alg.frames.get(0).id);

		// See if the frame is recycled correctly
		alg.reset();
		alg.addFrameDebug(2);
		assertEquals(1, alg.frames.size);
		assertSame(frameA, alg.frames.get(0));
		assertEquals(2, frameA.id);
		assertEquals(0, frameA.frame_to_world.T.x, UtilEjml.TEST_F64);

		// Add one more frame now
		BFrame frameB = alg.addFrameDebug(11);
		assertEquals(2, alg.frames.size);
		assertSame(frameB, alg.getLastFrame());
		assertEquals(11, frameB.id);
	}

	private VisOdomBundleAdjustment<BTrack> createAlgSingleCamera() {
		var alg = new VisOdomBundleAdjustment<>(BTrack::new);
		alg.addCamera(new CameraPinholeBrown(0, 0, 0, 0, 0, 100, 200));
		return alg;
	}

	@Test
	void removeFrame() {
		VisOdomBundleAdjustment<BTrack> alg = createAlgSingleCamera();

		BFrame frameA = alg.addFrame(0);
		BFrame frameB = alg.addFrame(1);

		BTrack trackA = alg.addTrack(1, 2, 3, 4);
		BTrack trackB = alg.addTrack(1, 2, 3, 4);
		BTrack trackC = alg.addTrack(1, 2, 3, 4);

		alg.addObservation(frameA, trackA, 1, 2);
		alg.addObservation(frameA, trackC, 1, 3);
		alg.addObservation(frameB, trackB, 1, 4);
		alg.addObservation(frameB, trackC, 1, 5);

		alg.removeFrame(frameA, new ArrayList<>());
		assertEquals(1, alg.frames.size);
		assertEquals(2, alg.tracks.size);
		assertSame(frameB, alg.frames.get(0));
		assertTrue(alg.tracks.contains(trackB));
		assertTrue(alg.tracks.contains(trackC));
	}

	@Test
	void getFirstFrame() {
		VisOdomBundleAdjustment<BTrack> alg = createAlgSingleCamera();

		BFrame frameA = alg.frames.grow();
		assertSame(frameA, alg.getFirstFrame());
		alg.frames.grow();
		assertSame(frameA, alg.getFirstFrame());
	}

	@Test
	void getLastFrame() {
		VisOdomBundleAdjustment<BTrack> alg = createAlgSingleCamera();

		BFrame frameA = alg.frames.grow();
		assertSame(frameA, alg.getLastFrame());
		BFrame frameB = alg.frames.grow();
		assertSame(frameB, alg.getLastFrame());
	}

	@Nested
	class CheckBTrack {
		@Test
		void isObservedBy() {
			BFrame frameA = new BFrame();
			BFrame frameB = new BFrame();
			BFrame frameC = new BFrame();

			BTrack track = new BTrack();

			BObservation o = track.observations.grow();
			o.frame = frameA;
			o = track.observations.grow();
			o.frame = frameB;

			assertTrue(track.isObservedBy(frameA));
			assertTrue(track.isObservedBy(frameB));
			assertFalse(track.isObservedBy(frameC));
		}

		@Test
		void removeRefs() {
			BFrame frameA = new BFrame();
			BFrame frameB = new BFrame();
			BFrame frameC = new BFrame();

			BTrack track = new BTrack();

			BObservation o = track.observations.grow();
			o.frame = frameA;
			o = track.observations.grow();
			o.frame = frameB;

			assertTrue(track.removeRef(frameA));
			assertFalse(track.removeRef(frameA));
			assertFalse(track.removeRef(frameC));
			assertTrue(track.removeRef(frameB));
			assertFalse(track.removeRef(frameB));
			assertEquals(0, track.observations.size);
		}
	}

	private void createPerfectScene( VisOdomBundleAdjustment<BTrack> vsba ) {
		vsba.reset();

		List<Point3D_F64> cloud = UtilPoint3D_F64.random(new Point3D_F64(0, 0, 1.5),
				-1.5, 1.5, -0.5, 0.5, -0.2, 0.2, 500, rand);

		LensDistortionPinhole distortion = new LensDistortionPinhole(pinhole);
		Point2Transform2_F64 n2n = distortion.distort_F64(false, false);

		vsba.addCamera(pinhole);

		Point3D_F64 Xv = new Point3D_F64(); // 3D point in view reference frame
		Point2D_F64 n = new Point2D_F64();  // normalized image coordinate
		Point2D_F64 p = new Point2D_F64();  // pixel coordinate

		for (int i = 0; i < cloud.size(); i++) {
			Point3D_F64 X = cloud.get(i);
			vsba.addTrack(X.x, X.y, X.z, 1.0).hasBeenInlier = true;
		}

		for (int viewidx = 0; viewidx < 5; viewidx++) {
			BFrame frame = vsba.addFrame(viewidx);
			frame.frame_to_world.setTo(-1 + 2.0*viewidx/4.0, 0, 0, EulerType.XYZ, rand.nextGaussian()*0.1, 0, 0);

			// add visible features to each view
			for (int i = 0; i < cloud.size(); i++) {
				Point3D_F64 X = cloud.get(i);
				frame.frame_to_world.transformReverse(X, Xv);
				if (Xv.z <= 0)
					continue;

				n2n.compute(Xv.x/Xv.z, Xv.y/Xv.z, n);
				PerspectiveOps.convertNormToPixel(pinhole, n.x, n.y, p);

				if (!pinhole.isInside(p.x, p.y))
					continue;

				BTrack track = vsba.tracks.get(i);
				vsba.addObservation(frame, track, p.x, p.y);
			}
		}
	}
}
