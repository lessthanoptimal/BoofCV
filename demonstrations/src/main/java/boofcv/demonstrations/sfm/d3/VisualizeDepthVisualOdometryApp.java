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

package boofcv.demonstrations.sfm.d3;

import boofcv.abst.feature.associate.AssociateDescTo2D;
import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.abst.feature.associate.ScoreAssociateHamming_B;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.DepthVisualOdometry;
import boofcv.abst.sfm.d3.PyramidDirectColorDepth_to_DepthVisualOdometry;
import boofcv.abst.tracker.ConfigTrackerDda;
import boofcv.abst.tracker.PointTracker;
import boofcv.abst.tracker.PointTrackerToTwoPass;
import boofcv.abst.tracker.PointTrackerTwoPass;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.alg.tracker.klt.ConfigPKlt;
import boofcv.factory.feature.associate.ConfigAssociateGreedy;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.factory.feature.describe.FactoryDescribeRegionPoint;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.factory.tracker.FactoryPointTracker;
import boofcv.factory.tracker.FactoryPointTrackerTwoPass;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.d3.Polygon3DSequenceViewer;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.struct.calib.VisualDepthParameters;
import boofcv.struct.distort.DoNothing2Transform2_F32;
import boofcv.struct.feature.TupleDesc_B;
import boofcv.struct.image.*;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Visualizes data from
 *
 *
 * @author Peter Abeles
 */

