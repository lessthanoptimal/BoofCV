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

package boofcv.alg.sfm;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.factory.feature.tracker.FactoryPointSequentialTracker;
import boofcv.gui.VideoProcessAppBase;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.image.SimpleImageSequence;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class VisualizeMonoSeparated<I extends ImageSingleBand, D extends ImageSingleBand>
extends VideoProcessAppBase<I,D> {

	private final static int maxFeatures = 500;

	int inputWidth,inputHeight;

	ImagePointTracker<I> tracker;
	MonocularSeparatedMotion<I> alg;

	boolean processedImage = false;

	ImagePanel videoPanel = new ImagePanel();
	DisplayMonoPath pathGui = new DisplayMonoPath();

	int frameCount = 0;
	boolean updatedEstimate;

	public VisualizeMonoSeparated(Class<I> imageType, Class<D> derivType) {
		super(0,imageType);
		
		tracker = FactoryPointSequentialTracker.klt(maxFeatures,new int[]{1,2,4,8},5,3,2,imageType,derivType);
		                           
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add( pathGui,BorderLayout.EAST);
		p.add( videoPanel , BorderLayout.CENTER);
		pathGui.setPreferredSize(new Dimension(300,300));

		videoPanel.addMouseListener(this);
		setMainGUI(p);
	}
	
	public void configure( IntrinsicParameters cameraParam )  {
		PointTransform_F64 removeRadial = LensDistortionOps.transformRadialToNorm_F64(cameraParam);

//		WrapMonocularSeparatedMotion<I> w =
//				(WrapMonocularSeparatedMotion<I>)FactoryVisualOdometry.
//						monoSeparated(maxFeatures/2, 12, 1.2,  2.5*Math.PI/180.0, tracker, removeRadial);
//
//		alg = w.getAlg();
	}

	@Override
	protected void process(SimpleImageSequence<I> sequence) {
		if( !sequence.hasNext() )
			return;
		// stop the image processing code
		stopWorker();

		this.sequence = sequence;

		// save the input image dimension
		I input = sequence.next();
		inputWidth = input.width;
		inputHeight = input.height;

		// start everything up and resume processing
		doRefreshAll();
	}

	@Override
	protected void updateAlg(I frame, BufferedImage buffImage) {
		if( alg == null )
			return;
		System.out.println("*-*-*-*-*   FRAME "+frameCount+++" *-*-*-*-*");

		updatedEstimate = alg.process(frame);

	}

	@Override
	protected void updateAlgGUI(I frame, BufferedImage imageGUI, double fps) {

		Se3_F64 worldToCamera = alg.getWorldToKey();

		Se3_F64 cameraToWorld = worldToCamera.invert(null);
		pathGui.addLocation(cameraToWorld.getT());
		pathGui.repaint();

		if(updatedEstimate) {
			Vector3D_F64 T = cameraToWorld.getT();
			double euler[] = RotationMatrixGenerator.matrixToEulerXYZ(cameraToWorld.getR());
			System.out.println("  Pose "+T);
			System.out.printf("  rotation x=%6.2f y=%6.2f z=%6.2f\n",euler[0],euler[1],euler[2]);
		}

		Graphics2D g2 = imageGUI.createGraphics();

		// test to see if there are features not being shown
//		for( PointTrack p : tracker.getActiveTracks() ) {
//			VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,Color.GREEN);
//		}

		for( MultiViewTrack t : alg.getTracker().getPairs() ) {
			Point2D_F64 p = t.getPixel().currLoc;
			
			if( t.views.size() == 1 ) {
				VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,Color.BLUE);
			} else if( t.views.size() >= 2 ) {
				VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,Color.RED);
			} else {
				VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,Color.GRAY);
			}
		}

		List<PointTrack> spawned = tracker.getNewTracks();
		for( PointTrack p : spawned ) {
			VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,Color.PINK);
		}
		
		videoPanel.setBufferedImage(imageGUI);
		videoPanel.setPreferredSize(new Dimension(frame.width,frame.height));
		videoPanel.repaint();

		processedImage = true;
	}

	@Override
	public void refreshAll(Object[] cookies) {
		stopWorker();


		startWorkerThread();
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
	}

	@Override
	public void loadConfigurationFile(String fileName) {
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}
	
	public static void main( String args[] ) {
		VisualizeMonoSeparated app = new VisualizeMonoSeparated(ImageFloat32.class, ImageFloat32.class);

		IntrinsicParameters cameraParam = BoofMiscOps.loadXML("intrinsic.xml");
		app.configure(cameraParam);

		List<PathLabel> inputs = new ArrayList<PathLabel>();
//		inputs.add(new PathLabel("walking", "/home/pja/temp/easy_short.mjpeg"));
//		inputs.add(new PathLabel("walking", "/home/pja/temp/low.mjpeg"));
		inputs.add(new PathLabel("walking", "/home/pja/temp/low_curve.mjpeg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Monocular Visual Odometry");
	}
}
