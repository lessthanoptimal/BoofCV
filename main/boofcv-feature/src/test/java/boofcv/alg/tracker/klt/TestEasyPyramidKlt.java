/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.klt;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.tracker.FactoryTrackerAlg;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestEasyPyramidKlt extends BoofStandardJUnit {
	int width = 50;
	int height = 60;

	int featureRadius = 2;

	GrayF32 image = new GrayF32(width, height);
	ConfigEasyKlt config = new ConfigEasyKlt();
	EasyPyramidKlt<GrayF32, GrayF32> tracker = createDefault();

	int cornerX = 20;
	int cornerY = 22;

	@BeforeEach public void setup() {
		setTargetLocation(cornerX, cornerY);
	}

	private void setTargetLocation( int x, int y ) {
		ImageMiscOps.fillUniform(image, rand, 0, 1);
		ImageMiscOps.fillRectangle(image, 100, x, y, 20, 20);
	}

	private EasyPyramidKlt<GrayF32, GrayF32> createDefault() {
		return FactoryTrackerAlg.kltEasy(config, GrayF32.class, GrayF32.class);
	}

	@Test void allTogether() {
		// Initialize from first track
		tracker.startFrame(image);
		tracker.addTrack(cornerX, cornerY);
		tracker.describe();
		tracker.finishFrame();
		assertEquals(1, tracker.tracks.size);

		// track it after moving the image
		tracker.startFrame(shiftFrame(1, 0));
		tracker.track();
		assertEquals(0, tracker.removeFailed());
		tracker.describe();
		tracker.finishFrame();
		assertEquals(1, tracker.tracks.size);
		tracker.forEachTrack(( idx, meta, feature ) -> {
			assertEquals(0, meta.id);
			assertEquals(cornerX + 1, feature.x, 0.1);
			assertEquals(cornerY, feature.y, 0.1);
		});

		// One more iteration
		tracker.startFrame(shiftFrame(1, 2));
		tracker.track();
		assertEquals(0, tracker.removeFailed());
		tracker.describe();
		tracker.finishFrame();
		assertEquals(1, tracker.tracks.size);
		tracker.forEachTrack(( idx, meta, feature ) -> {
			assertEquals(0, meta.id);
			assertEquals(cornerX + 1, feature.x, 0.1);
			assertEquals(cornerY + 2, feature.y, 0.1);
		});

		// Test reset
		tracker.reset();
		assertEquals(0, tracker.getNextTrackID());
		assertEquals(0, tracker.tracks.size);
		assertEquals(0, tracker.metadata.size);
	}

	// You can't change the image size so this should throw an exception
	@Test void changeImageSizeException() {
		tracker.startFrame(image);
		tracker.finishFrame();
		assertThrows(IllegalArgumentException.class, () -> {
			tracker.startFrame(new GrayF32(2, 2));
		});
	}

	@Test void removeFailed() {
		tracker.startFrame(image);
		assertEquals(0, tracker.removeFailed());

		tracker.addTrack(cornerX, cornerY);
		tracker.addTrack(cornerX, cornerY);
		tracker.addTrack(0, cornerY);
		tracker.addTrack(cornerX, cornerY);
		tracker.addTrack(cornerX, cornerY);
		tracker.metadata.get(0).status = KltTrackFault.FAILED;
		tracker.metadata.get(1).status = KltTrackFault.BACKWARDS;
		tracker.metadata.get(2).status = KltTrackFault.SUCCESS;
		tracker.metadata.get(3).status = KltTrackFault.OUT_OF_BOUNDS;
		tracker.metadata.get(4).status = KltTrackFault.DESCRIBE;

		// All but one should be removed
		assertEquals(4, tracker.removeFailed());

		// Verify it saved the correct one
		assertEquals(0.0, tracker.tracks.get(0).x );
		assertEquals(cornerY, tracker.tracks.get(0).y );
	}

	@Test void clearFailedStatus() {
		tracker.startFrame(image);

		// Shouldn't blow up if empty
		tracker.clearFailedStatus();

		// Add tracks. Everything that's not SUCCESS gets cleared
		for (int i = 0; i < 4; i++) {
			tracker.addTrack(cornerX, cornerY);
			tracker.metadata.getTail().status = KltTrackFault.FAILED;
		}

		tracker.clearFailedStatus();
		tracker.forEachTrack((id, meta, feature) -> {
			assertEquals(KltTrackFault.SUCCESS, meta.status);
		});
	}

	@Test void removeFeatureFast() {
		tracker.startFrame(image);
		for (int i = 0; i < 5; i++) {
			tracker.addTrack(0, 0);
		}
		tracker.removeFeatureFast(2);

		// See if it has been removed, but don't check the order
		assertEquals(4, tracker.tracks.size());

		tracker.forEachTrack(( idx, meta, feature ) -> {
			assertNotEquals(2, meta.id);
		});

	}

	@Test void removeFeatureOrder() {
		tracker.startFrame(image);
		for (int i = 0; i < 5; i++) {
			tracker.addTrack(0, 0);
		}
		tracker.removeFeatureOrder(2);

		// See if it has been removed, but don't check the order
		assertEquals(4, tracker.tracks.size());

		assertEquals(0, tracker.metadata.get(0).id);
		assertEquals(1, tracker.metadata.get(1).id);
		assertEquals(3, tracker.metadata.get(2).id);
		assertEquals(4, tracker.metadata.get(3).id);
	}

	private GrayF32 shiftFrame( int deltaX, int deltaY ) {
		GrayF32 dst = image.createSameShape();
		ImageMiscOps.copy(0, 0, deltaX, deltaY,
				dst.width - deltaX, dst.height - deltaY, image, dst);
		return dst;
	}
}
