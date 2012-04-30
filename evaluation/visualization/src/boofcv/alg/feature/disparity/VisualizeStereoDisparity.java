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

package boofcv.alg.feature.disparity;

import boofcv.abst.feature.disparity.StereoDisparity;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.UtilIntrinsic;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.feature.disparity.FactoryStereoDisparity;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.image.VisualizeImageData;
import boofcv.io.PathLabel;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class VisualizeStereoDisparity <T extends ImageSingleBand, D extends ImageSingleBand>
		extends SelectAlgorithmAndInputPanel
	implements DisparityDisplayPanel.Listener
{

	BufferedImage colorLeft;
	BufferedImage colorRight;
	BufferedImage disparityOut;

	T inputLeft;
	T inputRight;
	T rectLeft;
	T rectRight;
	D disparity;

	StereoParameters calib;
	RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();

	DisparityDisplayPanel control = new DisparityDisplayPanel();
	ImagePanel gui = new ImagePanel();

	int selectedAlg;
	StereoDisparity<T,D> activeAlg;

	// camera parameters after rectification
	DenseMatrix64F rect1,rect2,rectK;

	boolean processedImage = false;
	boolean rectifiedImages = false;

	public VisualizeStereoDisparity() {
		super(1);

		selectedAlg = 0;
		activeAlg = createAlg();
		addAlgorithm(0,"WTA Denoised",0);
		addAlgorithm(0,"WTA",1);

		control.setListener(this);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(control, BorderLayout.WEST);
		panel.add(gui,BorderLayout.CENTER);

		setMainGUI(panel);
	}

	public synchronized void process() {
		if( !rectifiedImages )
			return;

		activeAlg.process(rectLeft, rectRight, disparity);
		disparityOut = VisualizeImageData.grayMagnitudeTemp(disparity,null,activeAlg.getMaxDisparity());

		changeImageView();
	}

	/**
	 * Changes which image is being displayed depending on GUI selection
	 */
	private void changeImageView() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				BufferedImage img;

				switch (control.selectedView) {
					case 0:
						img = disparityOut;
						break;

					case 1:
						img = colorLeft;
						break;

					case 2:
						img = colorRight;
						break;

					default:
						throw new RuntimeException("Unknown option");
				}

				gui.setBufferedImage(img);
				gui.setPreferredSize(new Dimension(img.getWidth(), img.getHeight()));
				gui.repaint();
				processedImage = true;
			}
		});
	}

	@Override
	public void refreshAll(Object[] cookies) {
		process();
	}

	@Override
	public synchronized void setActiveAlgorithm(int indexFamily, String name, Object cookie) {
		int s = ((Number)cookie).intValue();
		if( s != selectedAlg ) {
			selectedAlg = s;
			activeAlg = createAlg();

			doRefreshAll();
		}
	}

	@Override
	public synchronized void changeInput(String name, int index) {
		colorLeft = media.openImage(inputRefs.get(index).getPath(0) );
		colorRight = media.openImage(inputRefs.get(index).getPath(1) );

		int w = colorLeft.getWidth();
		int h = colorLeft.getHeight();

		inputLeft = GeneralizedImageOps.createSingleBand(activeAlg.getInputType(),w,h);
		inputRight = GeneralizedImageOps.createSingleBand(activeAlg.getInputType(),w,h);
		rectLeft = GeneralizedImageOps.createSingleBand(activeAlg.getInputType(),w,h);
		rectRight = GeneralizedImageOps.createSingleBand(activeAlg.getInputType(),w,h);
		disparity = GeneralizedImageOps.createSingleBand(activeAlg.getDisparityType(),w,h);

		ConvertBufferedImage.convertFrom(colorLeft,inputLeft);
		ConvertBufferedImage.convertFrom(colorRight,inputRight);

		rectifyInputImages();

		doRefreshAll();
	}

	/**
	 * Removes distortion and rectifies images.
	 */
	private void rectifyInputImages() {
		// get intrinsic camera calibration matrices
		DenseMatrix64F K1 = UtilIntrinsic.calibrationMatrix(calib.left, null);
		DenseMatrix64F K2 = UtilIntrinsic.calibrationMatrix(calib.right,null);

		// compute rectification matrices
		rectifyAlg.process(K1,new Se3_F64(),K2,calib.getRightToLeft().invert(null));

		rect1 = rectifyAlg.getRect1();
		rect2 = rectifyAlg.getRect1();
		rectK = rectifyAlg.getCalibrationMatrix();

		// adjust view to maximize viewing area while not including black regions
		RectifyImageOps.allInsideLeft(calib.left, rect1, rect2, rectK);
		ImageDistort<T> distortRect1 = RectifyImageOps.rectifyImage(calib.left, rect1, activeAlg.getInputType());
		ImageDistort<T> distortRect2 = RectifyImageOps.rectifyImage(calib.right, rect2, activeAlg.getInputType());

		// rectify input images too
		MultiSpectral<T> ms1 = ConvertBufferedImage.convertFromMulti(colorLeft,null,activeAlg.getInputType());
		MultiSpectral<T> ms2 = ConvertBufferedImage.convertFromMulti(colorRight, null, activeAlg.getInputType());
		MultiSpectral<T> rectMs1 =
				new MultiSpectral<T>(activeAlg.getInputType(),ms1.width,ms1.height,ms1.getNumBands());
		MultiSpectral<T> rectMs2 =
				new MultiSpectral<T>(activeAlg.getInputType(),ms2.width,ms2.height,ms2.getNumBands());

		// rectify and undo distortion
		distortRect1.apply(inputLeft,rectLeft);
		distortRect2.apply(inputRight,rectRight);
		DistortImageOps.distortMS(ms1,rectMs1,distortRect1);
		DistortImageOps.distortMS(ms2,rectMs2,distortRect2);

		ConvertBufferedImage.convertTo(rectMs1,colorLeft);
		ConvertBufferedImage.convertTo(rectMs2,colorRight);

		rectifiedImages = true;
	}

	@Override
	public void loadConfigurationFile(String fileName) {
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public synchronized void disparitySettingChange() {
		activeAlg = createAlg();
		doRefreshAll();
	}

	@Override
	public void disparityGuiChange() {
		changeImageView();
	}

	public void setCalib(StereoParameters calib) {
		this.calib = calib;
	}

	public StereoDisparity<T,D> createAlg() {

		int r = control.regionRadius;

		switch( selectedAlg ) {
			case 0:
				changeGuiActive(true,true);
				return (StereoDisparity)FactoryStereoDisparity.rectWinnerTakeAll(
						control.maxDisparity,r,r,control.pixelError,control.reverseTol);

			case 1:
				changeGuiActive(false,false);
				return (StereoDisparity)FactoryStereoDisparity.rectWinnerTakeAll(
						control.maxDisparity,r,r,-1,-1);

			default:
				throw new RuntimeException("Unknown selection");
		}

	}

	/**
	 * Active and deactivates different GUI configurations
	 */
	private void changeGuiActive( final boolean error , final boolean reverse ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				control.setActiveGui(error,reverse);
			}
		});
	}

	public static void main( String args[] ) {

		VisualizeStereoDisparity app = new VisualizeStereoDisparity();

		String dirCalib = "../data/evaluation/calibration/stereo/Bumblebee2_Chess/";
		String dirImgs = "/home/pja/a/";

		StereoParameters calib = BoofMiscOps.loadXML(dirCalib+"stereo.xml");

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("Chair 1",dirImgs+"chair01_left.jpg",dirImgs+"chair01_right.jpg"));
		inputs.add(new PathLabel("Chair 2",dirImgs+"chair02_left.jpg",dirImgs+"chair02_right.jpg"));
		inputs.add(new PathLabel("Chair 3",dirImgs+"chair03_left.jpg",dirImgs+"chair03_right.jpg"));
		inputs.add(new PathLabel("Chair 4",dirImgs+"chair04_left.jpg",dirImgs+"chair04_right.jpg"));
		inputs.add(new PathLabel("Stones 1",dirImgs+"stones01_left.jpg",dirImgs+"stones01_right.jpg"));
		inputs.add(new PathLabel("Thing 1",dirImgs+"thing01_left.jpg",dirImgs+"thing01_right.jpg"));
		inputs.add(new PathLabel("Wall 1",dirImgs+"wall01_left.jpg",dirImgs+"wall01_right.jpg"));

		app.setCalib(calib);
		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Stereo Disparity");
	}
}
