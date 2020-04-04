/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.associate.AssociateDescription;
import boofcv.abst.feature.associate.ScoreAssociation;
import boofcv.abst.feature.describe.ConfigBrief;
import boofcv.abst.feature.describe.ConfigSiftScaleSpace;
import boofcv.abst.feature.describe.ConfigSurfDescribe;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.detect.interest.PointDetectorTypes;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GConvertImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.demonstrations.feature.detect.interest.*;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientation;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.dialogs.OpenImageSetDialog;
import boofcv.gui.feature.AssociationPanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * Visually shows the location of matching pairs of associated features in two images.
 *
 * @author Peter Abeles
 */
public class VisualizeAssociationMatchesApp<T extends ImageGray<T>, D extends ImageGray<D>>
		extends DemonstrationBase {

	// algorithms
	InterestPointDetector<T> detector;
	DescribeRegionPoint descriptor;
	AssociateDescription<TupleDesc> matcher;
	OrientationImage<T> orientation;
	boolean algorithmChange=true;
	boolean failure=false;
	// images
	BufferedImage buffLeft;
	BufferedImage buffRight;
	Planar<T> colorLeft,colorRight;
	T grayLeft,grayRight;

	Class<T> imageType;

	AssociationPanel panel = new AssociationPanel(20);
	AssociateControls controls = new AssociateControls();

	// selected algorithms
	private int selectedDetector;
	private int selectedDescriptor;
	private int selectedAssoc;

	// tells the progress monitor how far along it is
	volatile int progress;

	public VisualizeAssociationMatchesApp(java.util.List<PathLabel> examples , Class<T> imageType) {
		super(true,false,examples,ImageType.pl(3,imageType));
		this.imageType = imageType;

		colorLeft = new Planar<>(imageType, 1, 1, 3);
		colorRight = new Planar<>(imageType, 1, 1, 3);
		grayLeft = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		grayRight = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, panel);
	}

	@Override
	protected void handleInputChange( int source , InputMethod method , final int width , final int height ) {
		switch( source ) {
			case 0:
				buffLeft = ConvertBufferedImage.checkDeclare(width,height,buffLeft,BufferedImage.TYPE_INT_RGB);
				colorLeft.reshape(width,height);
				grayLeft.reshape(width,height);
				break;

//			case 1:
//				buffRight = ConvertBufferedImage.checkDeclare(width,height,buffRight,BufferedImage.TYPE_INT_RGB);
//				colorRight.reshape(width,height);
//				grayRight.reshape(width,height);
//				break;
		}
		SwingUtilities.invokeLater(()->{
			panel.setPreferredSize(width,height,width,height);
			controls.setImageSize(width,height);
		});
	}

	@Override
	protected void handleInputFailure(int source, String error) {
		failure = true;
		JOptionPane.showMessageDialog(this, error);
	}

	@Override
	protected void openFileMenuBar() {
		String[] files = BoofSwingUtil.openImageSetChooser(window, OpenImageSetDialog.Mode.EXACTLY,2);
		if( files == null )
			return;
		BoofSwingUtil.invokeNowOrLater(()->openImageSet(false,files));
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage bufferedIn, ImageBase input) {
		switch( sourceID ) {
			case 0:
				failure = false;
				buffLeft.createGraphics().drawImage(bufferedIn,0,0,null);
				colorLeft.setTo((Planar<T>)input);
				GConvertImage.average(colorLeft,grayLeft);
				break;

			case 1:
				if( failure ) // abort if it failed earlier
					return;
				int width = input.width;
				int height = input.height;
				// work around for input change only working for the first image
				buffRight = ConvertBufferedImage.checkDeclare(width,height,buffRight,BufferedImage.TYPE_INT_RGB);
				colorRight.reshape(width,height);
				grayRight.reshape(width,height);

				buffRight.createGraphics().drawImage(bufferedIn,0,0,null);
				colorRight.setTo((Planar<T>)input);
				GConvertImage.average(colorRight,grayRight);

				BoofSwingUtil.invokeNowOrLater((()->{
					panel.setImages(buffLeft,buffRight);
				}));
				processImage();
				break;
		}
	}

	private boolean declareAlgorithms() {
		switch( selectedDetector ) {
			case 0: detector = FactoryInterestPoint.fastHessian(controls.configFastHessian); break;
			case 1: detector = FactoryInterestPoint.sift(controls.configSiftScaleSpace,controls.configSiftDetector,imageType); break;
			case 2: {
				GeneralFeatureDetector<T, D> alg = controls.controlDetectPoint.create(imageType);
				detector = FactoryInterestPoint.wrapPoint(alg, 1, imageType, alg.getDerivType());
			} break;

			default:
				throw new IllegalArgumentException("Unknown detector");
		}

		switch( selectedDescriptor ) {
			case 0:
				if( controls.controlDescSurfSpeed.color ) {
					descriptor = FactoryDescribeRegionPoint.surfColorFast(
							controls.configSurfSpeed, ImageType.pl(3, imageType));
				} else {
					descriptor = FactoryDescribeRegionPoint.surfFast(controls.configSurfSpeed, imageType);
				}
				break;
			case 1:
				if( controls.controlDescSurfStable.color ) {
					descriptor = FactoryDescribeRegionPoint.surfColorStable(
							controls.configSurfStability, ImageType.pl(3, imageType));
				} else {
					descriptor = FactoryDescribeRegionPoint.surfStable(controls.configSurfStability, imageType);
				}
				break;
			case 2:descriptor = FactoryDescribeRegionPoint.sift(
					controls.configSiftScaleSpace,controls.controlDescSift.config, imageType); break;
			case 3:descriptor = FactoryDescribeRegionPoint.brief(controls.controlDescBrief.config, imageType); break;
			case 4:descriptor = FactoryDescribeRegionPoint.template(controls.controlDescTemplate.config, imageType); break;
			default:
				throw new IllegalArgumentException("Unknown descriptor");
		}

		// estimate orientation using this once since it is fast and accurate
		Class integralType = GIntegralImageOps.getIntegralType(imageType);
		OrientationIntegral orientationII = FactoryOrientationAlgs.sliding_ii(null, integralType);
		orientation = FactoryOrientation.convertImage(orientationII,imageType);

		ScoreAssociation scorer = FactoryAssociation.defaultScore(descriptor.getDescriptionType());
		int DOF = descriptor.createDescription().size();

		if( selectedAssoc < 2 ) {
			switch( selectedAssoc ) {
				case 0: matcher = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, false); break;
				case 1: matcher = FactoryAssociation.greedy(scorer, Double.MAX_VALUE, true); break;
			}
		} else {
			if( !TupleDesc_F64.class.isAssignableFrom(descriptor.getDescriptionType())) {
				JOptionPane.showMessageDialog(this, "Requires TupleDesc_F64 description type");
				return false;
			}

			switch( selectedAssoc ) {
				case 2: matcher = (AssociateDescription)FactoryAssociation.kdtree(null,DOF, 75); break;
				case 3: matcher = (AssociateDescription)FactoryAssociation.kdRandomForest(null,DOF, 75, 10, 5, 1233445565); break;
				default:
					throw new IllegalArgumentException("Unknown association");
			}
		}
		return true;
	}

	private void processImage() {
		if( algorithmChange ) {
			algorithmChange = false;
			if( !declareAlgorithms() ) {
				return;
			}
		}

		long time0 = System.nanoTime();
		final List<Point2D_F64> leftPts = new ArrayList<>();
		final List<Point2D_F64> rightPts = new ArrayList<>();
		FastQueue<TupleDesc> leftDesc = UtilFeature.createQueue(descriptor, 10);
		FastQueue<TupleDesc> rightDesc = UtilFeature.createQueue(descriptor,10);

		final ProgressMonitor progressMonitor = new ProgressMonitor(this,
				"Associating Features",
				"Detecting Left", 0, 3);

		// show a progress dialog if it is slow.  Needs to be in its own thread so if this stalls
		// the window will pop up
		progress = 0;
		new Thread() {
			public synchronized void run() {
				while (progress < 3) {
					SwingUtilities.invokeLater(() -> progressMonitor.setProgress(progress));
					try {
						wait(100);
					} catch (InterruptedException e) {
					}
				}
				progressMonitor.close();
			}
		}.start();


		// find feature points  and descriptions
		extractImageFeatures(colorLeft,grayLeft, leftDesc, leftPts);
		progress++;
		extractImageFeatures(colorRight,grayRight, rightDesc, rightPts);
		progress++;
		matcher.setSource(leftDesc);
		matcher.setDestination(rightDesc);
		matcher.associate();
		progress = 3;

		long time1 = System.nanoTime();
		controls.setTime((time1-time0)*1e-6); // TODO in gui thread?

		SwingUtilities.invokeLater(() -> {
			panel.setAssociation(leftPts, rightPts, matcher.getMatches());
			repaint();
		});
	}

	private void extractImageFeatures(Planar<T> color , T gray, FastQueue<TupleDesc> descs, List<Point2D_F64> locs) {
		detector.detect(gray);
		if( descriptor.getImageType().getFamily() == ImageType.Family.GRAY)
			descriptor.setImage(gray);
		else
			descriptor.setImage(color);
		orientation.setImage(gray);

		if (detector.hasScale()) {
			for (int i = 0; i < detector.getNumberOfFeatures(); i++) {
				double yaw = 0;

				Point2D_F64 pt = detector.getLocation(i);
				double radius = detector.getRadius(i);
				if (descriptor.requiresOrientation()) {
					orientation.setObjectRadius(radius);
					yaw = orientation.compute(pt.x, pt.y);
				}

				TupleDesc d = descs.grow();
				if (descriptor.process(pt.x, pt.y, yaw, radius, d)) {
					locs.add(pt.copy());
				} else {
					descs.removeTail();
				}
			}
		} else {
			orientation.setObjectRadius(10);
			for (int i = 0; i < detector.getNumberOfFeatures(); i++) {
				double yaw = 0;

				Point2D_F64 pt = detector.getLocation(i);
				if (descriptor.requiresOrientation()) {
					yaw = orientation.compute(pt.x, pt.y);
				}

				TupleDesc d = descs.grow();
				if (descriptor.process(pt.x, pt.y, yaw, 1, d)) {
					locs.add(pt.copy());
				} else {
					descs.removeTail();
				}
			}
		}
	}

	class AssociateControls extends StandardAlgConfigPanel implements ActionListener {
		JLabel labelTime = new JLabel();
		JLabel labelSize = new JLabel();
		JComboBox<String> comboDetect;
		JComboBox<String> comboDescribe;
		JComboBox<String> comboAssoc;

		// Containers for different sets of controls
		JPanel panelDetector = new JPanel();
		JPanel panelDescriptor = new JPanel();
		JPanel panelAssociate = new JPanel();

		// Controls for different detectors / descriptors
		ControlPanelSiftDetector controlDetectSift;
		ControlPanelFastHessian controlDetectFastHessian;
		ControlPanelPointDetector controlDetectPoint =
				new ControlPanelPointDetector(500, PointDetectorTypes.SHI_TOMASI,this::handleControlsUpdated);
		ControlPanelSurfDescribe.Speed controlDescSurfSpeed;
		ControlPanelSurfDescribe.Stability controlDescSurfStable;
		ControlPanelDescribeSift controlDescSift = new ControlPanelDescribeSift(null,this::handleControlsUpdated);
		ControlPanelDescribeBrief controlDescBrief =
				new ControlPanelDescribeBrief(new ConfigBrief(false),this::handleControlsUpdated);
		ControlPanelDescribeTemplate controlDescTemplate =
				new ControlPanelDescribeTemplate(null,this::handleControlsUpdated);

		// Configurations for detectors / descriptors
		ConfigFastHessian configFastHessian = new ConfigFastHessian( 1, 2, 200, 1, 9, 4, 4);
		ConfigSiftDetector configSiftDetector = new ConfigSiftDetector();
		ConfigSiftScaleSpace configSiftScaleSpace = new ConfigSiftScaleSpace();
		ConfigSurfDescribe.Speed configSurfSpeed = new ConfigSurfDescribe.Speed();
		ConfigSurfDescribe.Stability configSurfStability = new ConfigSurfDescribe.Stability();

		public AssociateControls() {
			comboDetect = combo(selectedDetector,"Fast Hessian","SIFT","Points");
			comboDescribe = combo(selectedDescriptor,"SURF-F","SURF-S","SIFT","BRIEF","Template");
			comboAssoc = combo(selectedAssoc,"Greedy","Greedy Backwards","K-D Tree BBF","Random Forest");

			// custom configurations for this demo
			configSiftDetector.maxFeaturesPerScale = 400;

			// create the algorithm controls
			controlDetectFastHessian = new ControlPanelFastHessian(configFastHessian,this::handleControlsUpdated);
			controlDetectFastHessian.setBorder(BorderFactory.createEmptyBorder());
			controlDetectSift = new ControlPanelSiftDetector(configSiftScaleSpace,configSiftDetector, this::handleControlsUpdated);
			controlDetectSift.setBorder(BorderFactory.createEmptyBorder());
			controlDescSurfSpeed = new ControlPanelSurfDescribe.Speed(configSurfSpeed,this::handleControlsUpdated);
			controlDescSurfSpeed.setBorder(BorderFactory.createEmptyBorder());
			controlDescSurfStable = new ControlPanelSurfDescribe.Stability(configSurfStability,this::handleControlsUpdated);
			controlDescSurfStable.setBorder(BorderFactory.createEmptyBorder());
			controlDescSift.setBorder(BorderFactory.createEmptyBorder());
			controlDescTemplate.setBorder(BorderFactory.createEmptyBorder());

			// Finish
			labelTime.setPreferredSize(new Dimension(70,26));
			labelTime.setHorizontalAlignment(SwingConstants.RIGHT);

			JTabbedPane tabbed = new JTabbedPane();
			tabbed.addTab("Detect",panelDetector);
			tabbed.addTab("Describe",panelDescriptor);
			tabbed.addTab("Associate",panelAssociate);

			handleDetectorChanged();
			handleDescriptorChanged();

			addLabeled(labelTime,"Time (ms)");
			add(labelSize);
			addLabeled(comboDetect,"Detect");
			addLabeled(comboDescribe,"Describe");
			addLabeled(comboAssoc,"Associate");
			add(tabbed);
		}

		private void handleControlsUpdated() {
			algorithmChange = true;
			reprocessInput();
		}

		private void handleDetectorChanged() {
			panelDetector.removeAll();
			switch( selectedDetector ) {
				case 0: panelDetector.add(controlDetectFastHessian); break;
				case 1: panelDetector.add(controlDetectSift); break;
				case 2: panelDetector.add(controlDetectPoint); break;
			}
			panelDescriptor.invalidate();
		}

		private void handleDescriptorChanged() {
			panelDescriptor.removeAll();
			switch( selectedDescriptor ) {
				case 0: panelDescriptor.add(controlDescSurfSpeed); break;
				case 1: panelDescriptor.add(controlDescSurfStable); break;
				case 2: panelDescriptor.add(controlDescSift); break;
				case 3: panelDescriptor.add(controlDescBrief); break;
				case 4: panelDescriptor.add(controlDescTemplate); break;
			}
			panelDescriptor.invalidate();
		}

		public void setTime( double milliseconds ) {
			labelTime.setText(String.format("%.1f",milliseconds));
		}

		public void setImageSize( int width , int height ) {
			labelSize.setText(width+" x "+height);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if( comboDetect == e.getSource() ) {
				selectedDetector = comboDetect.getSelectedIndex();
				handleDetectorChanged();
			} else if( comboDescribe == e.getSource() ){
				selectedDescriptor = comboDescribe.getSelectedIndex();
				handleDescriptorChanged();
			} else if( comboAssoc == e.getSource() ){
				selectedAssoc = comboAssoc.getSelectedIndex();
			}
			algorithmChange = true;
			reprocessInput();
		}
	}

	public static void main(String args[]) {
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

		SwingUtilities.invokeLater(()->{
			VisualizeAssociationMatchesApp app = new VisualizeAssociationMatchesApp(examples,GrayF32.class);

			// Processing time takes a bit so don't open right away
			app.openExample(examples.get(0));
			app.display("Associated Matches");
		});
	}
}
