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

package boofcv.demonstrations.enhance;

import boofcv.alg.enhance.EnhanceImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Displays various image enhancement filters
 *
 * @author Peter Abeles
 */
// TODO Add wavelet denoising
public class ImageEnhanceApp extends DemonstrationBase {

	public static String HISTOGRAM_GLOBAL = "Histogram Global";
	public static String HISTOGRAM_LOCAL = "Histogram Local";
	public static String SHARPEN_4 = "Sharpen-4";
	public static String SHARPEN_8 = "Sharpen-8";

	ImageZoomPanel imagePanel = new ImageZoomPanel();
	ControlPanel controls = new ControlPanel();

	// storage for histogram
	int histogram[] = new int[256];
	int transform[] = new int[256];

	GrayU8 enhanced = new GrayU8(1,1);

	BufferedImage output = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB);

	public ImageEnhanceApp(List<?> exampleInputs ) {
		super(exampleInputs, ImageType.single(GrayU8.class));

		imagePanel.setPreferredSize(new Dimension(800,800));
		imagePanel.addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {

				double curr = ImageEnhanceApp.this.controls.zoom;

				if( e.getWheelRotation() > 0 )
					curr *= 1.1;
				else
					curr /= 1.1;
				controls.setZoom(curr);
			}
		});

		imagePanel.requestFocus();

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, imagePanel);
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, final int width, final int height) {
		super.handleInputChange(source, method, width, height);

		enhanced.reshape(width, height);
		output = ConvertBufferedImage.checkDeclare(width,height,output,output.getType());

		BoofSwingUtil.invokeNowOrLater(new Runnable() {
			@Override
			public void run() {
				double zoom = BoofSwingUtil.selectZoomToShowAll(imagePanel,width,height);
				controls.setZoom(zoom);
				imagePanel.getVerticalScrollBar().setValue(0);
				imagePanel.getHorizontalScrollBar().setValue(0);
			}
		});
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase _input) {
		GrayU8 input = (GrayU8)_input;

		if( controls.showInput ) {
			output.createGraphics().drawImage(buffered,0,0,null);
		} else {
			long before = System.nanoTime();
			if( controls.activeAlgorithm.equals(HISTOGRAM_GLOBAL)) {
				ImageStatistics.histogram(input,0,histogram);
				EnhanceImageOps.equalize(histogram, transform);
				EnhanceImageOps.applyTransform(input, transform, enhanced);
			} else if( controls.activeAlgorithm.equals(HISTOGRAM_LOCAL)) {
				EnhanceImageOps.equalizeLocal(input, controls.radius, enhanced, histogram, transform);
			} else if( controls.activeAlgorithm.equals(SHARPEN_4)) {
				EnhanceImageOps.sharpen4(input, enhanced);
			} else if( controls.activeAlgorithm.equals(SHARPEN_8)) {
				EnhanceImageOps.sharpen8(input, enhanced);
			}
			long after = System.nanoTime();

			controls.setProcessingTime((after-before)*1e-9);

			ConvertBufferedImage.convertTo(enhanced, output);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				imagePanel.setBufferedImage(output);
				imagePanel.repaint();
			}
		});
	}

	protected void handleVisualsUpdate() {
		imagePanel.setScale(controls.zoom);
		imagePanel.repaint();
	}

	protected void handleSettingsChanged() {
		if( inputMethod == InputMethod.IMAGE ) {
			reprocessInput();
		}
	}

	class ControlPanel extends StandardAlgConfigPanel implements ChangeListener {
		protected JLabel processingTimeLabel = new JLabel();

		Vector comboBoxItems = new Vector();
		JComboBox comboAlgorithms = new JComboBox();
		List<Runnable> comboRunnable = new ArrayList<>();

		protected JSpinner selectZoom;
		JSpinner spinnerRadius;
		JCheckBox checkShowInput = new JCheckBox("Show Input");

		String activeAlgorithm;
		protected double zoom = 1;
		protected int radius = 50;
		protected boolean showInput=false;

		public ControlPanel() {
			final DefaultComboBoxModel model = new DefaultComboBoxModel(comboBoxItems);
			comboAlgorithms = new JComboBox(model);
			comboAlgorithms.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					int selectedAlgorithm = comboAlgorithms.getSelectedIndex();
					activeAlgorithm = (String)comboAlgorithms.getModel().getSelectedItem();
					comboRunnable.get(selectedAlgorithm).run();
					handleSettingsChanged();
				}
			});

			selectZoom = new JSpinner(new SpinnerNumberModel(zoom,MIN_ZOOM,MAX_ZOOM,1));
			selectZoom.addChangeListener(this);
			selectZoom.setMaximumSize(selectZoom.getPreferredSize());

			checkShowInput.setSelected(showInput);
			checkShowInput.setMaximumSize(checkShowInput.getPreferredSize());
			checkShowInput.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					showInput = checkShowInput.isSelected();
					handleSettingsChanged(); // doesn't need to reprocess but easiest way to show input or not
				}
			});

			spinnerRadius = new JSpinner(new SpinnerNumberModel(radius,1,200,1));
			spinnerRadius.addChangeListener(this);
			spinnerRadius.setMaximumSize(spinnerRadius.getPreferredSize());

			addAlgorithms();
			comboAlgorithms.setSelectedIndex(0);
			comboAlgorithms.setMaximumSize(comboAlgorithms.getPreferredSize());

			addLabeled(processingTimeLabel,"Time (ms)", this);
			add(comboAlgorithms);
			addLabeled(selectZoom,"Zoom",this);
			add(checkShowInput);
			addLabeled(spinnerRadius,"Radius", this);
			addVerticalGlue(this);
		}

		private void addAlgorithms() {
			addAlgorithm(HISTOGRAM_GLOBAL,false);
			addAlgorithm(HISTOGRAM_LOCAL,true);
			addAlgorithm(SHARPEN_4,false);
			addAlgorithm(SHARPEN_8,false);
		}

		public void addAlgorithm(String name, final boolean usesRadius ) {
			comboBoxItems.add(name);

			comboRunnable.add(new Runnable() {
				@Override
				public void run() {
					spinnerRadius.setEnabled(usesRadius);
				}
			});
		}

		public void setZoom( double zoom ) {
			zoom = Math.max(MIN_ZOOM,zoom);
			zoom = Math.min(MAX_ZOOM,zoom);
			this.zoom = zoom;

			BoofSwingUtil.invokeNowOrLater(new Runnable() {
				@Override
				public void run() {
					selectZoom.setValue(ControlPanel.this.zoom);
				}
			});
		}

		public void setProcessingTime( double seconds ) {
			processingTimeLabel.setText(String.format("%7.1f",(seconds*1000)));
		}

		@Override
		public void stateChanged(ChangeEvent e) {
			if( e.getSource() == selectZoom ) {
				zoom = ((Number) selectZoom.getValue()).doubleValue();
				handleVisualsUpdate();
				return;
			} else if( e.getSource() == spinnerRadius ) {
				radius = ((Number) spinnerRadius.getValue()).intValue();
			}
			handleSettingsChanged();
		}
	}

	public static void main(String[] args) {
		List<String> examples = new ArrayList<>();
		examples.add("enhance/dark.jpg");
		examples.add("enhance/dull.jpg");

		ImageEnhanceApp app = new ImageEnhanceApp(examples);

		app.openFile(new File(examples.get(0)));

		app.waitUntilInputSizeIsKnown();

		ShowImages.showWindow(app,"Image Enhancement",true);
	}

}
