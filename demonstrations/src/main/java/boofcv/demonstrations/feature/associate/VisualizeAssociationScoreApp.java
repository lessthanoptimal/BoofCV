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

package boofcv.demonstrations.feature.associate;

import boofcv.abst.feature.associate.*;
import boofcv.abst.feature.describe.DescribePointRadiusAngle;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.describe.ConfigDescribeRegion;
import boofcv.factory.feature.detect.interest.ConfigDetectInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientation;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.controls.ControlPanelDetDescAssocBase;
import boofcv.gui.dialogs.OpenImageSetDialog;
import boofcv.gui.feature.AssociationScorePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.feature.*;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows how tightly focused the score is around the best figure by showing the relative
 * size and number of features which have a similar score visually in the image. For example,
 * lots of other features with similar sized circles means the distribution is spread widely
 * while only one or two small figures means it is very narrow.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VisualizeAssociationScoreApp<T extends ImageGray<T>, D extends ImageGray<D>>
		extends DemonstrationBase {
	// These classes process the input images and compute association score
	InterestPointDetector<T> detector;
	DescribePointRadiusAngle descriptor;
	OrientationImage<T> orientation;

	// copies of input image
	BufferedImage buffLeft;
	BufferedImage buffRight;
	// gray scale versions of input image
	T imageLeft;
	T imageRight;
	Class<T> imageType;

	// visualizes association score
	AssociationScorePanel<TupleDesc> scorePanel;
	VisualizeScorePanel controls;
	boolean configChanged = false;

	public VisualizeAssociationScoreApp( java.util.List<PathLabel> examples, Class<T> imageType ) {
		super(true, false, examples, ImageType.single(imageType));
		this.imageType = imageType;

		imageLeft = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		imageRight = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		// estimate orientation using this once since it is fast
		Class integralType = GIntegralImageOps.getIntegralType(imageType);
		OrientationIntegral orientationII = FactoryOrientationAlgs.sliding_ii(null, integralType);
		orientation = FactoryOrientation.convertImage(orientationII, imageType);

		controls = new VisualizeScorePanel();
		scorePanel = new AssociationScorePanel<>(3);

		declareAlgorithms();

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, scorePanel);
	}

	private void declareAlgorithms() {
		detector = controls.createDetector(imageType);
		descriptor = controls.createDescriptor(imageType);

		SwingUtilities.invokeLater(() -> controls.setFeatureType(descriptor.getDescriptionType()));

		// estimate orientation using this once since it is fast and accurate
		Class integralType = GIntegralImageOps.getIntegralType(imageType);
		OrientationIntegral orientationII = FactoryOrientationAlgs.sliding_ii(null, integralType);
		orientation = FactoryOrientation.convertImage(orientationII, imageType);
	}

	@Override
	protected void openFileMenuBar() {
		String[] files = BoofSwingUtil.openImageSetChooser(window, OpenImageSetDialog.Mode.EXACTLY, 2);
		if (files == null)
			return;
		BoofSwingUtil.invokeNowOrLater(() -> openImageSet(false, files));
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, final int width, final int height ) {
		switch (source) {
			case 0:
				buffLeft = ConvertBufferedImage.checkDeclare(width, height, buffLeft, BufferedImage.TYPE_INT_RGB);
				SwingUtilities.invokeLater(() -> {
					scorePanel.setPreferredSize(width, height, width, height);
					controls.setImageSize(width, height);
				});
				break;

			case 1:
				buffRight = ConvertBufferedImage.checkDeclare(width, height, buffRight, BufferedImage.TYPE_INT_RGB);
				break;
		}
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage bufferedIn, ImageBase input ) {
		switch (sourceID) {
			case 0:
				imageLeft.setTo((T)input);
				buffLeft.createGraphics().drawImage(bufferedIn, 0, 0, null);
				break;

			case 1:
				imageRight.setTo((T)input);
				buffRight.createGraphics().drawImage(bufferedIn, 0, 0, null);

				BoofSwingUtil.invokeNowOrLater(() -> scorePanel.setImages(buffLeft, buffRight));
				processImage();
				break;
		}
	}

	/**
	 * Extracts image information and then passes that info onto scorePanel for display. Data is not
	 * recycled to avoid threading issues.
	 */
	private void processImage() {
		final List<Point2D_F64> leftPts = new ArrayList<>();
		final List<Point2D_F64> rightPts = new ArrayList<>();
		final List<TupleDesc> leftDesc = new ArrayList<>();
		final List<TupleDesc> rightDesc = new ArrayList<>();

		final ProgressMonitor progressMonitor = new ProgressMonitor(this,
				"Compute Feature Information",
				"", 0, 4);
		extractImageFeatures(progressMonitor, 0, imageLeft, leftDesc, leftPts);
		extractImageFeatures(progressMonitor, 2, imageRight, rightDesc, rightPts);

		SwingUtilities.invokeLater(() -> {
			progressMonitor.close();
			scorePanel.setScorer(controls.getSelected());
			scorePanel.setLocation(leftPts, rightPts, leftDesc, rightDesc);
			repaint();
		});
	}

	/**
	 * Detects the locations of the features in the image and extracts descriptions of each of
	 * the features.
	 */
	private void extractImageFeatures( final ProgressMonitor progressMonitor, final int progress,
									   T image,
									   List<TupleDesc> descs, List<Point2D_F64> locs ) {
		if (configChanged) {
			configChanged = false;
			declareAlgorithms();
		}

		SwingUtilities.invokeLater(() -> progressMonitor.setNote("Detecting"));
		detector.detect(image);
		SwingUtilities.invokeLater(() -> {
			progressMonitor.setProgress(progress + 1);
			progressMonitor.setNote("Describing");
		});
		descriptor.setImage(image);
		orientation.setImage(image);

		// See if the detector can detect the feature's scale
		if (detector.hasScale()) {
			for (int i = 0; i < detector.getNumberOfFeatures(); i++) {
				double yaw = 0;

				Point2D_F64 pt = detector.getLocation(i);
				double radius = detector.getRadius(i);
				if (descriptor.isOriented()) {
					orientation.setObjectRadius(radius);
					yaw = orientation.compute(pt.x, pt.y);
				}

				TupleDesc d = descriptor.createDescription();
				if (descriptor.process(pt.x, pt.y, yaw, radius, d)) {
					descs.add(d);
					locs.add(pt.copy());
				}
			}
		} else {
			// just set the radius to one in this case
			orientation.setObjectRadius(1);
			for (int i = 0; i < detector.getNumberOfFeatures(); i++) {
				double yaw = 0;

				Point2D_F64 pt = detector.getLocation(i);
				if (descriptor.isOriented()) {
					yaw = orientation.compute(pt.x, pt.y);
				}

				TupleDesc d = descriptor.createDescription();
				if (descriptor.process(pt.x, pt.y, yaw, 1, d)) {
					descs.add(d);
					locs.add(pt.copy());
				}
			}
		}
		SwingUtilities.invokeLater(() -> progressMonitor.setProgress(progress + 2));
	}

	@SuppressWarnings({"NullAway.Init"})
	public class VisualizeScorePanel extends ControlPanelDetDescAssocBase implements ActionListener {
		JLabel labelSize = new JLabel();

		// Containers for different sets of controls
		JPanel panelDetector = new JPanel(new BorderLayout());
		JPanel panelDescriptor = new JPanel(new BorderLayout());

		// selects which image to view
		JComboBox<ScoreItem> scoreTypes = new JComboBox<>();

		Class type;
		ScoreAssociation selected;

		public VisualizeScorePanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

			// custom configurations for this demo
			configDetDesc.detectFastHessian.maxFeaturesPerScale = 200;
			configDetDesc.detectSift.maxFeaturesPerScale = 400;
			configDetDesc.detectPoint.general.maxFeatures = 800;
			configDetDesc.detectPoint.general.radius = 5;

			// create the algorithm controls
			initializeControlsGUI();

			scoreTypes.addActionListener(this);
			scoreTypes.setMaximumSize(scoreTypes.getPreferredSize());

			JTabbedPane tabbed = new JTabbedPane();
			tabbed.addTab("Detect", panelDetector);
			tabbed.addTab("Describe", panelDescriptor);

			handleDetectorChanged();
			handleDescriptorChanged();

			add(labelSize);
			addLabeled(comboDetect, "Detect");
			addLabeled(comboDescribe, "Describe");
			addLabeled(scoreTypes, "Score");
			add(tabbed);
		}

		@Override
		protected void handleControlsUpdated() {
			configChanged = true;
			reprocessInput();
		}

		private void handleDetectorChanged() {
			panelDetector.removeAll();
			panelDetector.add(getDetectorPanel(), BorderLayout.CENTER);
			panelDetector.invalidate();
			handleControlsUpdated();
		}

		private void handleDescriptorChanged() {
			panelDescriptor.removeAll();
			panelDescriptor.add(getDescriptorPanel(), BorderLayout.CENTER);
			panelDescriptor.invalidate();
			handleControlsUpdated();
		}

		public void setImageSize( int width, int height ) {
			labelSize.setText(width + " x " + height);
		}

		public void setFeatureType( Class type ) {
			if (this.type == type)
				return;

			this.type = type;

			scoreTypes.removeActionListener(this);
			scoreTypes.removeAllItems();
			if (type == TupleDesc_B.class) {
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateHamming_B(), "Hamming"));
			} else if (type == NccFeature.class) {
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateNccFeature(), "NCC"));
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclidean_F64(), "Euclidean"));
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclideanSq.F64(), "Euclidean2"));
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateSad.F64(), "SAD"));
			} else if (TupleDesc_F64.class.isAssignableFrom(type)) {
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclidean_F64(), "Euclidean"));
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclideanSq.F64(), "Euclidean2"));
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateSad.F64(), "SAD"));
			} else if (type == TupleDesc_F32.class) {
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclideanSq.F32(), "Euclidean2"));
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateSad.F32(), "SAD"));
			} else if (type == TupleDesc_U8.class) {
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateSad.U8(), "SAD"));
			} else {
				throw new RuntimeException("Unknown description type " + type.getSimpleName());
			}
			selected = ((ScoreItem)scoreTypes.getSelectedItem()).assoc;
			scoreTypes.revalidate();
			scoreTypes.addActionListener(this);
		}

		@Override
		public void actionPerformed( ActionEvent e ) {
			if (comboDetect == e.getSource()) {
				configDetDesc.typeDetector =
						ConfigDetectInterestPoint.Type.values()[comboDetect.getSelectedIndex()];
				handleDetectorChanged();
			} else if (comboDescribe == e.getSource()) {
				configDetDesc.typeDescribe =
						ConfigDescribeRegion.Type.values()[comboDescribe.getSelectedIndex()];
				handleDescriptorChanged();
			} else if (scoreTypes == e.getSource()) {
				ScoreItem item = (ScoreItem)scoreTypes.getSelectedItem();
				selected = item.assoc;
				scorePanel.setScorer(controls.getSelected());
			}
		}

		public ScoreAssociation getSelected() {
			return selected;
		}
	}

	private static class ScoreItem {
		String name;
		ScoreAssociation assoc;

		private ScoreItem( ScoreAssociation assoc, String name ) {
			this.assoc = assoc;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static void main( String[] args ) {
		List<PathLabel> examples = new ArrayList<>();

		examples.add(new PathLabel("Cave",
				UtilIO.pathExample("stitch/cave_01.jpg"), UtilIO.pathExample("stitch/cave_02.jpg")));
		examples.add(new PathLabel("Kayak",
				UtilIO.pathExample("stitch/kayak_02.jpg"), UtilIO.pathExample("stitch/kayak_03.jpg")));
		examples.add(new PathLabel("Forest",
				UtilIO.pathExample("scale/rainforest_01.jpg"), UtilIO.pathExample("scale/rainforest_02.jpg")));
		examples.add(new PathLabel("Building",
				UtilIO.pathExample("stitch/apartment_building_01.jpg"), UtilIO.pathExample("stitch/apartment_building_02.jpg")));
		examples.add(new PathLabel("Trees Rotate",
				UtilIO.pathExample("stitch/trees_rotate_01.jpg"), UtilIO.pathExample("stitch/trees_rotate_03.jpg")));

		SwingUtilities.invokeLater(() -> {
			VisualizeAssociationScoreApp app = new VisualizeAssociationScoreApp(examples, GrayF32.class);

			// Processing time takes a bit so don't open right away
			app.openExample(examples.get(0));
			app.display("Visualize Association Score");
		});
	}
}
