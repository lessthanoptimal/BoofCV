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
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.feature.tracker.PointTrackerToTwoPass;
import boofcv.abst.feature.tracker.PointTrackerTwoPass;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.DepthVisualOdometry;
import boofcv.alg.distort.DoNothingPixelTransform_F32;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.feature.tracker.FactoryPointTrackerTwoPass;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.DepthVideoAppBase;
import boofcv.gui.VisualizeApp;
import boofcv.gui.d3.Polygon3DSequenceViewer;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt16;
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
public class VisualizeDepthVisualOdometryApp<I extends ImageSingleBand>
		extends DepthVideoAppBase<I,ImageUInt16> implements VisualizeApp, VisualOdometryPanel.Listener
{

	VisualOdometryPanel guiInfo;

	ImagePanel guiLeft;
	ImagePanel guiDepth;
	Polygon3DSequenceViewer guiCam3D;

	BufferedImage renderedDepth;

	DepthVisualOdometry<I,ImageUInt16> alg;

	boolean hasProcessedImage = false;
	boolean noFault;

	boolean showTracks;
	boolean showInliers;

	int numFaults;
	int numTracks;
	int numInliers;
	int whichAlg;

	public VisualizeDepthVisualOdometryApp(Class<I> imageType) {
		super(1, imageType, ImageUInt16.class);

		addAlgorithm(0, "Single P3P : KLT", 0);
		addAlgorithm(0, "Single P3P : ST-BRIEF", 1);
		addAlgorithm(0, "Single P3P : ST-SURF-KLT", 2);

		guiInfo = new VisualOdometryPanel(VisualOdometryPanel.Type.DEPTH);
		guiLeft = new ImagePanel();
		guiDepth = new ImagePanel();
		guiCam3D = new Polygon3DSequenceViewer();

		add(guiInfo, BorderLayout.WEST);
		add(guiDepth, BorderLayout.EAST);
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
	protected void process(SimpleImageSequence<I> sequence1, SimpleImageSequence<ImageUInt16> sequence2 ) {
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
	protected void updateAlg(I frame1, BufferedImage buffImage1, ImageUInt16 frame2, BufferedImage buffImage2) {
		if( config.visualParam.width != frame1.width || config.visualParam.height != frame1.height )
			throw new IllegalArgumentException("Miss match between calibration and actual image size");

		noFault = alg.process(frame1,frame2);
		if( !noFault ) {
			alg.reset();
			guiCam3D.init();
		}
	}

	@Override
	protected void updateAlgGUI(I frame1, final BufferedImage buffImage1,
								ImageUInt16 frame2, final BufferedImage buffImage2, final double fps) {
		if( !noFault) {
			numFaults++;
			return;
		}

		showTracks = guiInfo.isShowAll();
		showInliers = guiInfo.isShowInliers();

		if( renderedDepth == null ) {
			renderedDepth = new BufferedImage(frame2.width,frame2.height,BufferedImage.TYPE_INT_RGB);
		}

		drawFeatures((AccessPointTracks3D)alg,buffImage1);

		final Se3_F64 leftToWorld = alg.getCameraToWorld().copy();

		// TODO magic value from kinect.  Add to config file?
		VisualizeImageData.disparity(frame2, renderedDepth, 0, 10000, 0);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				guiLeft.setBufferedImage(buffImage1);
				guiDepth.setBufferedImage(renderedDepth);
				guiLeft.autoSetPreferredSize();
				guiDepth.autoSetPreferredSize();
				guiLeft.repaint();
				guiDepth.repaint();

				guiInfo.setCameraToWorld(leftToWorld);
				guiInfo.setNumFaults(numFaults);
				guiInfo.setNumTracks(numTracks);
				guiInfo.setNumInliers(numInliers);
				guiInfo.setFps(fps);
			}
		});


		double r = 0.15;

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
		alg = createVisualOdometry(whichAlg);
		alg.setCalibration(config.visualParam,new DoNothingPixelTransform_F32());

		guiInfo.reset();

		handleRunningStatus(2);

		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(config.visualParam,null);
		guiCam3D.init();
		guiCam3D.setK(K);
		guiCam3D.setStepSize(0.05);
		guiCam3D.setPreferredSize(new Dimension(config.visualParam.width, config.visualParam.height));
		guiCam3D.setMaximumSize(guiCam3D.getPreferredSize());
		startWorkerThread();
	}

	private DepthVisualOdometry<I,ImageUInt16> createVisualOdometry( int whichAlg ) {

		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DepthSparse3D<ImageUInt16> sparseDepth = new DepthSparse3D.I<ImageUInt16>(1e-3);

		PkltConfig pkltConfig = new PkltConfig();
		pkltConfig.templateRadius = 3;
		pkltConfig.pyramidScaling = new int[]{1,2,4,8};

		if( whichAlg == 0 ) {
			ConfigGeneralDetector configDetector = new ConfigGeneralDetector(600,3,1);

			PointTrackerTwoPass<I> tracker = FactoryPointTrackerTwoPass.klt(pkltConfig, configDetector,
					imageType, derivType);

			return FactoryVisualOdometry.
					depthDepthPnP(1.5, 120, 2, 200, 50, false, sparseDepth, tracker, imageType, ImageUInt16.class);
		} else if( whichAlg == 1 ) {

			ConfigGeneralDetector configExtract = new ConfigGeneralDetector(600,3,1);

			GeneralFeatureDetector detector = FactoryPointTracker.createShiTomasi(configExtract, derivType);
			DescribeRegionPoint describe = FactoryDescribeRegionPoint.brief(null,imageType);

			ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

			AssociateDescription2D<TupleDesc_B> associate =
					new AssociateDescTo2D<TupleDesc_B>(FactoryAssociation.greedy(score, 150, true));

			PointTrackerTwoPass tracker = FactoryPointTrackerTwoPass.dda(detector, describe, associate, null, 1, imageType);

			return FactoryVisualOdometry.
					depthDepthPnP(1.5, 80, 3, 200, 50, false, sparseDepth, tracker, imageType, ImageUInt16.class);
		} else if( whichAlg == 2 ) {
			PointTracker<I> tracker = FactoryPointTracker.
					combined_ST_SURF_KLT(new ConfigGeneralDetector(600, 3, 1),
							pkltConfig, 50, null, null, imageType, derivType);

			PointTrackerTwoPass<I> twopass = new PointTrackerToTwoPass<I>(tracker);

			return FactoryVisualOdometry.
					depthDepthPnP(1.5, 120, 3, 200, 50, false, sparseDepth, twopass, imageType, ImageUInt16.class);
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
					add(guiDepth, BorderLayout.EAST);
				} else {
					remove(guiDepth);
					add(guiCam3D,BorderLayout.EAST);
				}
				revalidate();
				repaint();
			}});
	}

	public static void main( String args[] ) throws FileNotFoundException {

		Class type = ImageFloat32.class;
//		Class type = ImageUInt8.class;

		VisualizeDepthVisualOdometryApp app = new VisualizeDepthVisualOdometryApp(type);

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("Circle", "../data/applet/kinect/circle/config.txt"));
		inputs.add(new PathLabel("Hallway", "../data/applet/kinect/straight/config.txt"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Depth Visual Odometry");
	}
}
