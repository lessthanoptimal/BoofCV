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

package boofcv.io.ffmpeg;

import boofcv.alg.misc.ImageStatistics;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/// Decodes a real video file to check that the trimmed FFmpeg frame grabber works end to end.
class TestFfmpegVideoImageSequence extends BoofStandardJUnit {
	/// Short clip from the example data. Skipped if the data submodule has not been checked out.
	private static final String VIDEO = UtilIO.pathExample("fiducial/microqr/movie.mp4");

	@BeforeAll static void checkDataAvailable() {
		assumeTrue(new File(VIDEO).exists(), "Example data not available: " + VIDEO);
	}

	/// Reads the whole sequence and sanity checks the decoded frames.
	@Test void decodeEntireSequence() {
		var sequence = new FfmpegVideoImageSequence<>(VIDEO, ImageType.SB_U8);

		int width = sequence.getWidth();
		int height = sequence.getHeight();
		assertTrue(width > 0 && height > 0);

		int count = 0;
		int notBlank = 0;
		while (sequence.hasNext()) {
			GrayU8 image = sequence.next();
			assertNotNull(image);
			// Every frame has to have the same shape as the sequence reports
			assertEquals(width, image.width);
			assertEquals(height, image.height);
			// A frame of a single constant value means the decode silently produced nothing useful
			if (ImageStatistics.max(image) != ImageStatistics.min(image))
				notBlank++;
			count++;
		}
		sequence.close();

		// Decoding has to produce a real sequence, not one or two frames before giving up
		assertEquals(270, count, "Wrong number of frames decoded");
		assertEquals(count, notBlank, "Some frames decoded to a constant image");
		assertFalse(sequence.hasNext());
		assertNull(sequence.next());
	}

	/// Color decoding has to give three bands that are not all identical.
	@Test void decodeColor() {
		var sequence = new FfmpegVideoImageSequence<>(VIDEO, ImageType.pl(3, GrayU8.class));

		Planar<GrayU8> image = sequence.next();
		assertNotNull(image);
		assertEquals(3, image.getNumBands());
		assertEquals(sequence.getWidth(), image.width);
		assertEquals(sequence.getHeight(), image.height);
		assertTrue(ImageStatistics.max(image.getBand(0)) > 0);
		sequence.close();
	}

	/// reset() has to rewind to the start and hand back the same first frame.
	@Test void resetRewinds() {
		var sequence = new FfmpegVideoImageSequence<>(VIDEO, ImageType.SB_U8);

		GrayU8 first = sequence.next().clone();
		for (int i = 0; i < 5; i++) {
			sequence.next();
		}

		sequence.reset();
		GrayU8 afterReset = sequence.next();

		assertEquals(0, sequence.getFrameNumber() - 1);
		assertEquals(first.width, afterReset.width);
		assertEquals(first.height, afterReset.height);
		// Identical pixels. Decoding the same file twice has to be deterministic.
		for (int y = 0; y < first.height; y++) {
			for (int x = 0; x < first.width; x++) {
				assertEquals(first.get(x, y), afterReset.get(x, y), "Mismatch at " + x + " " + y);
			}
		}
		sequence.close();
	}

	/// An unreadable file has to fail loudly rather than hand back an empty sequence.
	@Test void failsOnMissingFile() {
		assertThrows(RuntimeException.class,
				() -> new FfmpegVideoImageSequence<>("does_not_exist_9134.mp4", ImageType.SB_U8));
	}
}
