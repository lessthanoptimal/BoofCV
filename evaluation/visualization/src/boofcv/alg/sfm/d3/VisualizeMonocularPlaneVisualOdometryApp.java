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

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.d3.MonocularPlaneVisualOdometry;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.VisualizeApp;
import boofcv.gui.d2.PlaneView2D;
import boofcv.gui.d3.Polygon3DSequenceViewer;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.calib.MonoPlaneParameters;
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
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class VisualizeMonocularPlaneVisualOdometryApp<I extends ImageSingleBand>
		extends VideoProcessAppBase<I> implements VisualizeApp, VisualOdometryPanel.Listener
{

	protected MonoPlaneParameters config;
	VisualOdometryPanel guiInfo;

	ImagePanel guiLeft;
	Polygon3DSequenceViewer guiCam3D;
	PlaneView2D gui2D;

	Class<I> imageClass;

	MonocularPlaneVisualOdometry<I> alg;

	boolean hasProcessedImage = false;
	boolean noFault;

	boolean showTracks;
	boolean showInliers;

	int numFaults;
	int numTracks;
	int numInliers;
	int whichAlg;

	public VisualizeMonocularPlaneVisualOdometryApp(Class<I> imageClass) {
		super(1, imageClass);
		this.imageClass = imageClass;

		addAlgorithm(0, "Plane-Infinity : KLT", 0);
		addAlgorithm(0, "Overhead : KLT", 1);

		guiInfo = new VisualOdometryPanel(VisualOdometryPanel.Type.MONO_PLANE);
		guiLeft = new ImagePanel();
		guiCam3D = new Polygon3DSequenceViewer();
		gui2D = new PlaneView2D(0.1);

		add(guiInfo, BorderLayout.WEST);
		add(gui2D, BorderLayout.EAST);
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
	public void changeInput(String name, int index) {
		stopWorker();
		Reader r = media.openFile(inputRefs.get(index).getPath());
		BufferedReader in = new BufferedReader(r);
		try {
			String path = new File(inputRefs.get(index).getPath()).getParent();

			String lineConfig = in.readLine();
			String line1 = in.readLine();

			// adjust for relative paths
			if( lineConfig.charAt(0) != '/' )
				lineConfig = path+"/"+lineConfig;
			if( line1.charAt(0) != '/' )
				line1 = path+"/"+line1;

			config = UtilIO.loadXML(media.openFile(lineConfig));
			SimpleImageSequence<I> video = media.openVideo(line1, imageType);

			process(video);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void process(SimpleImageSequence<I> sequence ) {
		// stop the image processing code
		stopWorker();

		this.sequence = sequence;
		sequence.setLoop(false);

		// start everything up and resume processing
		doRefreshAll();
	}

	@Override
	protected void updateAlg(I frame, BufferedImage buffImage ) {
		if( config.intrinsic.width != frame.width || config.intrinsic.height != frame.height )
			throw new IllegalArgumentException("Miss match between calibration and actual image size");

		noFault = alg.process(frame);
		if( !noFault ) {
			alg.reset();
			guiCam3D.init();
		}
	}

	@Override
	protected void updateAlgGUI(I frame1, final BufferedImage buffImage1,  final double fps) {
		if( !noFault) {
			numFaults++;
			return;
		}

		showTracks = guiInfo.isShowAll();
		showInliers = guiInfo.isShowInliers();

		if( alg instanceof AccessPointTracks3D)
			drawFeatures((AccessPointTracks3D)alg,buffImage1);

		final Se3_F64 leftToWorld = alg.getCameraToWorld().copy();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				guiLeft.setBufferedImage(buffImage1);
				guiLeft.autoSetPreferredSize();
				guiLeft.repaint();

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

		gui2D.addPoint(leftToWorld.T.x,leftToWorld.T.z);
		gui2D.repaint();

		hasProcessedImage = true;
	}


	@Override
	public void refreshAll(Object[] cookies) {

		numFaults = 0;
		if( cookies != null )
			whichAlg = (Integer)cookies[0];
		alg = createVisualOdometry(whichAlg);
		alg.setCalibration(config);

		guiInfo.reset();
		gui2D.reset();

		handleRunningStatus(2);

		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(config.intrinsic,null);
		guiCam3D.init();
		guiCam3D.setK(K);
		guiCam3D.setStepSize(1);
		guiCam3D.setPreferredSize(new Dimension(config.intrinsic.width, config.intrinsic.height));
		guiCam3D.setMaximumSize(guiCam3D.getPreferredSize());
		gui2D.setPreferredSize(new Dimension(config.intrinsic.width, config.intrinsic.height));
		gui2D.setMaximumSize(gui2D.getPreferredSize());
		startWorkerThread();
	}

	private MonocularPlaneVisualOdometry<I> createVisualOdometry( int whichAlg ) {

		Class derivType = GImageDerivativeOps.getDerivativeType(imageClass);


		if( whichAlg == 0 ) {
			PkltConfig config = new PkltConfig();
			config.pyramidScaling = new int[]{1,2,4,8};
			config.templateRadius = 3;
			ConfigGeneralDetector configDetector = new ConfigGeneralDetector(600,3,1);

			PointTracker<I> tracker = FactoryPointTracker.klt(config, configDetector,imageClass,derivType);

			return FactoryVisualOdometry.monoPlaneInfinity(75,2,1.5,200, tracker, imageType);
		} else if( whichAlg == 1 ) {
			PkltConfig config = new PkltConfig();
			config.pyramidScaling = new int[]{1,2,4,8};
			config.templateRadius = 3;
			ConfigGeneralDetector configDetector = new ConfigGeneralDetector(600,3,1);

			PointTracker<I> tracker = FactoryPointTracker.klt(config, configDetector,imageClass,derivType);

			double cellSize = 0.06;
			double inlierGroundTol = 1.5;

			return FactoryVisualOdometry.monoPlaneOverhead(cellSize,25,0.7,
					inlierGroundTol,300,2,100,0.5,0.6, tracker, imageType);
		}  else {
			throw new RuntimeException("Unknown selection");
		}
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {

		stopWorker();

		whichAlg = (Integer)cookie;

		sequence.reset();

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
					gui2D.setPreferredSize(guiCam3D.getPreferredSize());
					remove(guiCam3D);
					add(gui2D, BorderLayout.EAST);
				} else {
					guiCam3D.setPreferredSize(gui2D.getPreferredSize());
					remove(gui2D);
					add(guiCam3D,BorderLayout.EAST);
				}
				revalidate();
				repaint();
			}});
	}

	public static void main( String args[] ) throws FileNotFoundException {

		Class type = ImageFloat32.class;
//		Class type = ImageUInt8.class;

		VisualizeMonocularPlaneVisualOdometryApp app = new VisualizeMonocularPlaneVisualOdometryApp(type);

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("Simulation", "../data/applet/vo/drc/config_plane.txt"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Monocular Plane Visual Odometry");
	}
}
