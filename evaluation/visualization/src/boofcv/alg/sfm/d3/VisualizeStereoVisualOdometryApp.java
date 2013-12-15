/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.d3;

import boofcv.abst.feature.associate.AssociateDescTo2D;
import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.ScoreAssociateHamming_B;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detdesc.DetectDescribeMulti;
import boofcv.abst.feature.detdesc.DetectDescribeMultiFusion;
import boofcv.abst.feature.detect.extract.ConfigExtract;
import boofcv.abst.feature.detect.extract.NonMaxSuppression;
import boofcv.abst.feature.detect.intensity.GeneralFeatureIntensity;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.detect.interest.DetectorInterestPointMulti;
import boofcv.abst.feature.detect.interest.GeneralToInterestMulti;
import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.feature.tracker.PointTrackerToTwoPass;
import boofcv.abst.feature.tracker.PointTrackerTwoPass;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.StereoVisualOdometry;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.detect.extract.FactoryFeatureExtractor;
import boofcv.factory.feature.detect.intensity.FactoryIntensityPoint;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.feature.tracker.FactoryPointTrackerTwoPass;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.StereoVideoAppBase;
import boofcv.gui.VisualizeApp;
import boofcv.gui.d3.Polygon3DSequenceViewer;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class VisualizeStereoVisualOdometryApp <I extends ImageSingleBand>
		extends StereoVideoAppBase<I> implements VisualizeApp, VisualOdometryPanel.Listener
{

	VisualOdometryPanel guiInfo;

	ImagePanel guiLeft;
	ImagePanel guiRight;
	Polygon3DSequenceViewer guiCam3D;

	StereoVisualOdometry<I> alg;

	boolean hasProcessedImage = false;
	boolean noFault;

	boolean showTracks;
	boolean showInliers;

	int numFaults;
	int numTracks;
	int numInliers;
	int whichAlg;

	public VisualizeStereoVisualOdometryApp( Class<I> imageType ) {
		super(1, imageType);

		addAlgorithm(0, "Single Depth : KLT", 0);
		addAlgorithm(0, "Single Depth : ST-BRIEF", 1);
		addAlgorithm(0, "Single Depth : ST-SURF-KLT", 2);
		addAlgorithm(0, "Dual Track : KLT + SURF", 3);
		addAlgorithm(0, "Quad Match : ST-BRIEF", 4);

		guiInfo = new VisualOdometryPanel(VisualOdometryPanel.Type.STEREO);
		guiLeft = new ImagePanel();
		guiRight = new ImagePanel();
		guiCam3D = new Polygon3DSequenceViewer();

		add(guiInfo, BorderLayout.WEST);
		add(guiRight, BorderLayout.EAST);
		setMainGUI(guiLeft);

		guiLeft.addMouseListener(this);
		guiInfo.setListener(this);
	}

	private void drawFeatures( AccessPointTracks3D tracker , BufferedImage image )  {

		numInliers=0;

		Graphics2D g2 = image.createGraphics();

		List<Point2D_F64> points = tracker.getAllTracks();

		if( points.size() == 0 )
			return;

		double ranges[] = new double[points.size() ];

		for( int i = 0; i < points.size(); i++ ) {
			ranges[i] = tracker.getTrackLocation(i).z;
		}
		Arrays.sort(ranges);
		double maxRange = ranges[(int)(ranges.length*0.8)];

		for( int i = 0; i < points.size(); i++ ) {
			Point2D_F64 pixel = points.get(i);

			if( showTracks && tracker.isNew(i) ) {
				VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,3,Color.GREEN);
				continue;
			}

			if( tracker.isInlier(i) ) {
				if( showInliers )
					VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,7,Color.BLUE,false);
				numInliers++;
			}

			if( !showTracks )
				continue;

			Point3D_F64 p3 = tracker.getTrackLocation(i);

			double r = p3.z/maxRange;
			if( r < 0 ) r = 0;
			else if( r > 1 ) r = 1;

			int color = (255 << 16) | ((int)(255*r) << 8);


			VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,3,new Color(color));
		}

		numTracks = points.size();

