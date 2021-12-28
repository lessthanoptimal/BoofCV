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

package boofcv.demonstrations.binary;

import boofcv.abst.filter.binary.BinaryLabelContourFinder;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.demonstrations.shapes.ThresholdControlPanel;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryBinaryContourFinder;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Visualizes binary contours.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VisualizeBinaryContourApp<T extends ImageGray<T>> extends DemonstrationBase
		implements ThresholdControlPanel.Listener {
	VisualizePanel guiImage;
	ContourControlPanel controls = new ContourControlPanel(this);

	BinaryLabelContourFinder contourAlg;
	InputToBinary<T> inputToBinary;

	GrayU8 binary = new GrayU8(1, 1);
	GrayS32 labeled = new GrayS32(1, 1);

	BufferedImage original;
	BufferedImage work;

	public VisualizeBinaryContourApp( List<String> exampleInputs, ImageType<T> imageType ) {
		super(exampleInputs, imageType);

		guiImage = new VisualizePanel();
		guiImage.setPreferredSize(new Dimension(800, 800));

		guiImage.getImagePanel().addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved( MouseWheelEvent e ) {
				controls.setZoom(BoofSwingUtil.mouseWheelImageZoom(controls.zoom, e));
			}
		});

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, guiImage);

		ConfigThreshold config = controls.getThreshold().createConfig();
		inputToBinary = FactoryThresholdBinary.threshold(config, imageType.getImageClass());
		contourAlg = FactoryBinaryContourFinder.linearChang2004();
		contourAlg.setConnectRule(controls.getConnectRule());
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, final int width, final int height ) {
		super.handleInputChange(source, method, width, height);

		binary.reshape(width, height);
		labeled.reshape(width, height);

		// reset the scaling and ensure the entire new image is visible
		BoofSwingUtil.invokeNowOrLater(new Runnable() {
			@Override
			public void run() {
				int w = guiImage.getWidth();
				int h = guiImage.getHeight();
				if (w == 0) {
					w = guiImage.getPreferredSize().width;
					h = guiImage.getPreferredSize().height;
				}

				double scale = Math.max(width/(double)w, height/(double)h);
				scale = Math.max(1, scale);
				System.out.println("scale " + scale);
				controls.setZoom(1.0/scale);
			}
		});
	}

	@Override
	public void processImage( int sourceID, long frameID, final BufferedImage buffered, ImageBase input ) {

		synchronized (this) {
			original = ConvertBufferedImage.checkCopy(buffered, original);
			work = ConvertBufferedImage.checkDeclare(buffered, work);
		}

//		SwingUtilities.invokeLater(new Runnable() {
//			@Override
//			public void run() {
//				Dimension d = guiImage.getPreferredSize();
//				if( d.getWidth() < buffered.getWidth() || d.getHeight() < buffered.getHeight() ) {
//					guiImage.setPreferredSize(new Dimension(buffered.getWidth(), buffered.getHeight()));
//				}
//			}});

		synchronized (this) {
			inputToBinary.process((T)input, binary);
			contourAlg.process(binary, labeled);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				viewUpdated();
			}
		});
	}

	@Override
	public void imageThresholdUpdated() {
		synchronized (this) {
			ConfigThreshold config = controls.getThreshold().createConfig();
			inputToBinary = FactoryThresholdBinary.threshold(config, getImageType(0).getImageClass());
		}
		reprocessImageOnly();
	}

	public void contourAlgUpdated() {
		synchronized (this) {
			contourAlg.setConnectRule(controls.getConnectRule());
		}
		reprocessImageOnly();
	}

	/**
	 * Called when how the data is visualized has changed
	 */
	public void viewUpdated() {
		BufferedImage active = null;
		if (controls.selectedView == 0) {
			active = original;
		} else if (controls.selectedView == 1) {
			VisualizeBinaryData.renderBinary(binary, false, work);
			active = work;
			work.setRGB(0, 0, work.getRGB(0, 0));
		} else {
			Graphics2D g2 = work.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, work.getWidth(), work.getHeight());
			active = work;
		}

		guiImage.setScale(controls.zoom);
		guiImage.setBufferedImageNoChange(active);
		guiImage.repaint();
	}

	class VisualizePanel extends ImageZoomPanel {

		@Override
		protected void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
			synchronized (VisualizeBinaryContourApp.this) {

				List<Contour> contours = BinaryImageOps.convertContours(contourAlg);

				VisualizeBinaryData.render(contours, Color.BLUE, Color.RED, 1.0, scale, g2);
			}
		}
	}

	public static void main( String[] args ) {
		List<String> examples = new ArrayList<>();
		examples.add(UtilIO.pathExample("shapes/shapes02.png"));
		examples.add(UtilIO.pathExample("shapes/concave01.jpg"));
		examples.add(UtilIO.pathExample("shapes/polygons01.jpg"));

		SwingUtilities.invokeLater(() -> {
			var app = new VisualizeBinaryContourApp<>(examples, ImageType.single(GrayF32.class));

			app.openFile(new File(examples.get(0)));
			app.display("Binary Contour Visualization");
		});
	}
}
