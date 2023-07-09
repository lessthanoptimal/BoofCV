/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.segmentation;

import boofcv.alg.color.ColorHsv;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.controls.BaseImageControlPanel;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.*;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Demonstrates how to segment an image based on a user selected color.
 *
 * @author Maxim Dossioukov
 */

public class SegmentColorDemoApp extends DemonstrationBase {
	// private variables
	private int mouseX, mouseY = 0;
	private float threshold = 0.4f;
	private final float minThreshold = 0.0f;
	private final float maxThreshold = 1.5f;
	private boolean clickStatus, colorDataObtained = false;
	private final float[] color = new float[3];

	ImageZoomPanel imageZoomPanel = new ImageZoomPanel();
	ControlPanel controls = new ControlPanel();
	BufferedImage output = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

	// click toggling for image processing
	public void toggleClick() {
		clickStatus = !clickStatus;
		if (!clickStatus) {
			colorDataObtained = false;
		}
	}

	// color processing for the clicked pixel
	public void processColor( int rgb ) {
		ColorHsv.rgbToHsv((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, color);
	}

	// default constructor for DemonstrationBase
	public SegmentColorDemoApp( List<?> exampleInputs ) {
		super(exampleInputs, ImageType.pl(3, GrayU8.class));

		imageZoomPanel.setPreferredSize(new Dimension(800, 400));
		imageZoomPanel.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked( MouseEvent e ) {
				mouseX = e.getX();
				mouseY = e.getY();
				toggleClick();
				reprocessImageOnly();
			}
		});

