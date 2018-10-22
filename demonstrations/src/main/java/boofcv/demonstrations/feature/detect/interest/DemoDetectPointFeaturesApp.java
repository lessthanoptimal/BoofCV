/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.detect.interest.ConfigFastCorner;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.detect.interest.GeneralToPointDetector;
import boofcv.abst.feature.detect.interest.PointDetector;
import boofcv.alg.feature.detect.intensity.HessianBlobIntensity;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.demonstrations.shapes.ShapeVisualizePanel;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I16;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static boofcv.gui.BoofSwingUtil.MAX_ZOOM;
import static boofcv.gui.BoofSwingUtil.MIN_ZOOM;

/**
 * Runs several point feature detection algorithms
 *
 * @author Peter Abeles
 */
public class DemoDetectPointFeaturesApp<T extends ImageGray<T>> extends DemonstrationBase {
	protected Class<T> imageClass;
	protected Class derivClass;

	VisualizePanel imagePanel = new VisualizePanel();
	ControlPanel controls = new ControlPanel();

	PointDetector<T> detector;
	boolean detectorChanged = true;

	final Object featureLock = new Object();
	List<QueueCorner> sets = new ArrayList<>();
	// END OWNED BY LOCK

	public DemoDetectPointFeaturesApp(List<?> exampleInputs, Class<T> imageClass ) {
		super(true,true,exampleInputs, ImageType.single(imageClass));
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

	@Override
	protected void configureVideo(int which, SimpleImageSequence sequence) {
		sequence.setLoop(true);
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
		detector.process((T)input);
		long timeAfter = System.nanoTime();
		seconds = (timeAfter-timeBefore)*1e-9;

		synchronized (featureLock) {
			for (int i = 0; i < detector.totalSets(); i++) {
				QueueCorner src = detector.getPointSet(i);
				QueueCorner dst = sets.get(i);
				dst.reset();
				dst.addAll(src);
			}
		}

		BoofSwingUtil.invokeNowOrLater(() -> {
			controls.setProcessingTime(seconds);
			imagePanel.setBufferedImage(buffered);
			imagePanel.repaint();
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

	class VisualizePanel extends ShapeVisualizePanel {
		Ellipse2D.Double circle = new Ellipse2D.Double();
		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			BoofSwingUtil.antialiasing(g2);

			synchronized (featureLock) {
				double r = 5;
				for (int setIndex = 0; setIndex < sets.size(); setIndex++) {
					Color color = setIndex == 0 ? Color.RED : Color.BLUE;
					QueueCorner points = sets.get(setIndex);
					for (int i = 0; i < points.size; i++) {
						Point2D_I16 c = points.get(i);
						VisualizeFeatures.drawPoint(g2,c.x*scale,c.y*scale,r,color,true,circle);
					}
				}
			}
		}
	}

	class ControlPanel extends StandardAlgConfigPanel implements ChangeListener {

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

			selectZoom = spinner(zoom,MIN_ZOOM,MAX_ZOOM,1);
			spinnerRadius = spinner(configExtract.radius,1,200,1);
			spinnerThreshold = spinner(configExtract.threshold,0,1000000,100f);
			spinnerMaxFeatures = spinner(configExtract.maxFeatures,-1,1000000,10);
			spinnerFastTol = spinner(fastPixelTol,1,255,1);

			checkWeighted.setSelected(weighted);
			checkWeighted.addActionListener(actionEvent -> {
				weighted = checkWeighted.isSelected();
				handleSettingsChanged();
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
			addAlgorithm("Harris", () -> createHarris());
			addAlgorithm("Shi-Tomasi", () -> createShiTomasi());
			addAlgorithm("Fast Intensity", () -> createFastIntensity());
			addAlgorithm("Fast", () -> createFast());
			addAlgorithm("KitRos", () -> createKitRos());
			addAlgorithm("Median", () -> createMedian());
			addAlgorithm("Hessian", () -> createHessian());
			addAlgorithm("Laplace", () -> createLaplace());
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
	private void createFastIntensity() {
		controls.configExtract.detectMinimums = true;
		controls.adjustControls(false,true);
		changeDetector(FactoryDetectPoint.createFast(
				new ConfigFastCorner(controls.fastPixelTol,9),controls.configExtract, imageClass));
	}
	private void createFast() {
		controls.adjustControls(false,true);
		ConfigFastCorner configFast = new ConfigFastCorner(controls.fastPixelTol,9);
		changeDetector(FactoryDetectPoint.createFast(configFast,imageClass));
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
		detector = new GeneralToPointDetector(fd,imageClass, derivClass);

		sets.clear();
		for (int i = 0; i < detector.totalSets(); i++) {
			sets.add( new QueueCorner());
		}
	}

	private void changeDetector(PointDetector fd) {
		detector = fd;

		sets.clear();
		for (int i = 0; i < detector.totalSets(); i++) {
			sets.add( new QueueCorner());
		}
	}


	public static void main(String[] args) {
		List<PathLabel> examples = new ArrayList<>();
		examples.add(new PathLabel("Chessboard", UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Chess/frame06.jpg")));
		examples.add(new PathLabel("Square Grid",UtilIO.pathExample("calibration/mono/Sony_DSC-HX5V_Square/frame06.jpg")));
		examples.add(new PathLabel("Shapes 01",UtilIO.pathExample("shapes/shapes01.png")));
		examples.add(new PathLabel("Amoeba Shapes",UtilIO.pathExample("amoeba_shapes.jpg")));
		examples.add(new PathLabel("Sunflowers",UtilIO.pathExample("sunflowers.jpg")));
		examples.add(new PathLabel("Beach",UtilIO.pathExample("scale/beach02.jpg")));
		examples.add(new PathLabel("Chessboard Movie",UtilIO.pathExample("fiducial/chessboard/movie.mjpeg")));

		DemoDetectPointFeaturesApp app = new DemoDetectPointFeaturesApp(examples,GrayF32.class);
		app.openExample(examples.get(0));
		app.waitUntilInputSizeIsKnown();
		app.display("Point Feature Detectors");
	}
}
