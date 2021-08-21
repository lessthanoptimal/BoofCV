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

package boofcv.alg.fiducial.qrcode;

import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.simulation.SimulatePlanarWorld;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Polygon2D_F64;

import static georegression.struct.se.SpecialEuclideanOps_F64.eulerXyz;

public class QrCodeDistortedChecks {
	CameraPinholeBrown intrinsic = new CameraPinholeBrown(250, 250, 0, 250, 250, 500, 500).
			fsetRadial(-0.1, -0.01);
	LensDistortionBrown distortion = new LensDistortionBrown(intrinsic);
	Point2Transform2_F64 p2p = distortion.undistort_F64(true, true);

	QrCodeGeneratorImage renderQR = new QrCodeGeneratorImage(10);
	SimulatePlanarWorld simulator = new SimulatePlanarWorld();

	Se3_F64 markerToWorld = eulerXyz(-0.3, 0, 1, 0.1, Math.PI, 0, null); // for distorted
//	Se3_F64 markerToWorld = eulerXyz(-0.2,0,1.2,0.1,Math.PI,0,null); // for undistorted

	QrCode qr = new QrCodeEncoder().setVersion(2).addNumeric("123").fixate();
	double w = 1.5;
	double r = w/2;

	GrayF32 image;
	GrayU8 binary = new GrayU8(intrinsic.width, intrinsic.height);

	public void render() {
		simulator.setCamera(intrinsic);
		simulator.setBackground(255);

		renderQR.setBorderModule(0);
		renderQR.render(qr);
		simulator.addSurface(markerToWorld, w, renderQR.getGrayF32());

		image = simulator.render();
		ThresholdImageOps.threshold(image, binary, 100, true);
	}

	public void setLocation( Polygon2D_F64 bl, Polygon2D_F64 tl, Polygon2D_F64 tr ) {
		double mw = w/qr.getNumberOfModules();

		simulator.computePixel(0, -r, -r + mw*7, bl.get(0));
		simulator.computePixel(0, -r + mw*7, -r + mw*7, bl.get(1));
		simulator.computePixel(0, -r + mw*7, -r, bl.get(2));
		simulator.computePixel(0, -r, -r, bl.get(3));

		simulator.computePixel(0, -r, r, tl.get(0));
		simulator.computePixel(0, -r + mw*7, r, tl.get(1));
		simulator.computePixel(0, -r + mw*7, r - mw*7, tl.get(2));
		simulator.computePixel(0, -r, r - mw*7, tl.get(3));

		simulator.computePixel(0, r - mw*7, r, tr.get(0));
		simulator.computePixel(0, r, r, tr.get(1));
		simulator.computePixel(0, r, r - mw*7, tr.get(2));
		simulator.computePixel(0, r - mw*7, r - mw*7, tr.get(3));
	}

	public void distToUndist( Polygon2D_F64 t ) {
		for (int i = 0; i < t.size(); i++) {
			Point2D_F64 p = t.get(i);
			p2p.compute(p.x, p.y, p);
		}
	}

	public void locateQrFeatures() {
		setLocation(qr.ppDown, qr.ppCorner, qr.ppRight);
		// input will be in undistorted coordinates
		distToUndist(qr.ppDown);
		distToUndist(qr.ppCorner);
		distToUndist(qr.ppRight);
		qr.threshCorner = qr.threshDown = qr.threshRight = 125;

		// used to compute location of modules
		int N = qr.getNumberOfModules();
		double mw = w/N;

		// set location of alignment pattern
		for (int i = 0; i < qr.alignment.size; i++) {
			QrCode.Alignment al = qr.alignment.get(i);
			simulator.computePixel(0, -r + (al.moduleX + 0.5)*mw, r - (al.moduleY + 0.5)*mw, al.pixel);
			p2p.compute(al.pixel.x, al.pixel.y, al.pixel);
		}
	}
}
