/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.describe.ConfigBrief;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.ConfigFastHessian;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.detect.interest.ConfigSiftDetector;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.abst.feature.orientation.OrientationImage;
import boofcv.abst.feature.orientation.OrientationIntegral;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.factory.feature.detect.interest.FactoryInterestPoint;
import boofcv.factory.feature.orientation.FactoryOrientation;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.StandardAlgConfigPanel;
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
 * size and number of features which have a similar score visually in the image.  For example,
 * lots of other features with similar sized circles means the distribution is spread widely
 * while only one or two small figures means it is very narrow.
 *
 * @author Peter Abeles
 */
public class VisualizeAssociationScoreApp<T extends ImageGray<T>, D extends ImageGray<D>>
		extends DemonstrationBase {
	// These classes process the input images and compute association score
	InterestPointDetector<T> detector;
	DescribeRegionPoint descriptor;
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
	VisualizeScorePanel controlPanel;

	public VisualizeAssociationScoreApp(java.util.List<PathLabel> examples , Class<T> imageType) {
		super(true,false,examples, ImageType.single(imageType));
		this.imageType = imageType;

		imageLeft = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
		imageRight = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		// estimate orientation using this once since it is fast
		Class integralType = GIntegralImageOps.getIntegralType(imageType);
		OrientationIntegral orientationII = FactoryOrientationAlgs.sliding_ii(null, integralType);
		orientation = FactoryOrientation.convertImage(orientationII, imageType);

		controlPanel = new VisualizeScorePanel();
		scorePanel = new AssociationScorePanel<>(3);

		declareAlgorithms();

		add(BorderLayout.WEST, controlPanel);
		add(BorderLayout.CENTER, scorePanel);
	}

	private void declareAlgorithms() {
		switch( controlPanel.selectedDetector ) {
			case 0: detector = FactoryInterestPoint.fastHessian(new ConfigFastHessian( 1, 2, 200, 1, 9, 4, 4)); break;
			case 1: detector = FactoryInterestPoint.sift(null,new ConfigSiftDetector(400),imageType); break;
			case 2: {
				Class derivType = GImageDerivativeOps.getDerivativeType(imageType);
				GeneralFeatureDetector<T, D> alg = FactoryDetectPoint.createShiTomasi(new ConfigGeneralDetector(500,2,1), null, derivType);
				detector = FactoryInterestPoint.wrapPoint(alg, 1, imageType, derivType);
			} break;

			default:
				throw new IllegalArgumentException("Unknown detector");
		}

		switch( controlPanel.selectedDescriptor ) {
			case 0:descriptor = FactoryDescribeRegionPoint.surfStable(null, imageType); break;
			case 1:descriptor = FactoryDescribeRegionPoint.surfColorStable(null, ImageType.pl(3, imageType)); break;
			case 2:descriptor = FactoryDescribeRegionPoint.sift(null,null, imageType); break;
			case 3:descriptor = FactoryDescribeRegionPoint.brief(new ConfigBrief(true), imageType); break;
			case 4:descriptor = FactoryDescribeRegionPoint.brief(new ConfigBrief(false), imageType); break;
			case 5:descriptor = FactoryDescribeRegionPoint.pixel(11, 11, imageType); break;
			case 6:descriptor = FactoryDescribeRegionPoint.pixelNCC(11, 11, imageType); break;
			default:
				throw new IllegalArgumentException("Unknown descriptor");
		}

		SwingUtilities.invokeLater(()-> {
			controlPanel.setFeatureType(descriptor.getDescriptionType());
		});

		// estimate orientation using this once since it is fast and accurate
		Class integralType = GIntegralImageOps.getIntegralType(imageType);
		OrientationIntegral orientationII = FactoryOrientationAlgs.sliding_ii(null, integralType);
		orientation = FactoryOrientation.convertImage(orientationII,imageType);
	}

	@Override
	protected void openFileMenuBar() {
		String[] files = BoofSwingUtil.openImageSetChooser(window, OpenImageSetDialog.Mode.EXACTLY,2);
		if( files == null )
			return;
		BoofSwingUtil.invokeNowOrLater(()->openImageSet(files));
	}

	@Override
	protected void handleInputChange( int source , InputMethod method , final int width , final int height ) {
		switch( source ) {
			case 0:
				buffLeft = ConvertBufferedImage.checkDeclare(width,height,buffLeft,BufferedImage.TYPE_INT_RGB);
				SwingUtilities.invokeLater(()->{
					scorePanel.setPreferredSize(width,height,width,height);
					controlPanel.setImageSize(width,height);
				});
				break;

			case 1:
				buffRight = ConvertBufferedImage.checkDeclare(width,height,buffRight,BufferedImage.TYPE_INT_RGB);
				break;
		}
	}

	@Override
	public void processImage(int sourceID, long frameID, BufferedImage bufferedIn, ImageBase input) {
		switch( sourceID ) {
			case 0:
				imageLeft.setTo((T)input);
				buffLeft.createGraphics().drawImage(bufferedIn,0,0,null);
				break;

			case 1:
				imageRight.setTo((T)input);
				buffRight.createGraphics().drawImage(bufferedIn,0,0,null);

				BoofSwingUtil.invokeNowOrLater((()-> scorePanel.setImages(buffLeft,buffRight)));
				processImage();
				break;
		}
	}

	/**
	 * Extracts image information and then passes that info onto scorePanel for display.  Data is not
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
			scorePanel.setScorer(controlPanel.getSelected());
			scorePanel.setLocation(leftPts, rightPts, leftDesc, rightDesc);
			repaint();
		});
	}

	/**
	 * Detects the locations of the features in the image and extracts descriptions of each of
	 * the features.
	 */
	private void extractImageFeatures(final ProgressMonitor progressMonitor, final int progress,
									  T image,
									  List<TupleDesc> descs, List<Point2D_F64> locs) {
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
				if (descriptor.requiresOrientation()) {
					orientation.setObjectRadius(radius);
					yaw = orientation.compute(pt.x, pt.y);
				}

				TupleDesc d = descriptor.createDescription();
				if ( descriptor.process(pt.x, pt.y, yaw, radius, d) ) {
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
				if (descriptor.requiresOrientation()) {
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

	public class VisualizeScorePanel extends StandardAlgConfigPanel implements ActionListener {
		JLabel labelSize = new JLabel();

		JComboBox<String> comboDetect;
		JComboBox<String> comboDescribe;

		// selects which image to view
		JComboBox scoreTypes = new JComboBox();

		Class type;
		ScoreAssociation selected;


		public int selectedDetector;
		public int selectedDescriptor;

		public VisualizeScorePanel() {
			setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
			setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

			comboDetect = combo(selectedDetector,"Fast Hessian","SIFT","Shi-Tomasi");
			comboDescribe = combo(selectedDescriptor,"SURF-S","SURF-S Color","SIFT","BRIEF","BRIEFSO","Pixel 11x11","NCC 11x11");

			scoreTypes.addActionListener(this);
			scoreTypes.setMaximumSize(scoreTypes.getPreferredSize());
			add(labelSize);
			addLabeled(scoreTypes, "Score: ", this);
		}

		public void setImageSize( int width , int height ) {
			labelSize.setText(width+" x "+height);
		}

		public void setFeatureType( Class type ) {
			if( this.type == type )
				return;

			this.type = type;

			scoreTypes.removeActionListener(this);
			scoreTypes.removeAllItems();
			if( type == TupleDesc_B.class ) {
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateHamming_B(),"Hamming"));
			} else if( type == NccFeature.class ) {
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateNccFeature(),"NCC"));
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclidean_F64(),"Euclidean"));
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclideanSq_F64(),"Euclidean2"));
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateSad_F64(),"SAD"));
			} else if( TupleDesc_F64.class.isAssignableFrom(type) ) {
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclidean_F64(),"Euclidean"));
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclideanSq_F64(),"Euclidean2"));
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateSad_F64(),"SAD"));
			} else if( type == TupleDesc_F32.class ) {
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclideanSq_F32(),"Euclidean2"));
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateSad_F32(),"SAD"));
			} else if( type == TupleDesc_U8.class ) {
				scoreTypes.addItem(new ScoreItem(new ScoreAssociateSad_U8(),"SAD"));
			} else {
				throw new RuntimeException("Unknown description type "+type.getSimpleName());
			}
			selected = ((ScoreItem)scoreTypes.getSelectedItem()).assoc;
			scoreTypes.revalidate();
			scoreTypes.addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			ScoreItem item = (ScoreItem)scoreTypes.getSelectedItem();
			selected = item.assoc;

			scorePanel.setScorer(controlPanel.getSelected());
		}

		public ScoreAssociation getSelected() {
			return selected;
		}
	}

	private static class ScoreItem
	{
		String name;
		ScoreAssociation assoc;

		private ScoreItem(ScoreAssociation assoc, String name) {
			this.assoc = assoc;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
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
			VisualizeAssociationScoreApp app = new VisualizeAssociationScoreApp(examples,GrayF32.class);

			// Processing time takes a bit so don't open right away
			app.openExample(examples.get(0));
			app.display("Visualize Association Score");
		});
	}
}
