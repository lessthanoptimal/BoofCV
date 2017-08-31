/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.tracking;

import boofcv.alg.tracker.meanshift.PixelLikelihood;
import boofcv.alg.tracker.meanshift.TrackerMeanShiftLikelihood;
import boofcv.gui.image.ShowImages;
import boofcv.gui.tracker.TrackerObjectQuadPanel;
import boofcv.io.MediaManager;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.shapes.RectangleLength2D_I32;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Example of how to use the low level implementation of mean-shift to track a specific color provided by the user.
 * The weights of each pixel is computed by RgbLikelihood, see below, and track regions are rectangular.  This
 * tracker works very well since the ball is almost uniformly blue.  If the light varried then a HSV color
 * model might be better.
 *
 * @author Peter Abeles
 */
public class ExampleTrackerMeanShiftLikelihood {

	/**
	 * Very simple implementation of PixelLikelihood.  Uses linear distance to compute how close
	 * a color is to the target color.
	 */
	public static class RgbLikelihood implements PixelLikelihood<Planar<GrayU8>> {

		int targetRed,targetGreen,targetBlue;
		float radius = 35;
		Planar<GrayU8> image;

		public RgbLikelihood(int targetRed, int targetGreen, int targetBlue) {
			this.targetRed = targetRed;
			this.targetGreen = targetGreen;
			this.targetBlue = targetBlue;
		}

		@Override
		public void setImage(Planar<GrayU8> image) {
			this.image = image;
		}

		@Override
		public boolean isInBounds(int x, int y) {
			return image.isInBounds(x,y);
		}

		/**
		 * This function is used to learn the target's model from the select image region.  Since the
		 * model is provided in the constructor it isn't needed or used.
		 */
		@Override
		public void createModel(RectangleLength2D_I32 target) {
			throw new RuntimeException("Not supported");
		}

		@Override
		public float compute(int x, int y) {
			int pixelR = image.getBand(0).get(x,y);
			int pixelG = image.getBand(1).get(x,y);
			int pixelB = image.getBand(2).get(x,y);

			// distance along each color band
			float red = Math.max(0, 1.0f - Math.abs(targetRed - pixelR) / radius);
			float green = Math.max(0,1.0f - Math.abs(targetGreen-pixelG)/radius);
			float blue = Math.max(0,1.0f - Math.abs(targetBlue-pixelB)/radius);

			// multiply them all together
			return red*green*blue;
		}
	}

	public static void main(String[] args) {
		MediaManager media = DefaultMediaManager.INSTANCE;
		String fileName = UtilIO.pathExample("tracking/balls_blue_red.mjpeg");
		RectangleLength2D_I32 location = new RectangleLength2D_I32(394,247,475-394,325-247);

		ImageType<Planar<GrayU8>> imageType = ImageType.pl(3,GrayU8.class);

		SimpleImageSequence<Planar<GrayU8>> video = media.openVideo(fileName, imageType);

		// Return a higher likelihood for pixels close to this RGB color
		RgbLikelihood likelihood = new RgbLikelihood(64,71,69);

		TrackerMeanShiftLikelihood<Planar<GrayU8>> tracker =
				new TrackerMeanShiftLikelihood<>(likelihood, 50, 0.1f);

		// specify the target's initial location and initialize with the first frame
		Planar<GrayU8> frame = video.next();
		// Note that the tracker will not automatically invoke RgbLikelihood.createModel() in its initialize function
		tracker.initialize(frame,location);

		// For displaying the results
		TrackerObjectQuadPanel gui = new TrackerObjectQuadPanel(null);
		gui.setPreferredSize(new Dimension(frame.getWidth(),frame.getHeight()));
		gui.setImageUI((BufferedImage)video.getGuiImage());
		gui.setTarget(location,true);
		ShowImages.showWindow(gui, "Tracking Results", true);

		// Track the object across each video frame and display the results
		while( video.hasNext() ) {
			frame = video.next();

			boolean visible = tracker.process(frame);

			gui.setImageUI((BufferedImage) video.getGuiImage());
			gui.setTarget(tracker.getLocation(),visible);
			gui.repaint();

			BoofMiscOps.pause(20);
		}
	}
}
