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

package boofcv.demonstrations.sfm.d3;

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
import boofcv.gui.DemonstrationBase2;
import boofcv.gui.d3.Polygon3DSequenceViewer;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.struct.calib.VisualDepthParameters;
import boofcv.struct.distort.DoNothing2Transform2_F32;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.*;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.RowMatrix_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Visualizes data from
 *
 * @author Peter Abeles
 */
// TODO add ability to select algorithm
	// TODO switch between depth and 3D view
	// TODO compute FPS and show it
	// TODO display status correctly
	// TODO add direct method
	// TODO custom visualization for direct method
	// TODO add algorithm specific tuning parameters?
public class VisualizeDepthVisualOdometryApp<I extends ImageGray<I>>
		extends DemonstrationBase2 implements VisualOdometryPanel.Listener
{

	VisualOdometryPanel guiInfo;

	JPanel dataPanels = new JPanel();
	ImagePanel guiLeft;
	ImagePanel guiDepth;
	Polygon3DSequenceViewer guiCam3D;

	BufferedImage renderedDepth;

	DepthVisualOdometry<I,GrayU16> alg;

	boolean noFault;

	boolean showTracks;
	boolean showInliers;

	int numFaults;
	int numTracks;
	int numInliers;
	int whichAlg;

	I imageRGB;
	GrayU16 imageDepth;

	BufferedImage bufferedRGB;

	protected VisualDepthParameters config;

	public VisualizeDepthVisualOdometryApp(List<PathLabel> examples , Class<I> imageType) {
		super(true,false,examples, ImageType.single(imageType), ImageType.single(GrayU16.class));

//		addAlgorithm(0, "Single P3P : KLT", 0);
//		addAlgorithm(0, "Single P3P : ST-BRIEF", 1);
//		addAlgorithm(0, "Single P3P : ST-SURF-KLT", 2);

		alg = createVisualOdometry(whichAlg);

		guiInfo = new VisualOdometryPanel(VisualOdometryPanel.Type.DEPTH);
		guiLeft = new ImagePanel();
		guiDepth = new ImagePanel();
		guiCam3D = new Polygon3DSequenceViewer();

		dataPanels.setLayout(new BoxLayout(dataPanels,BoxLayout.X_AXIS));
		dataPanels.add(guiLeft);
		dataPanels.add(guiDepth);

		add(guiInfo, BorderLayout.WEST);
		add(dataPanels, BorderLayout.CENTER);

//		guiLeft.addMouseListener(this);
		guiInfo.setListener(this);
	}

	@Override
	public void openFile(File file) {
		inputFilePath = file.getPath();

		Reader r = media.openFile(file.getPath());
		BufferedReader in = new BufferedReader(r);
		try {
			String path = file.getParent();

			String lineConfig = in.readLine();
			String line1 = in.readLine();
			String line2 = in.readLine();

			// adjust for relative paths
			if( lineConfig.charAt(0) != '/' )
				lineConfig = path+"/"+lineConfig;
			if( line1.charAt(0) != '/' )
				line1 = path+"/"+line1;
			if( line2.charAt(0) != '/' )
				line2 = path+"/"+line2;

			config = UtilIO.loadXML(media.openFile(lineConfig));
			openVideo(line1,line2);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reprocessInput() {
		openFile(new File(inputFilePath));
	}

	@Override
	protected void handleInputChange(int source, InputMethod method, int width, int height) {
		if( source != 0 )
			return;

		numFaults = 0;
		alg = createVisualOdometry(whichAlg);
		alg.setCalibration(config.visualParam,new DoNothing2Transform2_F32());

		guiInfo.reset();

//		handleRunningStatus(2);

		RowMatrix_F64 K = PerspectiveOps.calibrationMatrix(config.visualParam,(RowMatrix_F64)null);
		guiCam3D.init();
		guiCam3D.setK(K);
		guiCam3D.setStepSize(0.05);
		guiCam3D.setPreferredSize(new Dimension(config.visualParam.width, config.visualParam.height));
		guiCam3D.setMaximumSize(guiCam3D.getPreferredSize());

		dataPanels.setPreferredSize(new Dimension(width*2+10, height));
		dataPanels.setMaximumSize(dataPanels.getPreferredSize());
	}

	private void drawFeatures(AccessPointTracks3D tracker , BufferedImage image )  {

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
	public void processImage(int sourceID, long frameID, BufferedImage buffered, ImageBase input) {
		if( sourceID == 0 ) {
			imageRGB = (I)input;
			bufferedRGB = buffered;
		} else if( sourceID == 1 ) {
			imageDepth = (GrayU16)input;

			noFault = alg.process(imageRGB,imageDepth);
			if( !noFault ) {
				alg.reset();
				guiCam3D.init();
			}

			updateGUI();
		}
	}

	protected void updateGUI() {
		final double fps = 10; // TODO write thsi for real
		if( !noFault) {
			numFaults++;
			return;
		}

		showTracks = guiInfo.isShowAll();
		showInliers = guiInfo.isShowInliers();

		if( renderedDepth == null ) {
			renderedDepth = new BufferedImage(imageDepth.width,imageDepth.height,BufferedImage.TYPE_INT_RGB);
		}

		drawFeatures((AccessPointTracks3D)alg,bufferedRGB);

		final Se3_F64 leftToWorld = alg.getCameraToWorld().copy();

		// TODO magic value from kinect.  Add to config file?
		VisualizeImageData.disparity(imageDepth, renderedDepth, 0, 10000, 0);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				guiLeft.setBufferedImage(bufferedRGB);
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
	}


	private DepthVisualOdometry<I,GrayU16> createVisualOdometry(int whichAlg ) {

		Class imageType = getImageType(0).getImageClass();
		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DepthSparse3D<GrayU16> sparseDepth = new DepthSparse3D.I<>(1e-3);

		PkltConfig pkltConfig = new PkltConfig();
		pkltConfig.templateRadius = 3;
		pkltConfig.pyramidScaling = new int[]{1,2,4,8};

		if( whichAlg == 0 ) {
			ConfigGeneralDetector configDetector = new ConfigGeneralDetector(600,3,1);

			PointTrackerTwoPass<I> tracker = FactoryPointTrackerTwoPass.klt(pkltConfig, configDetector,
					imageType, derivType);

			return FactoryVisualOdometry.
					depthDepthPnP(1.5, 120, 2, 200, 50, false, sparseDepth, tracker, imageType, GrayU16.class);
		} else if( whichAlg == 1 ) {

			ConfigGeneralDetector configExtract = new ConfigGeneralDetector(600,3,1);

			GeneralFeatureDetector detector = FactoryPointTracker.createShiTomasi(configExtract, derivType);
			DescribeRegionPoint describe = FactoryDescribeRegionPoint.brief(null,imageType);

			ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

			AssociateDescription2D<TupleDesc_B> associate =
					new AssociateDescTo2D<>(FactoryAssociation.greedy(score, 150, true));

			PointTrackerTwoPass tracker = FactoryPointTrackerTwoPass.dda(detector, describe, associate, null, 1, imageType);

			return FactoryVisualOdometry.
					depthDepthPnP(1.5, 80, 3, 200, 50, false, sparseDepth, tracker, imageType, GrayU16.class);
		} else if( whichAlg == 2 ) {
			PointTracker<I> tracker = FactoryPointTracker.
					combined_ST_SURF_KLT(new ConfigGeneralDetector(600, 3, 1),
							pkltConfig, 50, null, null, imageType, derivType);

			PointTrackerTwoPass<I> twopass = new PointTrackerToTwoPass<>(tracker);

			return FactoryVisualOdometry.
					depthDepthPnP(1.5, 120, 3, 200, 50, false, sparseDepth, twopass, imageType, GrayU16.class);
		} else {
			throw new RuntimeException("Unknown selection");
		}
	}

//	@Override
//	protected void handleRunningStatus(int status) {
//		final String text;
//		final Color color;
//
//		switch( status ) {
//			case 0:
//				text = "RUNNING";
//				color = Color.BLACK;
//				break;
//
//			case 1:
//				text = "PAUSED";
//				color = Color.RED;
//				break;
//
//			case 2:
//				text = "FINISHED";
//				color = Color.RED;
//				break;
//
//			default:
//				text = "UNKNOWN";
//				color = Color.BLUE;
//		}
//
//		SwingUtilities.invokeLater(new Runnable() {
//			public void run() {
//				guiInfo.setStatus(text,color);
//			}});
//	}

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

		Class type = GrayF32.class;
//		Class type = GrayU8.class;

		List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Circle", UtilIO.pathExample("kinect/circle/config.txt")));
		inputs.add(new PathLabel("Hallway", UtilIO.pathExample("kinect/straight/config.txt")));

		VisualizeDepthVisualOdometryApp app = new VisualizeDepthVisualOdometryApp(inputs,type);

		app.openFile(new File(inputs.get(0).getPath()));
		app.waitUntilInputSizeIsKnown();

		ShowImages.showWindow(app, "Depth Visual Odometry",true);
	}
}
