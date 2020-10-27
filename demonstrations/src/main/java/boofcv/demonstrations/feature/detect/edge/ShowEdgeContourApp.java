/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.feature.detect.edge;

import boofcv.abst.filter.binary.BinaryLabelContourFinder;
import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.demonstrations.binary.SelectHistogramThresholdPanelV;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.factory.filter.binary.FactoryBinaryContourFinder;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays contours selected using different algorithms.
 *
 * @author Peter Abeles
 */
public class ShowEdgeContourApp<T extends ImageGray<T>, D extends ImageGray<D>>
		extends DemonstrationBase implements CannyControlBar.Listener, SelectHistogramThresholdPanelV.Listener {
	// shows panel for displaying input image
	ImagePanel imagePanel = new ImagePanel();
	ContourControls controls = new ContourControls();

	T workImage;
	Class<T> imageType;
	Class<D> derivType;

	GrayU8 binary = new GrayU8(1, 1);
	GrayS32 labeled = new GrayS32(1, 1);

	int previousBlur;
	CannyEdge<T, D> canny;
	BinaryLabelContourFinder contour = FactoryBinaryContourFinder.linearChang2004();

	public ShowEdgeContourApp( java.util.List<PathLabel> examples, Class<T> imageType ) {
		super(examples, ImageType.single(imageType));
		this.imageType = imageType;
		this.derivType = GImageDerivativeOps.getDerivativeType(imageType);

		workImage = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		previousBlur = controls.barCanny.getBlurRadius();
		canny = FactoryEdgeDetectors.canny(previousBlur, true, true, imageType, derivType);
		contour.setConnectRule(ConnectRule.EIGHT);

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, imagePanel);

//		panel.setPreferredSize(new Dimension(1000,500));
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, final int width, final int height ) {
		binary.reshape(width, height);
		labeled.reshape(width, height);
		BoofSwingUtil.invokeNowOrLater(() -> {
			controls.setImageSize(width, height);
			imagePanel.setPreferredSize(new Dimension(width, height));
		});
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage bufferedIn, ImageBase input ) {

		workImage.setTo((T)input);

		long time0, time1;
		final BufferedImage temp;
		if (controls.selectedAlg == 0) {
			if (previousBlur != controls.barCanny.getBlurRadius()) {
				previousBlur = controls.barCanny.getBlurRadius();
				canny = FactoryEdgeDetectors.canny(previousBlur, true, true, imageType, derivType);
			}

			time0 = System.nanoTime();
			double thresh = controls.barCanny.getThreshold()/100.0;
			canny.process(workImage, (float)thresh*0.1f, (float)thresh, null);
			time1 = System.nanoTime();

			List<EdgeContour> contours = canny.getContours();

			temp = VisualizeBinaryData.renderContours(contours, null, workImage.width, workImage.height, null);
		} else {
			// create a binary image by thresholding
			time0 = System.nanoTime();
			GThresholdImageOps.threshold(workImage, binary, controls.barBinary.getThreshold(), controls.barBinary.isDown());
			contour.process(binary, labeled);
			time1 = System.nanoTime();

			List<Contour> contours = BinaryImageOps.convertContours(contour);
			temp = VisualizeBinaryData.renderContours(contours, null, 0xFF1010,
					workImage.width, workImage.height, null);
		}

		SwingUtilities.invokeLater(() -> {
			controls.setTime((time1 - time0)*1e-6);
			controls.barBinary.getHistogramPanel().update(workImage);
			controls.barBinary.getHistogramPanel().repaint();
			if (controls.selectedView == 0)
				imagePanel.setImageRepaint(temp);
			else
				imagePanel.setImageRepaint(bufferedIn);
		});
	}

	@Override
	public void changeCanny() {
		reprocessImageOnly();
	}

	@Override
	public void histogramThresholdChange() {
		reprocessImageOnly();
	}

	class ContourControls extends StandardAlgConfigPanel implements ActionListener {
		JLabel labelTime = new JLabel();
		JLabel labelSize = new JLabel();
		JComboBox<String> comboView;
		JComboBox<String> comboAlgs;

		CannyControlBar barCanny;
		SelectHistogramThresholdPanelV barBinary;
		JPanel algPanel = new JPanel();

		int selectedView = 0;
		int selectedAlg = 0;

		public ContourControls() {
			comboView = combo(selectedView, "Contour", "Input");
			comboAlgs = combo(selectedAlg, "Canny", "Binary Contour");

			barCanny = new CannyControlBar(1, 15);
			barCanny.setListener(ShowEdgeContourApp.this);

			barBinary = new SelectHistogramThresholdPanelV(125, true);
			barBinary.setListener(ShowEdgeContourApp.this);

			algPanel.setLayout(new BorderLayout());
			algPanel.add(barCanny, BorderLayout.CENTER);

			addLabeled(labelTime, "Time (ms)");
			add(labelSize);
			addLabeled(comboView, "View");
			addLabeled(comboAlgs, "Algorithm");
			addAlignLeft(algPanel);
		}

		public void setTime( double milliseconds ) {
			labelTime.setText(String.format("%.1f", milliseconds));
		}

		public void setImageSize( int width, int height ) {
			labelSize.setText(width + " x " + height);
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			if (comboView == e.getSource()) {
				selectedView = comboView.getSelectedIndex();
			} else if (comboAlgs == e.getSource()) {
				selectedAlg = comboAlgs.getSelectedIndex();

				if (selectedAlg == 0) {
					algPanel.remove(barBinary);
					algPanel.add(barCanny, BorderLayout.NORTH);
					barCanny.repaint();
				} else {
					algPanel.remove(barCanny);
					algPanel.add(barBinary, BorderLayout.NORTH);
					barBinary.repaint();
				}
			}
			reprocessInput();
		}
	}

	public static void main( String[] args ) {

		List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Objects", UtilIO.pathExample("simple_objects.jpg")));
		examples.add(new PathLabel("Room", UtilIO.pathExample("indoors01.jpg")));
		examples.add(new PathLabel("Indoors", UtilIO.pathExample("lines_indoors.jpg")));
		examples.add(new PathLabel("shapes", UtilIO.pathExample("shapes/shapes01.png")));
		examples.add(new PathLabel("Human Statue", UtilIO.pathExample("standard/kodim17.jpg")));

		SwingUtilities.invokeLater(() -> {
			ShowEdgeContourApp<GrayF32, GrayF32> app = new ShowEdgeContourApp<>(examples, GrayF32.class);

			// Processing time takes a bit so don't open right away
			app.openExample(examples.get(0));
			app.display("Edge Contours");
		});
	}
}
