/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.calibration;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyDistortImageOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Given a set of stereo images and their intrinsic parameters, display the rectified images and a horizontal line
 * where the user clicks. The different rectified images show how the view can be optimized for different purposes.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class ShowRectifyCalibratedApp extends SelectAlgorithmAndInputPanel {

	ListDisplayPanel gui = new ListDisplayPanel();

	StereoParameters param;

	// distorted input images
	Planar<GrayF32> distLeft;
	Planar<GrayF32> distRight;

	// storage for undistorted and rectified images
	Planar<GrayF32> rectLeft;
	Planar<GrayF32> rectRight;

	boolean hasProcessed = false;

	public ShowRectifyCalibratedApp() {
		super(0);

		setMainGUI(gui);
	}

	public void configure( final BufferedImage origLeft, final BufferedImage origRight, StereoParameters param ) {
		this.param = param;

		// distorted images
		distLeft = ConvertBufferedImage.convertFromPlanar(origLeft, null, true, GrayF32.class);
		distRight = ConvertBufferedImage.convertFromPlanar(origRight, null, true, GrayF32.class);

		// storage for undistorted + rectified images
		rectLeft = new Planar<>(GrayF32.class,
				distLeft.getWidth(), distLeft.getHeight(), distLeft.getNumBands());
		rectRight = new Planar<>(GrayF32.class,
				distRight.getWidth(), distRight.getHeight(), distRight.getNumBands());

		// Compute rectification
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		Se3_F64 leftToRight = param.getRightToLeft().invert(null);

		// original camera calibration matrices
		DMatrixRMaj K1 = PerspectiveOps.pinholeToMatrix(param.getLeft(), (DMatrixRMaj)null);
		DMatrixRMaj K2 = PerspectiveOps.pinholeToMatrix(param.getRight(), (DMatrixRMaj)null);

		rectifyAlg.process(K1, new Se3_F64(), K2, leftToRight);

		// rectification matrix for each image
		DMatrixRMaj rect1 = rectifyAlg.getUndistToRectPixels1();
		DMatrixRMaj rect2 = rectifyAlg.getUndistToRectPixels2();
		DMatrixRMaj rectK = rectifyAlg.getCalibrationMatrix();

		// show results and draw a horizontal line where the user clicks to see rectification easier
		SwingUtilities.invokeLater(() -> {
			gui.reset();
			gui.addItem(new RectifiedPairPanel(true, origLeft, origRight), "Original");
		});

		// add different types of adjustments
		addRectified("No Adjustment", rect1, rect2);
		RectifyImageOps.allInsideLeft(param.left, rect1, rect2, rectK, null);
		addRectified("All Inside", rect1, rect2);
		RectifyImageOps.fullViewLeft(param.left, rect1, rect2, rectK, null);
		addRectified("Full View", rect1, rect2);

		hasProcessed = true;
	}

	private void addRectified( final String name, final DMatrixRMaj rect1, final DMatrixRMaj rect2 ) {
		FMatrixRMaj rect1_F32 = new FMatrixRMaj(3, 3); // TODO simplify code some how
		FMatrixRMaj rect2_F32 = new FMatrixRMaj(3, 3);
		ConvertMatrixData.convert(rect1, rect1_F32);
		ConvertMatrixData.convert(rect2, rect2_F32);

		// Will rectify the image
		ImageType<Planar<GrayF32>> imageType = ImageType.pl(3, GrayF32.class);
		ImageDistort<Planar<GrayF32>, Planar<GrayF32>> imageDistortLeft =
				RectifyDistortImageOps.rectifyImage(param.getLeft(), rect1_F32, BorderType.ZERO, imageType);
//		ImageDistort<Planar<GrayF32>, Planar<GrayF32>> imageDistortRight =
//				RectifyDistortImageOps.rectifyImage(param.getRight(), rect2_F32, BorderType.ZERO, imageType);

		// Fill the image with all black
		GImageMiscOps.fill(rectLeft, 0);
		GImageMiscOps.fill(rectRight, 0);

		// Render the rectified image
		imageDistortLeft.apply(distLeft, rectLeft);
		imageDistortLeft.apply(distRight, rectRight);

		// convert for output
		final BufferedImage outLeft = ConvertBufferedImage.convertTo(rectLeft, null, true);
		final BufferedImage outRight = ConvertBufferedImage.convertTo(rectRight, null, true);

		// Add this rectified image
		SwingUtilities.invokeLater(() -> gui.addItem(new RectifiedPairPanel(true, outLeft, outRight), name));
	}

	@Override
	public void refreshAll( Object[] cookies ) {}

	@Override
	public void setActiveAlgorithm( int indexFamily, String name, Object cookie ) {}

	@Override
	public void changeInput( String name, int index ) {
		PathLabel refs = inputRefs.get(index);

		StereoParameters param = CalibrationIO.load(media.openFileNotNull(refs.getPath(0)));
		BufferedImage origLeft = media.openImage(refs.getPath(1));
		BufferedImage origRight = media.openImage(refs.getPath(2));

		configure(Objects.requireNonNull(origLeft), Objects.requireNonNull(origRight), param);
	}

	@Override
	public void loadConfigurationFile( String fileName ) {}

	@Override
	public boolean getHasProcessedImage() {
		return hasProcessed;
	}

	public static void main( String[] args ) {
		ShowRectifyCalibratedApp app = new ShowRectifyCalibratedApp();

		// camera config, image left, image right
		String dir = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess");

		java.util.List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("BumbleBee", dir + "/stereo.yaml", dir + "/left05.jpg", dir + "/right05.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while (!app.getHasProcessedImage()) {
			BoofMiscOps.sleep(10);
		}
		ShowImages.showWindow(app, "Calibrated Camera Rectification", true);

		System.out.println("Done");
	}
}
