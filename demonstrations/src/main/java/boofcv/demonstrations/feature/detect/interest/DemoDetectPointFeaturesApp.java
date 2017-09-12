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

package boofcv.demonstrations.feature.detect.interest;

import boofcv.abst.feature.detect.interest.ConfigFast;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.EasyGeneralFeatureDetector;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase2;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Runs several point feature detection algorithms
 *
 * @author Peter Abeles
 */
public class DemoDetectPointFeaturesApp<T extends ImageGray<T>> extends DemonstrationBase2 {
	protected Class<T> imageClass;
	protected Class derivClass;

	VisualizePanel imagePanel = new VisualizePanel();
	ControlPanel controls = new ControlPanel();

	EasyGeneralFeatureDetector detector;
	boolean detectorChanged = true;

	final Object featureLock = new Object();
	FastQueue<Point2D_I16> positive = new FastQueue<>(Point2D_I16.class,true);
	FastQueue<Point2D_I16> negative = new FastQueue<>(Point2D_I16.class,true);
	// END OWNED BY LOCK

	public DemoDetectPointFeaturesApp(List<?> exampleInputs, Class<T> imageClass ) {
		super(exampleInputs, ImageType.single(imageClass));
		this.imageClass = imageClass;
		this.derivClass = GImageDerivativeOps.getDerivativeType(imageClass);

		imagePanel.setPreferredSize(new Dimension(800,800));

		imagePanel.addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {

				double curr = DemoDetectPointFeaturesApp.this.controls.zoom;

				if( e.getWheelRotation() > 0 )
					curr *= 1.1;
				else
					curr /= 1.1;
				controls.setZoom(curr);
			}
		});

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, imagePanel);
	}

	private void createHarris() {
		controls.configExtract.detectMinimums = false;
		controls.adjustControls(true,false);
		changeDetector(FactoryDetectPoint.createHarris(controls.configExtract, controls.weighted, derivClass));
	}
	private void createShiTomasi() {
		controls.configExtract.detectMinimums = false;
		controls.adjustControls(true,false);
		changeDetector(FactoryDetectPoint.createShiTomasi(controls.configExtract, controls.weighted, derivClass));
	}
	private void createFast() {
		controls.configExtract.detectMinimums = false;
		controls.adjustControls(false,true);
		changeDetector(FactoryDetectPoint.createFast(
				new ConfigFast(controls.fastPixelTol,9),controls.configExtract, imageClass));
	}
	private void createKitRos() {
		controls.configExtract.detectMinimums = false;
		controls.adjustControls(false,false);
		changeDetector(FactoryDetectPoint.createKitRos(controls.configExtract, derivClass));
	}
	private void createMedian() {
		controls.configExtract.detectMinimums = false;
		controls.adjustControls(false,false);
		changeDetector(FactoryDetectPoint.createMedian(controls.configExtract, imageClass));
	}
	private void createHessian() {
		controls.configExtract.detectMinimums = false;
		controls.adjustControls(false,false);
		changeDetector(FactoryDetectPoint.createHessian(HessianBlobIntensity.Type.DETERMINANT,
				controls.configExtract, derivClass));
	}
	private void createLaplace() {
		controls.configExtract.detectMinimums = true;
		controls.adjustControls(false,false);
		changeDetector(FactoryDetectPoint.createHessian(HessianBlobIntensity.Type.TRACE,
				controls.configExtract, derivClass));
	}

	private void changeDetector(GeneralFeatureDetector fd) {
		detector = new EasyGeneralFeatureDetector<>(fd, imageClass, derivClass);
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, final int width, final int height) {
		super.handleInputChange(source, method, width, height);

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
	public void processImage(int sourceID, long frameID, final BufferedImage buffered, ImageBase input) {
		if( detectorChanged ) {
			detectorChanged = false;
			// this can be done safely outside of the GUI thread
			controls.comboActions.get( controls.selectedAlgorithm ).run();
		}

		final double seconds;
		long timeBefore = System.nanoTime();
		detector.detect((ImageGray)input,null);
		long timeAfter = System.nanoTime();
		seconds = (timeAfter-timeBefore)*1e-9;

		synchronized (featureLock) {
			positive.reset();
			negative.reset();
			if (detector.getDetector().isDetectMinimums()) {
				QueueCorner l = detector.getMinimums();
				negative.growArray(l.size);
				negative.size = 0;
				for (int i = 0; i < l.size; i++) {
					negative.grow().set(l.get(i));
				}
			}
			if (detector.getDetector().isDetectMaximums()) {
				QueueCorner l = detector.getMaximums();
				positive.growArray(l.size);
				positive.size = 0;
				for (int i = 0; i < l.size; i++) {
					positive.grow().set(l.get(i));
				}
			}
		}

		BoofSwingUtil.invokeNowOrLater(new Runnable() {
			@Override
			public void run() {
				controls.setProcessingTime(seconds);
				imagePanel.setBufferedImage(buffered);
				imagePanel.repaint();
			}
		});
	}

	private void handleSettingsChanged() {
		detectorChanged = true;
		if( inputMethod == InputMethod.IMAGE ) {
			reprocessInput();
		}
	}

	private void handleVisualsUpdate() {
		imagePanel.setScale(controls.zoom);
		imagePanel.repaint();
	}

	class VisualizePanel extends ImageZoomPanel {
		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			synchronized (featureLock) {
				for (int i = 0; i < positive.size; i++) {
					Point2D_I16 c = positive.get(i);
					VisualizeFeatures.drawPoint(g2,(int)((c.x+0.5)*scale),(int)((c.y+0.5)*scale),Color.RED);
				}
				for (int i = 0; i < negative.size; i++) {
					Point2D_I16 c = negative.get(i);
					VisualizeFeatures.drawPoint(g2,(int)((c.x+0.5)*scale),(int)((c.y+0.5)*scale),Color.BLUE);
				}
			}
		}
	}

	class ControlPanel extends StandardAlgConfigPanel implements ChangeListener {

		protected final double minZoom = 0.01;
		protected final double maxZoom = 50;

		protected JLabel processingTimeLabel = new JLabel();

		Vector comboBoxItems = new Vector();
		JComboBox comboAlgorithms = new JComboBox();
		List<Runnable> comboActions = new ArrayList<>();

		protected JSpinner selectZoom;
		JSpinner spinnerRadius;
		JSpinner spinnerThreshold;
		JSpinner spinnerMaxFeatures;
		JSpinner spinnerFastTol;
		JCheckBox checkWeighted = new JCheckBox("Weighted");

		ConfigGeneralDetector configExtract = new ConfigGeneralDetector(-1,5,0);
		boolean weighted = false;
		int selectedAlgorithm=0;
		protected double zoom = 1;
		protected int fastPixelTol = 20;


		public ControlPanel() {
			final DefaultComboBoxModel model = new DefaultComboBoxModel(comboBoxItems);
			comboAlgorithms = new JComboBox(model);
			comboAlgorithms.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					selectedAlgorithm = comboAlgorithms.getSelectedIndex();
					handleSettingsChanged();
				}
			});
			addAlgorithms();
			comboAlgorithms.setSelectedIndex(0);
			comboAlgorithms.setMaximumSize(comboAlgorithms.getPreferredSize());

			selectZoom = new JSpinner(new SpinnerNumberModel(1,minZoom,maxZoom,1));
			selectZoom.addChangeListener(this);
			selectZoom.setMaximumSize(selectZoom.getPreferredSize());

			spinnerRadius = new JSpinner(new SpinnerNumberModel(configExtract.radius,1,200,1));
			spinnerRadius.addChangeListener(this);
			spinnerRadius.setMaximumSize(spinnerRadius.getPreferredSize());

			spinnerThreshold = new JSpinner(new SpinnerNumberModel(configExtract.threshold,0,1000000,100f));
			spinnerThreshold.addChangeListener(this);
			spinnerThreshold.setMaximumSize(spinnerThreshold.getPreferredSize());

			spinnerMaxFeatures = new JSpinner(new SpinnerNumberModel(configExtract.maxFeatures,-1,1000000,10));
			spinnerMaxFeatures.addChangeListener(this);
			spinnerMaxFeatures.setMaximumSize(spinnerMaxFeatures.getPreferredSize());

			spinnerFastTol = new JSpinner(new SpinnerNumberModel(fastPixelTol,1,255,1));
			spinnerFastTol.addChangeListener(this);
			spinnerFastTol.setMaximumSize(spinnerFastTol.getPreferredSize());

			checkWeighted.setSelected(weighted);
			checkWeighted.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent actionEvent) {
					weighted = checkWeighted.isSelected();
					handleSettingsChanged();
				}
			});
			checkWeighted.setMaximumSize(checkWeighted.getPreferredSize());

			addLabeled(processingTimeLabel,"Time (ms)", this);
			add(comboAlgorithms);
			addLabeled(selectZoom,"Zoom",this);
			addLabeled(spinnerRadius,"Radius", this);
			addLabeled(spinnerMaxFeatures,"Max Features", this);
			addLabeled(spinnerThreshold,"Threshold", this);
			addAlignLeft(checkWeighted,this);
			addLabeled(spinnerFastTol,"Fast Pixel Tol", this);
			addVerticalGlue(this);
		}

		private void addAlgorithms() {
			addAlgorithm("Harris", new Runnable() { public void run() { createHarris(); } });
			addAlgorithm("Shi-Tomasi", new Runnable() { public void run() { createShiTomasi(); } });
			addAlgorithm("Fast", new Runnable() { public void run() { createFast(); } });
			addAlgorithm("KitRos", new Runnable() { public void run() { createKitRos(); } });
			addAlgorithm("Median", new Runnable() { public void run() { createMedian(); } });
			addAlgorithm("Hessian", new Runnable() { public void run() { createHessian(); } });
			addAlgorithm("Laplace", new Runnable() { public void run() { createLaplace(); } });
		}

		public void adjustControls( final boolean usesWeighted , final boolean usesFastTol ) {
			BoofSwingUtil.invokeNowOrLater(new Runnable() {
				@Override
				public void run() {
					checkWeighted.setEnabled(usesWeighted);
					spinnerFastTol.setEnabled(usesFastTol);
				}
			});
		}

		public void addAlgorithm( String name , Runnable action ) {
			comboBoxItems.add(name);
			comboActions.add(action);
		}

		public void setZoom( double zoom ) {
			zoom = Math.max(minZoom,zoom);
			zoom = Math.min(maxZoom,zoom);
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
				configExtract.radius = ((Number) spinnerRadius.getValue()).intValue();
			} else if( e.getSource() == spinnerThreshold ) {
				configExtract.threshold = ((Number) spinnerThreshold.getValue()).floatValue();
			} else if( e.getSource() == spinnerMaxFeatures ) {
				configExtract.maxFeatures = ((Number) spinnerMaxFeatures.getValue()).intValue();
			} else if( e.getSource() == spinnerFastTol ) {
				fastPixelTol = ((Number) spinnerFastTol.getValue()).intValue();
			}
			handleSettingsChanged();
		}
	}

	public static void main(String[] args) {
		List<String> examples = new ArrayList<>();
		examples.add("shapes/shapes01.png");
		examples.add("amoeba_shapes.jpg");
		examples.add("sunflowers.jpg");
		examples.add("scale/beach02.jpg");

		DemoDetectPointFeaturesApp app = new DemoDetectPointFeaturesApp(examples,GrayF32.class);

		app.openFile(new File(examples.get(0)));

		app.waitUntilInputSizeIsKnown();

		ShowImages.showWindow(app,"Point Feature Detectors",true);
	}
}
