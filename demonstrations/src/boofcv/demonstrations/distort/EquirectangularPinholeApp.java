/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.spherical.EquirectangularToPinhole_F32;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.metric.UtilAngle;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a synthetic pinhole camera for displaying images from the equirectangular image
 *
 * @author Peter Abeles
 */
// TODO Draw camera  center in equirectangular
// TODO controls for pinhole.  width, height, FOV
// TODO controls for camera orientation.  pitch, yaw, roll
// TODO keyboard commands for camera should be relative to current camera orientation
public class EquirectangularPinholeApp<T extends ImageBase<T>> extends DemonstrationBase<T> {

	EquirectangularToPinhole_F32 distorter = new EquirectangularToPinhole_F32();
	ImageDistort<T,T> distortImage;

	BufferedImage buffPinhole = new BufferedImage(1,1,BufferedImage.TYPE_INT_BGR);
	BufferedImage buffEqui = new BufferedImage(1,1,BufferedImage.TYPE_INT_BGR);
	T equi;
	T pinhole;

	int camWidth = 320;
	int camHeight = 240;
	double hfov = 80; //  in degrees


	float yaw,pitch,roll;

	CameraPinhole cameraModel = new CameraPinhole();

	ImagePanel panelPinhole = new ImagePanel();
	ImagePanel panelEqui = new ImagePanel();

	public EquirectangularPinholeApp(List<?> exampleInputs, ImageType<T> imageType) {
		super(exampleInputs, imageType);

		updateIntrinsic();
		distorter.setPinhole(cameraModel);

		BorderType borderType = BorderType.EXTENDED;
		InterpolatePixel<T> interp =
				FactoryInterpolation.createPixel(0, 255, TypeInterpolate.BILINEAR,borderType, imageType);
		distortImage = FactoryDistort.distort(true, interp, imageType);
		distortImage.setRenderAll(true);

		equi = imageType.createImage(1,1);
		pinhole = imageType.createImage(camWidth,camHeight);
		buffPinhole = new BufferedImage(camWidth,camHeight,BufferedImage.TYPE_INT_BGR);
		panelPinhole.setPreferredSize( new Dimension(camWidth,camHeight));
		panelPinhole.setBufferedImage(buffEqui);
		panelPinhole.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				System.out.println("Key Event");
				switch( e.getKeyCode() ) {
					case KeyEvent.VK_W: pitch += 0.01; break;
					case KeyEvent.VK_S: pitch -= 0.01; break;
					case KeyEvent.VK_A: yaw -= 0.01; break;
					case KeyEvent.VK_D: yaw += 0.01; break;
					case KeyEvent.VK_Q: roll -= 0.01; break;
					case KeyEvent.VK_E: roll += 0.01; break;
					default:
						return;
				}
				System.out.println("yaw = "+yaw+" pitch "+pitch);
				distorter.setDirection(roll, yaw, pitch);
				distortImage.setModel(distorter); // dirty the transform
				if( inputMethod == InputMethod.IMAGE )
					rerenderPinhole();
			}
		});
		panelPinhole.setFocusable(true);
		panelPinhole.grabFocus();

		add(panelPinhole, BorderLayout.CENTER);
		add(panelEqui, BorderLayout.SOUTH);


	}

	@Override
	public void processImage(BufferedImage buffered, T input) {
		// create a copy of the input image for output purposes
		if( buffEqui.getWidth() != buffered.getWidth() || buffEqui.getHeight() != buffered.getHeight() ) {
			buffEqui = new BufferedImage(buffered.getWidth(),buffered.getHeight(),BufferedImage.TYPE_INT_BGR);
			panelEqui.setPreferredSize(new Dimension(buffered.getWidth(),buffered.getHeight()));
			panelEqui.setBufferedImageSafe(buffEqui);

			distorter.setEquirectangularShape(input.width,input.height);
			distortImage.setModel(distorter);
		}
		buffEqui.createGraphics().drawImage(buffered,0,0,null);
		equi.setTo(input);

		rerenderPinhole();
	}

	private void rerenderPinhole() {
		distortImage.apply(equi,pinhole);
		ConvertBufferedImage.convertTo(pinhole,buffPinhole,true);
		panelPinhole.setBufferedImageSafe(buffPinhole);
	}


	private void updateIntrinsic() {
		cameraModel.width = camWidth;
		cameraModel.height = camHeight;
		cameraModel.cx = camWidth/2;
		cameraModel.cy = camHeight/2;

		double f = (camWidth/2.0)/Math.tan(UtilAngle.degreeToRadian(hfov)/2.0);

		cameraModel.fx = cameraModel.fy = f;
		cameraModel.skew = 0;
	}

	public static void main(String[] args) {

		ImageType type = ImageType.pl(3, GrayU8.class);

		List<PathLabel> examples = new ArrayList<PathLabel>();
		examples.add(new PathLabel("Half Dome", UtilIO.pathExample("spherical/equirectangular_half_dome.jpg")));

		EquirectangularPinholeApp app = new EquirectangularPinholeApp(examples,type);

		app.openFile(new File(examples.get(0).getPath()));

		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app, "Equirectanglar to Pinhole Camera",true);

	}
}
