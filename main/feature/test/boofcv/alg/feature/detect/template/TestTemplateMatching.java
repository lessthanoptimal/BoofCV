/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.template;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.feature.Match;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
@SuppressWarnings("unchecked")
public class TestTemplateMatching {
	int width = 30;
	int height = 40;

	List<Match> expected;

	ImageFloat32 input = new ImageFloat32(width, height);
	ImageFloat32 template = new ImageFloat32(5, 6);

	/**
	 * Basic detection task with an extraction algorithm that has no border
	 */
	@Test
	public void basicTest_NOBORDER() {
		expected = new ArrayList<Match>();
		expected.add(new Match(10, 11, 15));
		expected.add(new Match(17, 15, 18));
		expected.add(new Match(0, 0, 18)); // shouldn't detect this guy since its inside the border

		DummyIntensity intensity = new DummyIntensity(false, 4, 5);

		TemplateMatching alg = new TemplateMatching(intensity);

		alg.setTemplate(template, 10);
		alg.process(input);

		expected.remove(2);
		checkResults(alg.getResults().toList(), expected, 4, 5);
	}

	/**
	 * Basic detection task with an extraction algorithm that has a border
	 */
	@Test
	public void basicTest_BORDER() {
		expected = new ArrayList<Match>();
		expected.add(new Match(10, 11, 15));
		expected.add(new Match(16, 15, 18));
		expected.add(new Match(0, 0, 18));

		DummyIntensity intensity = new DummyIntensity(true, 4, 5);

		TemplateMatching alg = new TemplateMatching(intensity);

		alg.setTemplate(template, 10);
		alg.process(input);

		checkResults(alg.getResults().toList(), expected, 4, 5);
	}

	/**
	 * Makes sure maximum number of matches is handled correctly
	 */
	@Test
	public void maxMatches() {
		expected = new ArrayList<Match>();
		expected.add(new Match(10, 11, 15));
		expected.add(new Match(16, 15, 18));
		expected.add(new Match(0, 0, 19));
		expected.add(new Match(22, 30, 15));

		DummyIntensity intensity = new DummyIntensity(true, 4, 5);

		TemplateMatching alg = new TemplateMatching(intensity);

		alg.setTemplate(template, 2);
		alg.process(input);

		// remove the lowest scores
		expected.remove(3);
		expected.remove(0);

		checkResults(alg.getResults().toList(), expected, 4, 5);
	}

	private void checkResults(List<Match> found, List<Match> expected,
							  int offsetX, int offsetY) {
		assertEquals(expected.size(), found.size());

		for (Match f : found) {
			boolean matched = false;
			for (Match e : expected) {
				if (e.x - offsetX == f.x & e.y - offsetY == f.y) {
					assertFalse(matched);
					assertEquals(e.score, f.score, 1e-8);
					matched = true;
				}
			}
			assertTrue(matched);
		}
	}

	private class DummyIntensity implements TemplateMatchingIntensity {

		ImageFloat32 intensity = new ImageFloat32(width, height);
		boolean border;
		int offsetX, offsetY;

		private DummyIntensity(boolean border, int offsetX, int offsetY) {
			this.border = border;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}

		@Override
		public void process(ImageBase image, ImageBase template) {
			GImageMiscOps.fill(intensity, 0);

			for (Match m : expected) {
				intensity.set(m.x, m.y, (float) m.score);
			}
		}

		@Override
		public ImageFloat32 getIntensity() {
			return intensity;
		}

		@Override
		public boolean isBorderProcessed() {
			return border;
		}

		@Override
		public int getOffsetX() {
			return offsetX;
		}

		@Override
		public int getOffsetY() {
			return offsetY;
		}
	}
}
