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

import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.struct.QueueCorner;
import boofcv.struct.feature.Match;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.sorting.QuickSelectArray;
import org.ddogleg.struct.FastQueue;

/**
 * Runs a template matching algorithm across the image. Local peaks are found in the resulting
 * intensity image and the best solutions returned.
 *
 * @author Peter Abeles
 */
public class TemplateMatching<T extends ImageBase> {

	// computes an intensity image identifying matches
	private TemplateMatchingIntensity<T> match;
	// searches for local matches in intensity image
	private NonMaxSuppression extractor;

	// Reference to the template being searched for
	private T template;
	// Maximum number of matches that can be returned
	private int maxMatches;

	// storage for found points
	private QueueCorner candidates = new QueueCorner(10);
	// working space for sorting the results
	private float scores[] = new float[10];
	private int indexes[] = new int[10];

	// storage for final points
	private FastQueue<Match> results = new FastQueue<Match>(10, Match.class, true);

	/**
	 * Specifies internal algorithm
	 *
	 * @param match Provide template matching intensity algorithm
	 */
	public TemplateMatching(TemplateMatchingIntensity<T> match) {
		this.match = match;

		extractor = FactoryFeatureExtractor.nonmax(new ConfigExtract(2, -Float.MAX_VALUE, 0, true));
	}

	/**
	 * Adjust how close to objects can be found to each other
	 *
	 * @param radius Distance in pixels.  Try using the template's radius or 2
	 */
	public void setMinimumSeparation(int radius) {
		extractor.setSearchRadius(radius);
	}

	/**
	 * Specifies the template to search for and the maximum number of matches to return.
	 *
	 * @param template   Template being searched for
	 * @param maxMatches The maximum number of matches it will return
	 */
	public void setTemplate(T template, int maxMatches) {
		this.template = template;
		this.maxMatches = maxMatches;

		int w = Math.min(template.width, template.height);
	}

	/**
	 * Performs template matching.
	 *
	 * @param image Image being processed
	 */
	public void process(T image) {

		// compute match intensities
		match.process(image, template);

		ImageFloat32 intensity = match.getIntensity();
		int offsetX = 0;
		int offsetY = 0;

		// adjust intensity image size depending on if there is a border or not
		if (!match.isBorderProcessed()) {
			int x0 = match.getOffsetX();
			int x1 = image.width - (template.width - offsetX);
			int y0 = match.getOffsetY();
			int y1 = image.height - (template.height - offsetY);
			intensity = intensity.subimage(x0, y0, x1, y1, null);
		} else {
			offsetX = match.getOffsetX();
			offsetY = match.getOffsetY();
		}

		// find local peaks in intensity image
		candidates.reset();
		extractor.process(intensity, null,null,null, candidates);

		// select the best matches
		if (scores.length < candidates.size) {
			scores = new float[candidates.size];
			indexes = new int[candidates.size];
		}

		for (int i = 0; i < candidates.size; i++) {
			Point2D_I16 p = candidates.get(i);

			scores[i] = -intensity.get(p.x, p.y);
		}

		int N = Math.min(maxMatches, candidates.size);

		QuickSelectArray.selectIndex(scores, N, candidates.size, indexes);

		// save the results
		results.reset();
		for (int i = 0; i < N; i++) {
			Point2D_I16 p = candidates.get(indexes[i]);

			Match m = results.grow();
			m.score = -scores[i];
			m.set(p.x - offsetX, p.y - offsetY);
		}
	}

	/**
	 * Returns all the found matches.  The location is the location of the top left corner
	 * of the template.  Score is the first score with higher number being better
	 *
	 * @return List of found templates
	 */
	public FastQueue<Match> getResults() {
		return results;
	}
}
