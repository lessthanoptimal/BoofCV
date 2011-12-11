/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.gui.image;


import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.PixelTransformHomography_F32;
import boofcv.alg.distort.impl.DistortSupport;
import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.alg.interpolate.impl.ImplBilinearPixel_F32;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.homo.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.transform.homo.HomographyPointOps;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Draws two images which are registered to each other by a Homography.
 *
 * @author Peter Abeles
 */
public class HomographyStitchPanel extends JPanel {

	BufferedImage output;

	double scale;
	int workWidth;
	int workHeight;

	PixelTransformHomography_F32 model;
	ImageDistort<ImageFloat32> distort;
	Point2D_I32 corners[];

	public HomographyStitchPanel( double scale , int workWidth , int workHeight ) {
		this.scale = scale;
		this.workWidth = workWidth;
		this.workHeight = workHeight;

		model = new PixelTransformHomography_F32();
		InterpolatePixel<ImageFloat32> interp = new ImplBilinearPixel_F32();
		distort = DistortSupport.createDistort(ImageFloat32.class, model, interp, null);

		corners = new Point2D_I32[4];
		for( int i = 0; i < corners.length; i++ ) {
			corners[i] = new Point2D_I32();
		}
	}

	private Homography2D_F64 createFromWorkToA( ImageFloat32 grayA ) {
		Homography2D_F64 fromAToWork = new Homography2D_F64(scale,0,grayA.width/4,0,scale,grayA.height/4,0,0,1);
		return fromAToWork.invert(null);
	}

	public synchronized void configure(BufferedImage imageA, BufferedImage imageB ,
									   Homography2D_F64 fromAtoB ) {

		ImageFloat32 grayA = ConvertBufferedImage.convertFrom(imageA, null, ImageFloat32.class);
		ImageFloat32 grayB = ConvertBufferedImage.convertFrom(imageB, null, ImageFloat32.class);

		ImageFloat32 work = new ImageFloat32(workWidth,workHeight);

		Homography2D_F64 fromWorkToA = createFromWorkToA(grayA);
		model.set(fromWorkToA);
		distort.apply(grayA,work);

		Homography2D_F64 fromWorkToB = fromWorkToA.concat(fromAtoB,null);
		model.set(fromWorkToB);

		distort.apply(grayB,work);

		output = new BufferedImage(work.width,work.height,BufferedImage.TYPE_INT_BGR);
		ConvertBufferedImage.convertTo(work,output);

		// save the corners of the distorted image
		Homography2D_F64 fromBtoWork = fromWorkToB.invert(null);
		corners[0] = renderPoint(0,0,fromBtoWork);
		corners[1] = renderPoint(grayB.width,0,fromBtoWork);
		corners[2] = renderPoint(grayB.width,grayB.height,fromBtoWork);
		corners[3] = renderPoint(0,grayB.height,fromBtoWork);

		setPreferredSize(new Dimension(output.getWidth(),output.getHeight()));
	}

	private Point2D_I32 renderPoint( int x0 , int y0 , Homography2D_F64 fromBtoWork )
	{
		Point2D_F64 result = new Point2D_F64();
		HomographyPointOps.transform(fromBtoWork,new Point2D_F64(x0,y0),result);
		return new Point2D_I32((int)result.x,(int)result.y);
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;

		// scale the image based upon the panel's size
		double scaleX = getWidth()/(double)output.getWidth();
		double scaleY = getHeight()/(double)output.getHeight();
		double scale = Math.min(scaleX,scaleY);

		// scale the image
		if( scale < 1 ) {
			AffineTransform affineScale = AffineTransform.getScaleInstance(scale,scale);
			AffineTransform a = g2.getTransform();
			a.concatenate(affineScale);
			g2.setTransform(a);
		}

		// draw the combined image
		g2.drawImage(output,0,0,output.getWidth(),output.getHeight(),null);

		// draw lines around the distorted image
		g2.setColor(Color.ORANGE);
		g2.setStroke(new BasicStroke(4));
		g2.drawLine(corners[0].x,corners[0].y,corners[1].x,corners[1].y);
		g2.drawLine(corners[1].x,corners[1].y,corners[2].x,corners[2].y);
		g2.drawLine(corners[2].x,corners[2].y,corners[3].x,corners[3].y);
		g2.drawLine(corners[3].x,corners[3].y,corners[0].x,corners[0].y);
	}


}
