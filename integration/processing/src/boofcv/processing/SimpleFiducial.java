/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.processing;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageBase;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified interface for detecting fiducials
 *
 * @author Peter Abeles
 */
public class SimpleFiducial {
	FiducialDetector detector;
	ImageBase boofImage;
	IntrinsicParameters intrinsic;

	public SimpleFiducial(FiducialDetector detector) {
		this.detector = detector;
		boofImage = detector.getInputType().createImage(1,1);
	}

	public void setIntrinsic( IntrinsicParameters intrinsic ) {
		detector.setIntrinsic(intrinsic);
		this.intrinsic = intrinsic;
	}

	public void guessCrappyIntrinsic( int width , int height ) {
		IntrinsicParameters param = new IntrinsicParameters();

		param.width = width; param.height = height;
		param.cx = width/2;
		param.cy = height/2;
		param.fx = param.cx/Math.tan(UtilAngle.degreeToRadian(30)); // assume 60 degree FOV
		param.fy = param.cx/Math.tan(UtilAngle.degreeToRadian(30));

		setIntrinsic(param);
	}

	public List<FiducialFound> detect( PImage image ) {
		boofImage.reshape(image.width,image.height);
		ConvertProcessing.convertFromRGB(image,boofImage);
		detector.detect(boofImage);

		List<FiducialFound> found = new ArrayList<FiducialFound>();
		for (int i = 0; i < detector.totalFound(); i++) {

			int id = detector.getId(i);
			double width = detector.getWidth(i);
			Se3_F64 fiducialToWorld = new Se3_F64();
			detector.getFiducialToCamera(i, fiducialToWorld);

			found.add( new FiducialFound(id,width,fiducialToWorld) );
		}

		return found;
	}

	public void render( PApplet p , FiducialFound fiducial ) {
		double r = fiducial.getWidth()/2.0;
		Point3D_F64 corners[] = new Point3D_F64[8];
		corners[0] = new Point3D_F64(-r,-r,0);
		corners[1] = new Point3D_F64( r,-r,0);
		corners[2] = new Point3D_F64( r, r,0);
		corners[3] = new Point3D_F64(-r, r,0);
		corners[4] = new Point3D_F64(-r,-r,r);
		corners[5] = new Point3D_F64( r,-r,r);
		corners[6] = new Point3D_F64( r, r,r);
		corners[7] = new Point3D_F64(-r, r,r);

		Se3_F64 targetToCamera = fiducial.getFiducialToCamera();
		Point2D_I32 pixel[] = new Point2D_I32[8];
		Point2D_F64 a = new Point2D_F64();
		for (int i = 0; i < 8; i++) {
			Point3D_F64 c = corners[i];
			SePointOps_F64.transform(targetToCamera, c, c);
			PerspectiveOps.convertNormToPixel(intrinsic, c.x / c.z, c.y / c.z, a);
			pixel[i] = new Point2D_I32((int)(a.x+0.5),(int)(a.y+0.5));
		}

		p.strokeWeight(3.0f);
		p.stroke(255, 0, 0);
		p.line(pixel[0].x, pixel[0].y, pixel[1].x, pixel[1].y);
		p.line(pixel[1].x,pixel[1].y,pixel[2].x,pixel[2].y);
		p.line(pixel[2].x,pixel[2].y,pixel[3].x,pixel[3].y);
		p.line(pixel[3].x,pixel[3].y,pixel[0].x,pixel[0].y);

		p.stroke(0, 0, 0);
		p.line(pixel[0].x,pixel[0].y,pixel[4].x,pixel[4].y);
		p.line(pixel[1].x,pixel[1].y,pixel[5].x,pixel[5].y);
		p.line(pixel[2].x,pixel[2].y,pixel[6].x,pixel[6].y);
		p.line(pixel[3].x,pixel[3].y,pixel[7].x,pixel[7].y);

		p.stroke(0x00, 0xFF, 0x00, 125);
		p.line(pixel[4].x,pixel[4].y,pixel[5].x,pixel[5].y);
		p.stroke(0xC0, 0x10, 0xC0, 125);
		p.line(pixel[5].x,pixel[5].y,pixel[6].x,pixel[6].y);
		p.stroke(0x00, 0xA0, 0xC0, 125);
		p.line(pixel[6].x,pixel[6].y,pixel[7].x,pixel[7].y);
		p.stroke(0, 0, 0xFF);
		p.line(pixel[7].x,pixel[7].y,pixel[4].x,pixel[4].y);

		Point2D_I32 middle = pixel[0];
		middle.x = middle.y = 0;
		for (int i = 4; i < 8; i++) {
			middle.x += pixel[i].x;
			middle.y += pixel[i].y;
		}
		middle.x /= 4;
		middle.y /= 4;

		PFont f = p.createFont("Arial",24,true);
		p.textFont(f,24);
		p.fill(255, 0, 0);
		p.text(fiducial.getId()+"",middle.x,middle.y);
	}
}
