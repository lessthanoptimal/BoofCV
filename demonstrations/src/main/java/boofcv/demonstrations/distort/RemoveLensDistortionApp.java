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

package boofcv.demonstrations.distort;

import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps_F32;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays undistorted images with different types of adjustments for visibility.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class RemoveLensDistortionApp<T extends ImageBase<T>> extends DemonstrationBase {

	ListDisplayPanel gui = new ListDisplayPanel();

	CameraPinholeBrown param;

	// distorted input
	T dist;

	// storage for undistorted image
	T undist;

	public RemoveLensDistortionApp( List<?> exampleInputs, ImageType<T> imageType ) {
		super(false, false, exampleInputs, imageType);

		add(gui, BorderLayout.CENTER);
	}

	@Override
	public void openFile( File file ) {
		File candidates[] = new File[]{
				new File(file.getParent(), "intrinsic.yaml"),
				new File(file.getParent(), "intrinsicLeft.yaml"), // this is a bit of a hack...
				new File(file.getParent(), file.getName() + ".yaml")};

		CameraPinholeBrown model = null;
		for (File c : candidates) {
			try {
				model = CalibrationIO.load(UtilIO.ensureURL(c.getPath()));
				break;
			} catch (RuntimeException ignore) {}
		}
		if (model == null) {
			System.err.println("Can't find camera model for this image");
			return;
		}
		this.param = model;
		super.openFile(file);
	}

	@Override
	public void processImage( int sourceID, long frameID, final BufferedImage buffered, ImageBase input ) {
		// strip away distortion parameters
		CameraPinhole desired = new CameraPinhole(param);

		// distorted image
		dist = (T)input.clone();

		// storage for undistorted image
		undist = (T)input.createSameShape();

		// show results and draw a horizontal line where the user clicks to see rectification easier
		SwingUtilities.invokeLater(() -> {
			gui.reset();
			gui.addItem(new ImagePanel(buffered), "Original");
		});

		// add different types of adjustments
		Point2Transform2_F32 add_p_to_p = LensDistortionOps_F32.transformChangeModel(AdjustmentType.NONE, param, desired, true, null);
		addUndistorted("No Adjustment", add_p_to_p);
		Point2Transform2_F32 expand = LensDistortionOps_F32.transformChangeModel(AdjustmentType.EXPAND, param, desired, true, null);
		addUndistorted("Expand", expand);
		Point2Transform2_F32 fullView = LensDistortionOps_F32.transformChangeModel(AdjustmentType.FULL_VIEW, param, desired, true, null);
		addUndistorted("Full View", fullView);
		Point2Transform2_F32 center = LensDistortionOps_F32.transformChangeModel(AdjustmentType.CENTER, param, desired, true, null);
		addUndistorted("Center", center);
	}

	private void addUndistorted( final String name, final Point2Transform2_F32 model ) {
		// Set up image distort
		InterpolatePixel<T> interp = FactoryInterpolation.
				createPixel(0, 255, InterpolationType.BILINEAR, BorderType.ZERO, undist.getImageType());
		ImageDistort<T, T> undistorter = FactoryDistort.distort(false, interp, undist.getImageType());
		undistorter.setModel(new PointToPixelTransform_F32(model));

		undistorter.apply(dist, undist);

		final BufferedImage out = ConvertBufferedImage.convertTo(undist, null, true);

		// Add this rectified image
		SwingUtilities.invokeLater(() -> gui.addItem(new ImagePanel(out), name));
	}

	public static void main( String[] args ) {
		ImageType type = ImageType.pl(3, GrayU8.class);

		java.util.List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Sony HX5V", UtilIO.pathExample("structure/dist_cyto_01.jpg")));
		inputs.add(new PathLabel("BumbleBee2",
				UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/left01.jpg")));

		RemoveLensDistortionApp app = new RemoveLensDistortionApp(inputs, type);

		app.openFile(new File(inputs.get(0).getPath()));

		app.display("Remove Lens Distortion");
	}
}
