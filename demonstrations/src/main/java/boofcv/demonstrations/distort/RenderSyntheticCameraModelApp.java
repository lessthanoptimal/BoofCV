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
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.border.BorderType;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.CameraUniversalOmni;
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
import java.util.Arrays;
import java.util.List;

/**
 * Re-renders the image with a new camera model
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class RenderSyntheticCameraModelApp<T extends ImageBase<T>> extends DemonstrationBase
		implements PinholePanel.Listener, UniversalPanel.Listener {

	// TODO Finish adding support for more camera models. This will require refactoring how distortion is done

	ImagePanel gui = new ImagePanel();

	PinholePanel controlPinhole = new PinholePanel(RenderSyntheticCameraModelApp.this);
	UniversalPanel controlUniversal = new UniversalPanel(RenderSyntheticCameraModelApp.this);

	CameraPinholeBrown modelBrown = new CameraPinholeBrown(4);
	CameraUniversalOmni modelUniversal = new CameraUniversalOmni(4);

	ControlPanel controls = new ControlPanel();

	// distorted input
	T dist;

	// storage for undistorted image
	T undist;

	ImageDistort<T, T> undistorter;
	BufferedImage out;

	public RenderSyntheticCameraModelApp( List<?> exampleInputs, ImageType<T> imageType ) {
		super(true, true, exampleInputs, imageType);

		configureDefaultModel(100, 100);

		InterpolatePixel<T> interp = FactoryInterpolation.
				createPixel(0, 255, InterpolationType.BILINEAR, BorderType.ZERO, imageType);
		undistorter = FactoryDistort.distort(true, interp, imageType);

		controlPinhole.setCameraModel(modelBrown);
		controlUniversal.setCameraModel(modelUniversal);

		dist = imageType.createImage(1, 1);
		undist = imageType.createImage(1, 1);

		add(controls, BorderLayout.WEST);
		add(gui, BorderLayout.CENTER);
	}

	@Override
	public synchronized void processImage( int sourceID, long frameID, final BufferedImage buffered, final ImageBase input ) {
		dist.setTo((T)input);

		if (modelBrown.width != input.width || modelBrown.height != input.height) {
			configureDefaultModel(input.width, input.height);
			updatedPinholeModel(modelBrown);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					controlPinhole.setCameraModel(modelBrown);
				}
			});
		} else {
			renderCameraModel();
		}
	}

	private void configureDefaultModel( int width, int height ) {
		modelBrown.width = width;
		modelBrown.height = height;
		modelBrown.fx = width*1.5;
		modelBrown.fy = width*1.5;
		modelBrown.skew = 0;
		modelBrown.cx = width/2.0;
		modelBrown.cy = height/2.0;
		modelBrown.t1 = modelBrown.t2 = 0;
		Arrays.fill(modelBrown.radial, 0);

		modelUniversal.setTo(modelBrown);
		modelUniversal.mirrorOffset = 1.0;
	}

	@Override public synchronized void updatedPinholeModel( CameraPinholeBrown desired ) {
		updateCameraModel(modelBrown, desired);
	}

	@Override public void updatedUniversalModel( CameraUniversalOmni desired ) {
//		updateCameraModel(modelUniversal, desired);
	}

	private void updateCameraModel( CameraPinhole model, CameraPinhole desired ) {
		if (undist.width != model.width || undist.height != model.height) {
			undist.reshape(model.width, model.height);
			SwingUtilities.invokeLater(() -> gui.setPreferredSize(new Dimension(undist.width, undist.height)));
		}

		Point2Transform2_F32 add_p_to_p = LensDistortionOps_F32.
				transformChangeModel(controls.adjustment, model, desired, true, null);
		undistorter.setModel(new PointToPixelTransform_F32(add_p_to_p));

		if (inputMethod == InputMethod.IMAGE)
			renderCameraModel();
	}


	private void renderCameraModel() {
		undistorter.apply(dist, undist);

		if (out != null && (out.getWidth() != undist.width || out.getHeight() != undist.height)) {
			out = new BufferedImage(undist.width, undist.height, out.getType());
		}

		out = ConvertBufferedImage.convertTo(undist, out, true);
		gui.setImageUI(out);
	}

	class ControlPanel extends StandardAlgConfigPanel {
		AdjustmentType adjustment = AdjustmentType.NONE;
		JComboBox<String> selectAdjustment = combo(adjustment.ordinal(), AdjustmentType.values());
		JComboBox<String> selectModel = combo(0, "Brown", "Universal");

		JPanel modelPanel = new JPanel();

		public ControlPanel() {
			modelPanel.add(controlPinhole);

//			addLabeled(selectModel, "Camera Model");
			addLabeled(selectAdjustment, "View");
			addAlignLeft(modelPanel);
		}

		@Override public void controlChanged( Object source ) {
			if (source == selectAdjustment) {
				adjustment = AdjustmentType.values()[selectAdjustment.getSelectedIndex()];
				updatedPinholeModel(controlPinhole.getCameraModel());
			} else if (source == selectModel) {
				modelPanel.removeAll();
				switch (selectModel.getSelectedIndex()) {
					case 0 -> modelPanel.add(controlPinhole);
					case 1 -> modelPanel.add(controlUniversal);
				}
				modelPanel.invalidate();
				modelPanel.validate();
				modelPanel.repaint();
			}
		}
	}

	public static void main( String[] args ) {
		ImageType type = ImageType.pl(3, GrayU8.class);

		List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Sony HX5V", UtilIO.pathExample("structure/dist_cyto_01.jpg")));
		inputs.add(new PathLabel("BumbleBee2",
				UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/left01.jpg")));

		var app = new RenderSyntheticCameraModelApp(inputs, type);

		app.openFile(new File(inputs.get(0).getPath()));

		app.display("Render Synthetic Camera Model");
	}
}
