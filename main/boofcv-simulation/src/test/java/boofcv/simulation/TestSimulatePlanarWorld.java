/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.simulation;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.calibration.CalibrationIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraUniversalOmni;
import boofcv.struct.image.GrayF32;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.se.Se3_F64;

import java.awt.image.BufferedImage;
import java.io.File;

/**
 * @author Peter Abeles
 */
public class TestSimulatePlanarWorld {
	public static void main(String[] args) {
		GrayF32 image = new GrayF32(400,300);
		GImageMiscOps.fill(image,255);
		GImageMiscOps.fillRectangle(image,90,20,20,40,40);
		GImageMiscOps.fillRectangle(image,90,60,60,40,40);
		GImageMiscOps.fillRectangle(image,90,100,20,40,40);

		GImageMiscOps.fillRectangle(image,90,300,200,60,60);

		Se3_F64 rectToWorld = new Se3_F64();
		rectToWorld.T.set(0,0,-0.2);
//		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,1.0,0,rectToWorld.R);
		rectToWorld = rectToWorld.invert(null);

		Se3_F64 rectToWorld2 = new Se3_F64();
		rectToWorld2.T.set(0,-0.20,0.3);

		String fisheyePath = UtilIO.pathExample("fisheye/theta/");
		CameraUniversalOmni model = CalibrationIO.load(new File(fisheyePath,"front.yaml"));

		SimulatePlanarWorld alg = new SimulatePlanarWorld();

		alg.setCamera(model);
		alg.addTarget(rectToWorld,0.3,image);
		alg.addTarget(rectToWorld2,0.15,image);
		alg.render();

		BufferedImage output = new BufferedImage(model.width,model.height,BufferedImage.TYPE_INT_RGB);

		ConvertBufferedImage.convertTo(alg.getOutput(),output);

		ImagePanel panel = ShowImages.showWindow(output,"Rendered Fisheye",true);

		for (int i = 0; i < 2000; i++) {
			alg.getImageRect(0).rectToWorld.T.x = 0.7*Math.sin(i*0.01);
			ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,0,0,i*0.05,
					alg.getImageRect(1).rectToWorld.R);
			alg.render();
			ConvertBufferedImage.convertTo(alg.getOutput(),output);
			panel.repaint();
			BoofMiscOps.sleep(10);
		}
	}
}