// TODO custom visualization for direct method
// TODO   - Key Fraction - Diversity - Key Frames
// TODO   - Show image unwarping
// TODO   - Show depth of pixel that is in view
// TODO add algorithm specific tuning parameters?
// TODO Add feature point cloud to VO view?
// TODO Click on polygon to get the frame it was generated from?
// TODO Add log to file option for location and 3D cloud
public class VisualizeDepthVisualOdometryApp
		extends DemonstrationBase implements VisualOdometryPanel2.Listener, ActionListener
{
	VisualOdometryPanel2 statusPanel;
	VisualOdometryAlgorithmPanel algorithmPanel;
	VisualOdometryFeatureTrackerPanel featurePanel;
	VisualOdometryDirectPanel directPanel;

	JPanel mainPanel = new JPanel();
	JSplitPane viewPanel;
	ImagePanel guiLeft;
	ImagePanel guiDepth;
	Polygon3DSequenceViewer guiCam3D;

	BufferedImage renderedDepth;

	DepthVisualOdometry alg;

	boolean noFault;

	boolean showTracks;
	boolean showInliers;

	int numFaults;
	int numTracks;
	int numInliers;
	double fractionInBounds;
	int whichAlg=-1;

	AlgType algType = AlgType.UNKNOWN;

	double fps;
	int frameNumber;

	ImageBase imageRGB;
	GrayU16 imageDepth;

	BufferedImage bufferedRGB;

	protected VisualDepthParameters config;
	JComboBox selectAlgorithm;

	public VisualizeDepthVisualOdometryApp(List<PathLabel> examples ) {
		super(true,false,examples);


		selectAlgorithm = new JComboBox();
		selectAlgorithm.addItem( "Single P3P : KLT" );
		selectAlgorithm.addItem( "Single P3P : ST-BRIEF" );
		selectAlgorithm.addItem( "Single P3P : ST-SURF-KLT" );
		selectAlgorithm.addItem( "Direct" );

		selectAlgorithm.addActionListener(this);
		selectAlgorithm.setMaximumSize(selectAlgorithm.getPreferredSize());
		menuBar.add(selectAlgorithm);

		statusPanel = new VisualOdometryPanel2(VisualOdometryPanel2.Type.DEPTH);
		guiLeft = new ImagePanel();
		guiDepth = new ImagePanel();
		guiCam3D = new Polygon3DSequenceViewer();

		viewPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
				guiLeft, guiDepth);

		algorithmPanel = new VisualOdometryAlgorithmPanel();
		featurePanel = new VisualOdometryFeatureTrackerPanel();
		directPanel = new VisualOdometryDirectPanel();

		mainPanel.setLayout(new BorderLayout());
		mainPanel.add(viewPanel, BorderLayout.CENTER);
		mainPanel.add(algorithmPanel, BorderLayout.NORTH);

		add(statusPanel, BorderLayout.WEST);
		add(mainPanel, BorderLayout.CENTER);

//		guiLeft.addMouseListener(this);
		statusPanel.setListener(this);

		changeSelectedAlgortihm(0);
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

			config = CalibrationIO.load(media.openFile(lineConfig));
			openVideo(false,line1,line2);
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

		fps = -1;
		numFaults = 0;
		frameNumber = 0;
		alg.reset();
		alg.setCalibration(config.visualParam,new DoNothing2Transform2_F32());

		statusPanel.reset();

		handleRunningStatus(Status.RUNNING);

		guiCam3D.init();
		guiCam3D.setFocalLength(300);
		guiCam3D.setStepSize(0.05);
		guiCam3D.setPreferredSize(new Dimension(config.visualParam.width, config.visualParam.height));

		viewPanel.setPreferredSize(new Dimension(width*2+20, height));
		viewPanel.setDividerLocation(width);
		viewPanel.setMaximumSize(viewPanel.getPreferredSize());
	}

	@Override
	protected void handleInputClose(int source) {
		if( source != 0 )
			return;
		handleRunningStatus(Status.FINISHED);
	}

	private void drawFeatures(AccessPointTracks3D tracker , BufferedImage image )  {

		numInliers=0;

		Graphics2D g2 = image.createGraphics();

		List<Point2D_F64> points = tracker.getAllTracks(null);

		if( points.size() == 0 )
			return;

		double[] ranges = new double[points.size() ];

		Point3D_F64 world = new Point3D_F64();
		for( int i = 0; i < points.size(); i++ ) {
			tracker.getTrackWorld3D(i,world);
			ranges[i] = world.z;
		}
		Arrays.sort(ranges);
		double maxRange = ranges[(int)(ranges.length*0.8)];

		for( int i = 0; i < points.size(); i++ ) {
			Point2D_F64 pixel = points.get(i);

			if( showTracks && tracker.isTrackNew(i) ) {
				VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,3,Color.GREEN);
				continue;
			}

			if( tracker.isTrackInlier(i) ) {
				if( showInliers )
					VisualizeFeatures.drawPoint(g2,(int)pixel.x,(int)pixel.y,7,Color.BLUE,false);
				numInliers++;
			}

			if( !showTracks )
				continue;

			tracker.getTrackWorld3D(i,world);
			double r = world.z/maxRange;
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
			imageRGB = input;
			bufferedRGB = buffered;
		} else if( sourceID == 1 ) {
			imageDepth = (GrayU16)input;
			frameNumber++;

			long before = System.nanoTime();
			noFault = alg.process(imageRGB,imageDepth);
			long after = System.nanoTime();
			double elapsed = (after-before)/1e9;

			if( fps < 0 ) {
				fps = 1.0/elapsed;
			} else {
				double lambda = 0.95;
				fps = lambda*fps + (1.0-lambda)*(1.0/elapsed);
			}

			if( !noFault ) {
				alg.reset();
				guiCam3D.init();
			}

			if( frameNumber-1 == statusPanel.getStopFrame()) {
				streamPaused = true;
			}

			updateGUI();
		}
	}

	protected void updateGUI() {
		if( !noFault) {
			numFaults++;
			return;
		}

		showTracks = statusPanel.isShowAll();
		showInliers = statusPanel.isShowInliers();

		if( renderedDepth == null ) {
			renderedDepth = new BufferedImage(imageDepth.width,imageDepth.height,BufferedImage.TYPE_INT_RGB);
		}

		switch( algType) {
			case FEATURE:
				drawFeatures((AccessPointTracks3D)alg,bufferedRGB);
			break;

			case DIRECT:
				fractionInBounds = ((PyramidDirectColorDepth_to_DepthVisualOdometry)alg).getFractionInBounds();
				break;
		}

		final Se3_F64 leftToWorld = ((Se3_F64)alg.getCameraToWorld()).copy();

		// TODO magic value from kinect.  Add to config file?
		VisualizeImageData.disparity(imageDepth, renderedDepth, 10000, 0);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				guiLeft.setImage(bufferedRGB);
				guiDepth.setImage(renderedDepth);
				guiLeft.autoSetPreferredSize();
				guiDepth.autoSetPreferredSize();
				guiLeft.repaint();
				guiDepth.repaint();

				statusPanel.setCameraToWorld(leftToWorld);
				statusPanel.setNumFaults(numFaults);

				statusPanel.setFps(fps);
				statusPanel.setFrameNumber(frameNumber-1);

				statusPanel.setPaused(streamPaused);

				switch( algType ) {
					case FEATURE: {
						featurePanel.setNumTracks(numTracks);
						featurePanel.setNumInliers(numInliers);
					} break;

					case DIRECT: {
						directPanel.setInBounds(fractionInBounds);
					} break;
				}
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


	private void changeSelectedAlgortihm(int whichAlg ) {

		this.whichAlg = whichAlg;
		AlgType prevAlgType = this.algType;


		Class imageType = GrayU8.class;
		Class derivType = GImageDerivativeOps.getDerivativeType(imageType);

		DepthSparse3D<GrayU16> sparseDepth = new DepthSparse3D.I<>(1e-3);

		ConfigPKlt configPKlt = new ConfigPKlt();
		configPKlt.templateRadius = 3;
		configPKlt.pyramidLevels = ConfigDiscreteLevels.levels(4);

		algType = AlgType.UNKNOWN;
		if (whichAlg == 0) {
			algType = AlgType.FEATURE;

			ConfigGeneralDetector configDetector = new ConfigGeneralDetector(600, 3, 1);

			PointTrackerTwoPass tracker = FactoryPointTrackerTwoPass.klt(configPKlt, configDetector,
					imageType, derivType);

			alg = FactoryVisualOdometry.
					depthDepthPnP(1.5, 120, 2, 200, 50, false, sparseDepth, tracker, imageType, GrayU16.class);
		} else if (whichAlg == 1) {
			algType = AlgType.FEATURE;

			ConfigGeneralDetector configExtract = new ConfigGeneralDetector(600, 3, 1);

			GeneralFeatureDetector detector = FactoryPointTracker.createShiTomasi(configExtract, derivType);
			DescribeRegionPoint describe = FactoryDescribeRegionPoint.brief(null, imageType);

			ScoreAssociateHamming_B score = new ScoreAssociateHamming_B();

			AssociateDescription2D<TupleDesc_B> associate =
					new AssociateDescTo2D<>(FactoryAssociation.greedy(new ConfigAssociateGreedy(true,150),score));

			PointTrackerTwoPass tracker = FactoryPointTrackerTwoPass.dda(detector, describe, associate, null, 1,
					new ConfigTrackerDda(), imageType);

			alg = FactoryVisualOdometry.
					depthDepthPnP(1.5, 80, 3, 200, 50, false, sparseDepth, tracker, imageType, GrayU16.class);
		} else if (whichAlg == 2) {
			algType = AlgType.FEATURE;

			PointTracker tracker = FactoryPointTracker.
					combined_ST_SURF_KLT(new ConfigGeneralDetector(600, 3, 1),
							configPKlt, 50, null, null, imageType, derivType);

			PointTrackerTwoPass twopass = new PointTrackerToTwoPass<>(tracker);

			alg = FactoryVisualOdometry.
					depthDepthPnP(1.5, 120, 3, 200, 50, false, sparseDepth, twopass, imageType, GrayU16.class);
		} else if (whichAlg == 3) {
			algType = AlgType.DIRECT;
			alg = FactoryVisualOdometry.
					depthDirect(sparseDepth, ImageType.pl(3,GrayF32.class), GrayU16.class);
		} else {
			throw new RuntimeException("Unknown selection");
		}

		if (algType != prevAlgType) {
			switch( prevAlgType ) {
				case FEATURE:
					mainPanel.remove(featurePanel);
					break;

				case DIRECT:
					mainPanel.remove(directPanel);
					break;

				default:
					mainPanel.remove(algorithmPanel);
					break;
			}

			switch (algType) {
				case FEATURE:
					mainPanel.add(featurePanel, BorderLayout.NORTH);
					break;
				case DIRECT:
					mainPanel.add(directPanel, BorderLayout.NORTH);
					break;
				default:
					mainPanel.add(algorithmPanel, BorderLayout.NORTH);
					break;
			}
			mainPanel.invalidate();
		}

		setImageTypes(alg.getVisualType(),ImageType.single(alg.getDepthType()));
	}

	protected void handleRunningStatus(Status status) {
		final String text;
		final Color color;

		switch( status ) {
			case RUNNING:
				color = Color.BLACK;
				break;

			case PAUSED:
				color = Color.RED;
				break;

			case FINISHED:
				color = Color.RED;
				break;

			default:
				color = Color.BLUE;
		}

		text = status.name();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				switch( algType ) {
					case FEATURE:
						featurePanel.setStatus(text,color);
						break;

					case DIRECT:
						directPanel.setStatus(text,color);
						break;

					default:
						algorithmPanel.setStatus(text,color);
						break;
				}
			}});
	}

	@Override
	public void eventVoPanel(final int view) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if( view == 0 ) {
					viewPanel.setRightComponent(guiDepth);
				} else {
					viewPanel.setRightComponent(guiCam3D);
				}
				viewPanel.revalidate();
				viewPanel.repaint();
			}});
	}

	@Override
	public void handlePausedToggle() {
		streamPaused = statusPanel.paused;
	}

	@Override
	public void handleStep() {
		if( streamPaused ) {
			streamPaused = false;
		}
		streamStepCounter = 1;
	}

	@Override
	protected void enterPausedState() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				statusPanel.setPaused(true);
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() != selectAlgorithm )
			return;

		int which = selectAlgorithm.getSelectedIndex();

		if( which == whichAlg )
			return;

		stopAllInputProcessing();
		changeSelectedAlgortihm(which);
		reprocessInput();
	}

	enum Status
	{
		RUNNING,
		PAUSED,
		FINISHED,
		UNKNOWN
	}

	enum AlgType
	{
		UNKNOWN,
		FEATURE,
		DIRECT
	}

	public static void main( String args[] ) throws FileNotFoundException {

		List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Circle", UtilIO.pathExample("kinect/circle/config.txt")));
		inputs.add(new PathLabel("Hallway", UtilIO.pathExample("kinect/straight/config.txt")));

		VisualizeDepthVisualOdometryApp app = new VisualizeDepthVisualOdometryApp(inputs);

		app.openFile(new File(inputs.get(0).getPath()));
		app.waitUntilInputSizeIsKnown();

		ShowImages.showWindow(app, "Depth Visual Odometry",true);
	}
}
