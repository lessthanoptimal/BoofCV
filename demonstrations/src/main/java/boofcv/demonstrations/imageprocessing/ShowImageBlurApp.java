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

package boofcv.demonstrations.imageprocessing;

import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.ProgressMonitorThread;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.*;
import pabeles.concurrency.GrowArray;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Shows the result of different blur operations
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ShowImageBlurApp<T extends ImageGray<T>> extends DemonstrationBase {
	int radius = 2;
	int active = 0;

	ImagePanel gui = new ImagePanel();
	BlurControls controls = new BlurControls();

	Planar<T> output;
	T storage;
	BufferedImage renderedImage;
	GrowArray workspaces;

	public ShowImageBlurApp( java.util.List<PathLabel> examples, Class<T> imageType ) {
		super(examples, ImageType.pl(3, imageType));

		output = new Planar<>(imageType, 1, 1, 3);
		storage = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		workspaces = GeneralizedImageOps.createGrowArray(storage.getImageType());

		add(gui, BorderLayout.CENTER);
		add(controls, BorderLayout.WEST);
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, final int width, final int height ) {
		output.reshape(width, height);
		storage.reshape(width, height);

		renderedImage = ConvertBufferedImage.checkDeclare(width, height, renderedImage, BufferedImage.TYPE_INT_RGB);

		SwingUtilities.invokeLater(() -> {
			gui.setPreferredSize(new Dimension(width, height));
			controls.setImageSize(width, height);
		});
	}

	@Override
	public void processImage( int sourceID, long frameID, final BufferedImage buffered, ImageBase input ) {
		Planar<T> image = (Planar<T>)input;

		// median can take a while. Disable the GUI
		if (inputMethod == InputMethod.IMAGE) {
			SwingUtilities.invokeLater(() -> {
				controls.comboAlg.setEnabled(false);
				controls.spinnerRadius.setEnabled(false);
			});
		}

		// Let the user know something is happening
		ProgressMonitorThread monitor = new MyMonitor(this, "Please Wait");
		monitor.start();

		long time0 = System.nanoTime();
		switch (active) {
			case 0:
				GBlurImageOps.gaussian(image, output, -1, radius, storage);
				break;

			case 1:
				GBlurImageOps.mean(image, output, radius, storage, workspaces);
				break;

			case 2:
				GBlurImageOps.median(image, output, radius, radius, workspaces);
				break;
		}
		long time1 = System.nanoTime();

		monitor.stopThread();

		ConvertBufferedImage.convertTo(output, renderedImage, true);
		SwingUtilities.invokeLater(() -> {
			if (inputMethod == InputMethod.IMAGE) {
				controls.comboAlg.setEnabled(true);
				controls.spinnerRadius.setEnabled(true);
			}
			controls.setTime((time1 - time0)*1e-6);
			gui.setImageRepaint(renderedImage);
		});
	}

	private static class MyMonitor extends ProgressMonitorThread {

		protected MyMonitor( Component comp, String message ) {
			super(new ProgressMonitor(comp,
					"Blurring the Image",
					message, 0, 1));
		}

		@Override
		public void doRun() {}
	}

	class BlurControls extends StandardAlgConfigPanel implements ChangeListener, ActionListener {
		JLabel labelTime = new JLabel();
		JLabel labelSize = new JLabel();
		JSpinner spinnerRadius;
		JComboBox<String> comboAlg;

		public BlurControls() {
			spinnerRadius = spinner(radius, 1, 100, 2);
			comboAlg = combo(0, "Gaussian", "Mean", "Median");

			labelTime.setPreferredSize(new Dimension(70, 26));
			labelTime.setHorizontalAlignment(SwingConstants.RIGHT);

			addLabeled(labelTime, "Time (ms)");
			add(labelSize);
			addAlignLeft(comboAlg);
			addLabeled(spinnerRadius, "Radius");
		}

		public void setTime( double milliseconds ) {
			labelTime.setText(String.format("%.1f", milliseconds));
		}

		public void setImageSize( int width, int height ) {
			labelSize.setText(width + " x " + height);
		}

		@Override
		public void stateChanged( ChangeEvent e ) {
			if (spinnerRadius == e.getSource()) {
				radius = ((Number)spinnerRadius.getValue()).intValue();
				reprocessImageOnly();
			}
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			if (comboAlg == e.getSource()) {
				active = comboAlg.getSelectedIndex();
				reprocessImageOnly();
			}
		}
	}

	public static void main( String[] args ) {
		java.util.List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Horses", UtilIO.pathExample("segment/berkeley_horses.jpg")));
		examples.add(new PathLabel("Human Statue", UtilIO.pathExample("standard/kodim17.jpg")));
		examples.add(new PathLabel("sunflowers", UtilIO.pathExample("sunflowers.jpg")));
		examples.add(new PathLabel("Kangaroo", UtilIO.pathExample("segment/berkeley_kangaroo.jpg")));
		examples.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		examples.add(new PathLabel("beach", UtilIO.pathExample("scale/beach02.jpg")));
		examples.add(new PathLabel("Face Paint", UtilIO.pathExample("standard/kodim15.jpg")));

		SwingUtilities.invokeLater(() -> {
			ShowImageBlurApp app = new ShowImageBlurApp(examples, GrayU8.class);

			app.openExample(examples.get(0));
			app.display("Blur Image Ops");
		});
	}
}
