/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.distort.PointToPixelTransform_F32;
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
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
public class RenderSyntheticCamerModelApp<T extends ImageBase<T>> extends DemonstrationBase
	implements PinholePanel.Listener, ActionListener
{

	ImagePanel gui = new ImagePanel();
	PinholePanel control;
	JComboBox selectAdjustment;
	AdjustmentType adjustment = AdjustmentType.NONE;

	CameraPinholeBrown origModel = new CameraPinholeBrown(2);

	// distorted input
	T dist;

	// storage for undistorted image
	T undist;

	ImageDistort<T,T> undistorter;
	BufferedImage out;

	public RenderSyntheticCamerModelApp(List<?> exampleInputs, ImageType<T> imageType) {
		super(true,true,exampleInputs, imageType);

		configureDefaultModel(100, 100);

		selectAdjustment = new JComboBox(AdjustmentType.values());
		selectAdjustment.addActionListener(this);
		selectAdjustment.setMaximumSize(selectAdjustment.getPreferredSize());
		control = new PinholePanel(origModel, this);

		dist = imageType.createImage(1,1);
		undist = imageType.createImage(1,1);

		StandardAlgConfigPanel leftPanel = new StandardAlgConfigPanel();
		leftPanel.addAlignLeft(selectAdjustment, leftPanel);
		leftPanel.addAlignLeft(control, leftPanel);

		add(leftPanel, BorderLayout.WEST);
		add(gui, BorderLayout.CENTER);

		InterpolatePixel<T> interp = FactoryInterpolation.
				createPixel(0,255,InterpolationType.BILINEAR, BorderType.ZERO, imageType);
		undistorter = FactoryDistort.distort(true, interp, imageType);
	}


	@Override
	public synchronized void processImage(int sourceID, long frameID, final BufferedImage buffered, final ImageBase input)
	{
		dist.setTo((T)input);

		if( origModel.width != input.width || origModel.height != input.height ) {
			configureDefaultModel(input.width, input.height);
			updatedPinholeModel(origModel);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					control.setCameraModel(origModel);
				}
			});
		} else {
			renderCameraModel();
		}
	}

	private void configureDefaultModel(int width , int height ) {
		origModel.width = width;
		origModel.height = height;
		origModel.fx = width*1.5;
		origModel.fy = width*1.5;
		origModel.skew = 0;
		origModel.cx = width/2.0;
		origModel.cy = height/2.0;
		origModel.t1 = origModel.t2 = 0;
		Arrays.fill(origModel.radial,0);
	}

	@Override
	public synchronized void updatedPinholeModel(CameraPinholeBrown desired) {

		if( undist.width != desired.width || undist.height != desired.height ) {
			undist.reshape(desired.width, desired.height);
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					gui.setPreferredSize(new Dimension(undist.width, undist.height));
//					gui.invalidate();
				}
			});
		}

		Point2Transform2_F32 add_p_to_p = LensDistortionOps_F32.
				transformChangeModel(adjustment, origModel,desired,true,null);
		undistorter.setModel(new PointToPixelTransform_F32(add_p_to_p));

		if( inputMethod == InputMethod.IMAGE )
			renderCameraModel();
	}

	private void renderCameraModel() {
		undistorter.apply(dist,undist);

		if( out != null && (out.getWidth() != undist.width || out.getHeight() != undist.height )) {
			out = new BufferedImage(undist.width, undist.height, out.getType());
		}

		out = ConvertBufferedImage.convertTo(undist,out,true);
		gui.setImageUI(out);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getSource() == selectAdjustment ) {
			adjustment = AdjustmentType.values()[selectAdjustment.getSelectedIndex()];
			updatedPinholeModel(control.getDesired());
		}
	}


	public static void main( String args[] ) {
		ImageType type = ImageType.pl(3, GrayU8.class);

		List<PathLabel> inputs = new ArrayList<>();
		inputs.add(new PathLabel("Sony HX5V", UtilIO.pathExample("structure/dist_cyto_01.jpg")));
		inputs.add(new PathLabel("BumbleBee2",
				UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/left01.jpg")));

		RenderSyntheticCamerModelApp app = new RenderSyntheticCamerModelApp(inputs,type);

		app.openFile(new File(inputs.get(0).getPath()));

		app.display("Render Synthetic Camera Model");
	}

}