//		g2.setColor(Color.BLACK);
//		g2.fillRect(25,15,80,45);
//		g2.setColor(Color.CYAN);
//		g2.drawString("Total: " + numTracks, 30, 30);
//		g2.drawString("Inliers: "+numInliers,30,50);
	}


	@Override
	protected void process(SimpleImageSequence<I> sequence1, SimpleImageSequence<I> sequence2 ) {
		// stop the image processing code
		stopWorker();

		sequence1.setLoop(false);
		sequence2.setLoop(false);

		this.sequence1 = sequence1;
		this.sequence2 = sequence2;

		// start everything up and resume processing
		doRefreshAll();
	}

	@Override
	protected void updateAlg(I frame1, BufferedImage buffImage1, I frame2, BufferedImage buffImage2) {
		if( config.left.width != frame1.width || config.left.height != frame1.height )
			throw new IllegalArgumentException("Miss match between calibration and actual image size");

		noFault = alg.process(frame1,frame2);
	}

	@Override
	protected void updateAlgGUI(I frame1, final BufferedImage buffImage1,
								I frame2, final BufferedImage buffImage2, final double fps) {
		if( !noFault)
			numFaults++;

		showTracks = guiInfo.isShowAll();
		showInliers = guiInfo.isShowInliers();
		drawFeatures((AccessPointTracks3D)alg,buffImage1);

		final Se3_F64 leftToWorld = alg.getCameraToWorld().copy();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				guiLeft.setBufferedImage(buffImage1);
				guiRight.setBufferedImage(buffImage2);
				guiLeft.autoSetPreferredSize();
				guiRight.autoSetPreferredSize();
				guiLeft.repaint();
				guiRight.repaint();

				guiInfo.setCameraToWorld(leftToWorld);
				guiInfo.setNumFaults(numFaults);
				guiInfo.setNumTracks(numTracks);
				guiInfo.setNumInliers(numInliers);
				guiInfo.setFps(fps);
			}
		});


		double r = config.getBaseline();

		Point3D_F64 p1 = new Point3D_F64(-r,-r,0);
		Point3D_F64 p2 = new Point3D_F64(r,-r,0);
		Point3D_F64 p3 = new Point3D_F64(r,r,0);
		Point3D_F64 p4 = new Point3D_F64(-r,r,0);

		SePointOps_F64.transform(leftToWorld,p1,p1);
		SePointOps_F64.transform(leftToWorld,p2,p2);
		SePointOps_F64.transform(leftToWorld,p3,p3);
		SePointOps_F64.transform(leftToWorld,p4,p4);

		guiCam3D.add(p1,p2,p3,p4);
		guiCam3D.repaint();

		hasProcessedImage = true;
	}


	@Override
	public void refreshAll(Object[] cookies) {

		numFaults = 0;
		if( cookies != null )
			whichAlg = (Integer)cookies[0];
		alg = createStereoDepth(whichAlg);
		alg.setCalibration(config);

		guiInfo.reset();

		handleRunningStatus(2);

		IntrinsicParameters right = config.right;
		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(config.left,null);
		guiCam3D.init();
		guiCam3D.setK(K);
		guiCam3D.setStepSize(config.getBaseline());
		guiCam3D.setPreferredSize(new Dimension(right.width, right.height));
		guiCam3D.setMaximumSize(guiCam3D.getPreferredSize());
		startWorkerThread();
	}

	private StereoVisualOdometry<I> createStereoDepth( int whichAlg ) {

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		StereoDisparitySparse<I> disparity =
				FactoryStereoDisparity.regionSparseWta(2,150,3,3,30,-1,true,imageType);

		PkltConfig kltConfig = new PkltConfig();
		kltConfig.templateRadius = 3;
		kltConfig.pyramidScaling = new int[]{1, 2, 4, 8};

		if( whichAlg == 0 ) {
			ConfigGeneralDetector configDetector = new ConfigGeneralDetector(600,3,1);

			PointTrackerTwoPass<I> tracker = FactoryPointTrackerTwoPass.klt(kltConfig, configDetector,
					imageType,derivType);

			return FactoryVisualOdometry.stereoDepth(1.5,120,2,200,50, false, disparity, tracker, imageType);
		} else if( whichAlg == 1 ) {

			ConfigGeneralDetector configExtract = new ConfigGeneralDetector(600,3,1);

			GeneralFeatureDetector detector = FactoryPointTracker.createShiTomasi(configExtract, derivType);
			DescribeRegionPoint describe = FactoryDescribeRegionPoint.brief(null,imageType);

			ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

			AssociateDescription2D<TupleDesc_B> associate =
					new AssociateDescTo2D<TupleDesc_B>(FactoryAssociation.greedy(score, 150, true));

			PointTrackerTwoPass tracker = FactoryPointTrackerTwoPass.dda(detector, describe, associate, null, 1, imageType);

			return FactoryVisualOdometry.stereoDepth(1.5,80,3,200,50, false, disparity, tracker, imageType);
		} else if( whichAlg == 2 ) {
			PointTracker<I> tracker = FactoryPointTracker.
					combined_ST_SURF_KLT(new ConfigGeneralDetector(600, 3, 0),
							kltConfig, 50, null, null, imageType, derivType);

			PointTrackerTwoPass<I> twopass = new PointTrackerToTwoPass<I>(tracker);

			return FactoryVisualOdometry.stereoDepth(1.5,80,3,200,50, false, disparity, twopass, imageType);
		} else if( whichAlg == 3 ) {
			ConfigGeneralDetector configDetector = new ConfigGeneralDetector(600,3,1);

			PointTracker<I> trackerLeft = FactoryPointTracker.klt(kltConfig, configDetector,imageType,derivType);
			PointTracker<I> trackerRight = FactoryPointTracker.klt(kltConfig, configDetector,imageType,derivType);

			DescribeRegionPoint describe = FactoryDescribeRegionPoint.surfFast(null, imageType);

			return FactoryVisualOdometry.stereoDualTrackerPnP(90, 2, 1.5, 1.5, 200, 50,
					trackerLeft, trackerRight,describe, imageType);
		} else if( whichAlg == 4 ) {
//			GeneralFeatureIntensity intensity =
//					FactoryIntensityPoint.hessian(HessianBlobIntensity.Type.TRACE,imageType);
			GeneralFeatureIntensity intensity =
					FactoryIntensityPoint.shiTomasi(1,false,imageType);
			NonMaxSuppression nonmax = FactoryFeatureExtractor.nonmax(new ConfigExtract(2,50,0,true,false,true));
			GeneralFeatureDetector general = new GeneralFeatureDetector(intensity,nonmax);
			general.setMaxFeatures(600);
			DetectorInterestPointMulti detector = new GeneralToInterestMulti(general,2,imageType,derivType);
//			DescribeRegionPoint describe = FactoryDescribeRegionPoint.brief(new ConfigBrief(true),imageType);
//			DescribeRegionPoint describe = FactoryDescribeRegionPoint.pixelNCC(5,5,imageType);
			DescribeRegionPoint describe = FactoryDescribeRegionPoint.surfFast(null, imageType);
			DetectDescribeMulti detDescMulti =  new DetectDescribeMultiFusion(detector,null,describe);

			return FactoryVisualOdometry.stereoQuadPnP(1.5, 0.5 ,75, Double.MAX_VALUE, 300, 50, detDescMulti, imageType);
		} else {
			throw new RuntimeException("Unknown selection");
		}
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {

		stopWorker();

		whichAlg = (Integer)cookie;

		sequence1.reset();
		sequence2.reset();

		refreshAll(null);
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return hasProcessedImage;
	}

	@Override
	protected void handleRunningStatus(int status) {
		final String text;
		final Color color;

		switch( status ) {
			case 0:
				text = "RUNNING";
				color = Color.BLACK;
				break;

			case 1:
				text = "PAUSED";
				color = Color.RED;
				break;

			case 2:
				text = "FINISHED";
				color = Color.RED;
				break;

			default:
				text = "UNKNOWN";
				color = Color.BLUE;
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				guiInfo.setStatus(text,color);
			}});
	}

	@Override
	public void eventVoPanel(final int view) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if( view == 0 ) {
					remove(guiCam3D);
					add(guiRight, BorderLayout.EAST);
				} else {
					remove(guiRight);
					add(guiCam3D,BorderLayout.EAST);
				}
				revalidate();
				repaint();
			}});
	}

	public static void main( String args[] ) throws FileNotFoundException {

		Class type = ImageFloat32.class;
//		Class type = ImageUInt8.class;

		VisualizeStereoVisualOdometryApp app = new VisualizeStereoVisualOdometryApp(type);

//		app.setMediaManager(new XugglerMediaManager());

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("Inside", "../data/applet/vo/library/config.txt"));
		inputs.add(new PathLabel("Outside", "../data/applet/vo/backyard/config.txt"));
		inputs.add(new PathLabel("Urban", "../data/applet/vo/rockville/config.txt"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Stereo Visual Odometry");
	}
}
