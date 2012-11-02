/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.sfm.StereoVisualOdometry;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.factory.sfm.FactoryVisualOdometry;
import boofcv.gui.StereoVideoAppBase;
import boofcv.gui.VisualizeApp;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImageGridPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.xuggler.XugglerMediaManager;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

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
		extends StereoVideoAppBase<I> implements VisualizeApp
{

	ImageGridPanel guiImages;
	VisualOdometryPanel guiInfo;

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

		addAlgorithm(0, "KLT - Depth P3P", 0);
		addAlgorithm(0, "BRIEF - Depth P3P", 1);


		guiInfo = new VisualOdometryPanel();
		guiImages = new ImageGridPanel(1,2);

		add(guiInfo, BorderLayout.WEST);
		setMainGUI(guiImages);

		guiImages.addMouseListener(this);
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
		noFault = alg.process(frame1,frame2);
	}

	@Override
	protected void updateAlgGUI(I frame1, BufferedImage buffImage1,
								I frame2, BufferedImage buffImage2, final double fps) {
		if( !noFault)
			numFaults++;

		final Se3_F64 leftToWorld = alg.getLeftToWorld().copy();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				guiInfo.setCameraToWorld(leftToWorld);
				guiInfo.setNumFaults(numFaults);
				guiInfo.setNumTracks(numTracks);
				guiInfo.setNumInliers(numInliers);
				guiInfo.setFps(fps);
			}
		});

		showTracks = guiInfo.isShowAll();
		showInliers = guiInfo.isShowInliers();
		drawFeatures((AccessPointTracks3D)alg,buffImage1);

		guiImages.setImage(0,0,buffImage1);
		guiImages.setImage(0,1,buffImage2);

		guiImages.autoSetPreferredSize();

		hasProcessedImage = true;
	}


	@Override
	public void refreshAll(Object[] cookies) {

		numFaults = 0;
		alg = createStereoDepth(whichAlg==0);
		alg.setCalibration(config);

		guiInfo.reset();

		handleRunningStatus(2);

		startWorkerThread();
	}

	private StereoVisualOdometry<I> createStereoDepth( boolean useKlt ) {
		ImagePointTracker<I> tracker;

		if( useKlt ) {
			Class derivType = GImageDerivativeOps.getDerivativeType(imageType);
			tracker = FactoryPointSequentialTracker.klt(600,new int[]{1,2,4,8},3,3,2,imageType,derivType);
		} else {
//			tracker = FactoryPointSequentialTracker.dda_FH_SURF(600, 200, 1, imageType);
			tracker = FactoryPointSequentialTracker.dda_ShiTomasi_BRIEF(400, 200, 2, 0, imageType);
		}

		StereoDisparitySparse<I> disparity =
				FactoryStereoDisparity.regionSparseWta(0,150,3,3,30,-1,true,imageType);

		return FactoryVisualOdometry.stereoDepth(120, 4, 1.5, tracker, disparity, 0, imageType);
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

	public static void main( String args[] ) throws FileNotFoundException {

		Class type = ImageFloat32.class;
//		Class type = ImageUInt8.class;

		VisualizeStereoVisualOdometryApp app = new VisualizeStereoVisualOdometryApp(type);

		app.setMediaManager(new XugglerMediaManager());

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("Outdoors", "/home/pja/temp/config.txt"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Stereo Visual Odometry");
	}
}
