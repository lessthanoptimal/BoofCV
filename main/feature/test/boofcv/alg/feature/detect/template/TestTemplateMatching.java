/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
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

	GrayF32 input = new GrayF32(width, height);
	GrayF32 template = new GrayF32(5, 6);

	/**
	 * Basic detection task with an extraction algorithm that has no border
	 */
	@Test
	public void basicTest_NOBORDER() {
		expected = new ArrayList<>();
		expected.add(new Match(10, 11, 15));
		expected.add(new Match(17, 15, 18));
		expected.add(new Match(0, 0, 18)); // shouldn't detect this guy since its inside the border

		DummyIntensity intensity = new DummyIntensity(false, 4, 5);

		TemplateMatching alg = new TemplateMatching(intensity);

		alg.setImage(input);
		alg.setTemplate(template,null, 10);
		alg.process();

		expected.remove(2);
		checkResults(alg.getResults().toList(), expected, 4, 5);
	}

	/**
	 * Basic detection task with an extraction algorithm that has a border
	 */
	@Test
	public void basicTest_BORDER() {
		expected = new ArrayList<>();
		expected.add(new Match(10, 11, 15));
		expected.add(new Match(16, 15, 18));
		expected.add(new Match(0, 0, 18));

		DummyIntensity intensity = new DummyIntensity(true, 4, 5);

		TemplateMatching alg = new TemplateMatching(intensity);

		alg.setImage(input);
		alg.setTemplate(template,null, 10);
		alg.process();

		checkResults(alg.getResults().toList(), expected, 4, 5);
	}

	/**
	 * Makes sure maximum number of matches is handled correctly
	 */
	@Test
	public void maxMatches() {
		expected = new ArrayList<>();
		expected.add(new Match(10, 11, 15));
		expected.add(new Match(16, 15, 18));
		expected.add(new Match(0, 0, 19));
		expected.add(new Match(22, 30, 15));

		DummyIntensity intensity = new DummyIntensity(true, 4, 5);

		TemplateMatching alg = new TemplateMatching(intensity);

		alg.setImage(input);
		alg.setTemplate(template,null, 2);
		alg.process();

		// remove the lowest scores
		expected.remove(3);
		expected.remove(0);

		checkResults(alg.getResults().toList(), expected, 4, 5);
	}

	@Test
	public void withMask() {
		expected = new ArrayList<>();
		DummyIntensity intensity = new DummyIntensity(false, 4, 5);

		TemplateMatching alg = new TemplateMatching(intensity);

		alg.setImage(input);
		alg.setTemplate(template, new GrayF32(5,5), 10);
		alg.process();

		assertTrue(intensity.maskedCalled);
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

		GrayF32 intensity = new GrayF32(width, height);
		boolean border;
		int offsetX, offsetY;
		boolean maskedCalled = false;

		private DummyIntensity(boolean border, int offsetX, int offsetY) {
			this.border = border;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}

		@Override
		public void setInputImage(ImageBase image) {
			assertTrue(image!=null);
		}

		@Override
		public void process(ImageBase template) {
			assertTrue(template!=null);
			GImageMiscOps.fill(intensity, 0);

			for (Match m : expected) {
				intensity.set(m.x, m.y, (float) m.score);
			}
		}

		@Override
		public void process(ImageBase template, ImageBase mask) {
			assertTrue(mask!=null);
			maskedCalled = true;
			process(template);
		}

		@Override
		public GrayF32 getIntensity() {
			return intensity;
		}

		@Override
		public boolean isBorderProcessed() {
			return border;
		}

		@Override
		public int getBorderX0() {
			return offsetX;
		}

		@Override
		public int getBorderX1() {
			return offsetX;
		}

		@Override
		public int getBorderY0() {
			return offsetY;
		}

		@Override
		public int getBorderY1() {
			return offsetY;
		}
	}
}
