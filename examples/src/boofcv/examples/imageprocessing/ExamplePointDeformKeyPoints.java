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

package boofcv.examples.imageprocessing;

import boofcv.abst.distort.ConfigDeformPointMLS;
import boofcv.abst.distort.PointDeformKeyPoints;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.border.BorderType;
import boofcv.factory.distort.FactoryDistort;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_F32;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstration on how to use a key point based image deformation. A static image will be modified in real time
 * to make it appear as if the 'man' is waving. These deformation algorithms specify keypoints which match between
 * two images. When the pixel coordinate is moved the pixels are distorted such that they minimize an error function.
 *
 * @author Peter Abeles
 */
public class ExamplePointDeformKeyPoints {
	public static void main(String[] args) {
		BufferedImage orig = UtilImageIO.loadImage(UtilIO.pathExample("standard/man_mls.jpg"));
		BufferedImage bufferedOut = new BufferedImage(orig.getWidth(),orig.getHeight(),BufferedImage.TYPE_INT_RGB);

		Planar<GrayF32> input = ConvertBufferedImage.convertFrom(orig,true, ImageType.pl(3,GrayF32.class));
		Planar<GrayF32> output = input.createSameShape();

		List<Point2D_F32> src = new ArrayList<>();
		List<Point2D_F32> dst = new ArrayList<>();

		src.add(new Point2D_F32(64, 241));
		src.add(new Point2D_F32(266, 119));
		src.add(new Point2D_F32(265, 240));
		src.add(new Point2D_F32(208, 410));
		src.add(new Point2D_F32(181, 536));
		src.add(new Point2D_F32(335, 409));
		src.add(new Point2D_F32(375, 531));
		src.add(new Point2D_F32(473, 238));

		for(Point2D_F32 p : src ) {
			dst.add( p.copy() );
		}

		ConfigDeformPointMLS config = new ConfigDeformPointMLS();
		PointDeformKeyPoints deform = FactoryDistort.deformMls(config);
		deform.setImageShape(input.width,input.height);


		ImageDistort<Planar<GrayF32>,Planar<GrayF32>> distorter =
				FactoryDistort.distort(true, InterpolationType.BILINEAR,
				BorderType.ZERO, input.getImageType(), input.getImageType());

		deform.setImageShape(input.width,input.height);
		deform.setSource(src);
		deform.setDestination(dst);

		ConvertBufferedImage.convertTo(output, bufferedOut, true);
		ImagePanel panel = ShowImages.showWindow(bufferedOut,"Point Based Distortion Animation", true);

		int count = 0;
		while( true ) {
			// specify new locations of key points
			double theta = count++*Math.PI/30;
			dst.get(7).y = (float)(238 + Math.sin(theta)*30);       // right arm
			dst.get(0).y = (float)(241 - Math.sin(theta*2.0)*20);   // left arm
			dst.get(1).x = (float)(266 + Math.sin(theta*0.25)*10);  // head

			// tell the deformation algorithm that destination points have changed
			deform.setDestination(dst);
			// Tell the distorter that the model has changed. If cached is set to false you can ignore this step
			distorter.setModel( new PointToPixelTransform_F32(deform));
			// distort the image
			distorter.apply(input, output);
			// Show the results
			ConvertBufferedImage.convertTo(output, bufferedOut, true);
			panel.repaint();

			BoofMiscOps.sleep(30);
		}

	}
}