		imageZoomPanel.addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved( MouseWheelEvent e ) {
				controls.setZoom(BoofSwingUtil.mouseWheelImageZoom(SegmentColorDemoApp.this.controls.zoom, e));
			}
		});

		// configure the layout, Controls on the left, imageZoom on the main center panel
		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, imageZoomPanel);
	}

	@Override
	protected void configureVideo( int which, SimpleImageSequence sequence ) {
		super.configureVideo(which, sequence);
		sequence.setLoop(true);
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		super.handleInputChange(source, method, width, height);
		
		clickStatus = false;
		output = ConvertBufferedImage.checkDeclare(width, height, output, output.getType());
		// change the size of the GUI to match the input image as well as handle zooming scale
		BoofSwingUtil.invokeNowOrLater(() -> {
			double zoom = BoofSwingUtil.selectZoomToShowAll(imageZoomPanel, width, height);
			controls.setImageSize(width, height);
			controls.setZoom(zoom);
			imageZoomPanel.setScale(zoom);
			imageZoomPanel.updateSize(width, height);
			imageZoomPanel.getVerticalScrollBar().setValue(0);
			imageZoomPanel.getHorizontalScrollBar().setValue(0);
		});
	}

	// retrieve and process the user selected coordinates
	public void clickProcessing( final BufferedImage image, int mouseX, int mouseY ) {

		// accurate conversion of coordinates to image coordinates
		if (colorDataObtained) {
			return;
		}

		Point2D_F64 imageCoordinates = imageZoomPanel.pixelToPoint(mouseX, mouseY);
		double imageX = imageCoordinates.x;
		double imageY = imageCoordinates.y;

		// See if it's inside the image, if not do nothing
		if (!BoofMiscOps.isInside(image.getWidth(), image.getHeight(), imageX, imageY))
			return;

		// grab the rgb value at the image coordinates
		int rgb = image.getRGB((int)imageX, (int)imageY);

		//processes and populate color data array
		processColor(rgb);
		colorDataObtained = true;
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase _input ) {
		if (clickStatus) {
			clickProcessing(buffered, mouseX, mouseY);
		}

		float hue = color[0];
		float saturation = color[1];

		Planar<GrayF32> input = ConvertBufferedImage.convertFromPlanar(buffered, null, true, GrayF32.class);
		Planar<GrayF32> hsv = input.createSameShape();

		// Convert into HSV
		ColorHsv.rgbToHsv(input, hsv);

		// Euclidean distance squared threshold for deciding which pixels are members of the selected set
		float maxDist2 = threshold*threshold;

		// Extract hue and saturation bands which are independent of intensity
		GrayF32 H = hsv.getBand(0);
		GrayF32 S = hsv.getBand(1);

		// Adjust the relative importance of Hue and Saturation.
		// Hue has a range of 0 to 2*PI and Saturation from 0 to 1.
		float adjustUnits = (float)(Math.PI/2.0);

		// step through each pixel and mark how close it is to the selected color
		var output = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);

		// start timer
		long before = System.nanoTime();
		for (int y = 0; y < hsv.height; y++) {
			for (int x = 0; x < hsv.width; x++) {
				// Hue is an angle in radians, so simple subtraction doesn't work
				float dh = UtilAngle.dist(H.unsafe_get(x, y), hue);
				float ds = (S.unsafe_get(x, y) - saturation)*adjustUnits;

				// this distance measure is a bit naive, but good enough for to demonstrate the concept
				float dist2 = dh*dh + ds*ds;
				if (dist2 <= maxDist2) {
					output.setRGB(x, y, buffered.getRGB(x, y));
				}
			}
		}
		// end timer
		long after = System.nanoTime();
		// calculate and update the processing time
		controls.setProcessingTimeS((after - before)*1e-9);

		// update the image panel
		SwingUtilities.invokeLater(() -> {
			if (clickStatus) {
				imageZoomPanel.setBufferedImageNoChange(output);
			} else {
				imageZoomPanel.setBufferedImageNoChange(buffered);
			}
			imageZoomPanel.repaint();
		});
	}

	class ControlPanel extends BaseImageControlPanel {
		JSlider thresholdSlider = new JSlider(0, 100);

		public ControlPanel() {

			thresholdSlider.addChangeListener(e -> {
				int sliderValue = thresholdSlider.getValue();
				threshold = minThreshold + (sliderValue/(float)100)*(maxThreshold - minThreshold);
				reprocessImageOnly();
			});

			selectZoom = spinner(1.0, MIN_ZOOM, MAX_ZOOM, 1.0);

			addLabeled(imageSizeLabel, "Image Shape");
			addLabeled(processingTimeLabel, "Time (ms)");
			addLabeled(selectZoom, "Zoom: ");
			addLabeled(thresholdSlider, "Threshold");
			addVerticalGlue(this);
		}

		@Override public void controlChanged( final Object source ) {
			if (source == selectZoom) {
				// tell the image that it's zoom factor has changed
				imageZoomPanel.setScale(controls.zoom);
				imageZoomPanel.repaint();
				return;
			} else if (source == thresholdSlider) {
				// change the threshold and reprocess the image
				threshold = ((Number)thresholdSlider.getValue()).intValue();
			}
			reprocessImageOnly();
		}
	}

	public static void main( String[] args ) {
		var examples = new ArrayList<PathLabel>();
		examples.add(new PathLabel("Sunflowers", UtilIO.pathExample("sunflowers.jpg")));
		examples.add(new PathLabel("Mountain", UtilIO.pathExample("recognition/scene/image08.jpg")));
		examples.add(new PathLabel("Drone", UtilIO.pathExample("recognition/scene/image14.jpg")));
		examples.add(new PathLabel("Car", UtilIO.pathExample("recognition/pixabay/car02.jpg")));
		examples.add(new PathLabel("chipmunk", UtilIO.pathExample("tracking/chipmunk.mjpeg")));
		examples.add(new PathLabel("Dash Cam", UtilIO.pathExample("tracking/dashcam01.mp4")));
		examples.add(new PathLabel("book", UtilIO.pathExample("tracking/track_book.mjpeg")));
		examples.add(new PathLabel("Chessboard Movie", UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));

		SwingUtilities.invokeLater(() -> {
			var app = new SegmentColorDemoApp(examples);

			app.openExample(examples.get(0));
			app.display("Segment Color Demo");
		});
	}
}
