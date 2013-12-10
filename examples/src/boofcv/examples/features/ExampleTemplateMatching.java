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

package boofcv.examples.features;

import boofcv.alg.feature.detect.template.TemplateMatching;
import boofcv.alg.feature.detect.template.TemplateMatchingIntensity;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.template.FactoryTemplateMatching;
import boofcv.factory.feature.detect.template.TemplateScoreType;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.Match;
import boofcv.struct.image.ImageFloat32;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Example of how to find objects inside an image using template matching.  Template matching works
 * well when there is little noise in the image and the object's appearance is known and static.
 *
 * @author Peter Abeles
 */
public class ExampleTemplateMatching {

	/**
	 * Demonstrates how to search for matches of a template inside an image
	 *
	 * @param image           Image being searched
	 * @param template        Template being looked for
	 * @param expectedMatches Number of expected matches it hopes to find
	 * @return List of match location and scores
	 */
	private static List<Match> findMatches(ImageFloat32 image, ImageFloat32 template,
										   int expectedMatches) {
		// create template matcher.
		TemplateMatching<ImageFloat32> matcher =
				FactoryTemplateMatching.createMatcher(TemplateScoreType.SUM_DIFF_SQ, ImageFloat32.class);

		// Find the points which match the template the best
		matcher.setTemplate(template, expectedMatches);
		matcher.process(image);

		return matcher.getResults().toList();

	}

	/**
	 * Computes the template match intensity image and displays the results. Brighter intensity indicates
	 * a better match to the template.
	 */
	public static void showMatchIntensity(ImageFloat32 image, ImageFloat32 template) {

		// create algorithm for computing intensity image
		TemplateMatchingIntensity<ImageFloat32> matchIntensity =
				FactoryTemplateMatching.createIntensity(TemplateScoreType.SUM_DIFF_SQ, ImageFloat32.class);

		// apply the template to the image
		matchIntensity.process(image, template);

		// get the results
		ImageFloat32 intensity = matchIntensity.getIntensity();

		// adjust the intensity image so that white indicates a good match and black a poor match
		// the scale is kept linear to highlight how ambiguous the solution is
		float min = ImageStatistics.min(intensity);
		float max = ImageStatistics.max(intensity);
		float range = max - min;
		PixelMath.plus(intensity, -min, intensity);
		PixelMath.divide(intensity, range, intensity);
		PixelMath.multiply(intensity, 255.0f, intensity);

		BufferedImage output = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_BGR);
		VisualizeImageData.grayMagnitude(intensity, output, -1);
		ShowImages.showWindow(output, "Match Intensity");
	}

	public static void main(String args[]) {

		// Load image and templates
		String directory = "../data/applet/template/";

		ImageFloat32 image = UtilImageIO.loadImage(directory + "desktop.png", ImageFloat32.class);
		ImageFloat32 templateX = UtilImageIO.loadImage(directory + "x.png", ImageFloat32.class);
		ImageFloat32 templatePaint = UtilImageIO.loadImage(directory + "paint.png", ImageFloat32.class);
		ImageFloat32 templateBrowser = UtilImageIO.loadImage(directory + "browser.png", ImageFloat32.class);

		// create output image to show results
		BufferedImage output = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_BGR);
		ConvertBufferedImage.convertTo(image, output);
		Graphics2D g2 = output.createGraphics();

		// Searches for a small 'x' that indicates where a window can be closed
		// Only two such objects are in the image so at best there will be one false positive
		g2.setColor(Color.RED);
		drawRectangles(g2, image, templateX, 3);
		// show match intensity image for this template
		showMatchIntensity(image, templateX);

		// Now it searches for a specific icon for which there is only one match
		g2.setColor(Color.BLUE);
		drawRectangles(g2, image, templatePaint, 1);

		// Look for the Google Chrome browser icon. There is no match for this icon..
		g2.setColor(Color.ORANGE);
		drawRectangles(g2, image, templateBrowser, 1);

		// False positives can some times be pruned using the error score.  In photographs taken
		// in the real world template matching tends to perform very poorly

		ShowImages.showWindow(output, "Found Matches");
	}

	/**
	 * Helper function will is finds matches and displays the results as colored rectangles
	 */
	private static void drawRectangles(Graphics2D g2,
									   ImageFloat32 image, ImageFloat32 template,
									   int expectedMatches) {
		List<Match> found = findMatches(image, template, expectedMatches);

		int r = 2;
		int w = template.width + 2 * r;
		int h = template.height + 2 * r;

		g2.setStroke(new BasicStroke(3));
		for (Match m : found) {
			// the return point is the template's top left corner
			int x0 = m.x - r;
			int y0 = m.y - r;
			int x1 = x0 + w;
			int y1 = y0 + h;

			g2.drawLine(x0, y0, x1, y0);
			g2.drawLine(x1, y0, x1, y1);
			g2.drawLine(x1, y1, x0, y1);
			g2.drawLine(x0, y1, x0, y0);
		}
	}
}
