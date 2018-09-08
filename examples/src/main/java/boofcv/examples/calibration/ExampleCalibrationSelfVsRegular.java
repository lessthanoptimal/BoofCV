/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.calibration;

import boofcv.abst.fiducial.calib.ConfigChessboard;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.abst.geo.TriangulateTwoViews;
import boofcv.abst.geo.calibration.CalibrateMonoPlanar;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.geo.GeometricResult;
import boofcv.alg.geo.MultiViewOps;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.selfcalib.SelfCalibrationLinearDualQuadratic;
import boofcv.factory.fiducial.FactoryFiducialCalibration;
import boofcv.factory.geo.EnumFundamental;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class ExampleCalibrationSelfVsRegular {

	public static CameraPinholeRadial performStandardCalibration(List<Point2D_F64> layout , List<CalibrationObservation> observations ) {
		// Declare and setup the calibration algorithm
		CalibrateMonoPlanar calibrationAlg = new CalibrateMonoPlanar(layout);

		// tell it type type of target and which parameters to estimate
		calibrationAlg.configurePinhole( true, 2, false);

		// add feature locations
		for (int i = 0; i < observations.size(); i++) {
			calibrationAlg.addImage(observations.get(i));
		}

		// process and compute intrinsic parameters
		return calibrationAlg.process();
	}

	public static CameraPinholeRadial performSelfCalibration(List<CalibrationObservation> observations ) {
		List<DMatrixRMaj> projectives = new ArrayList<>();

		Estimate1ofEpipolar estimateF = FactoryMultiView.fundamental_1(EnumFundamental.LINEAR_8,0);
		DMatrixRMaj F = new DMatrixRMaj(3,3);

		List<AssociatedPair> associated = new ArrayList<>();
		CalibrationObservation obs = observations.get(0);

		// Assume the image center is the optical center. This makes auto calibration work much better
		double cx = obs.getWidth()/2;
		double cy = obs.getHeight()/2;

		for (int i = 0; i < obs.points.size(); i++) {
			PointIndex2D_F64 o = obs.points.get(i);
			AssociatedPair a = new AssociatedPair();
			// need to subtract the center to make tha math work out
			a.p1.x = o.x - cx;
			a.p1.y = o.y - cy;
			associated.add(a);
		}

		obs = observations.get(1);
		for (int j = 0; j < obs.points.size(); j++) {
			PointIndex2D_F64 o = obs.points.get(j);
			AssociatedPair a = associated.get(j);
			a.p2.x = o.x - cx;
			a.p2.y = o.y - cy;
		}
		if( !estimateF.process(associated,F) )
			throw new RuntimeException("Estimate Fundamental failed");

		// Now find the camera matrices. x' = P*X , where P is the camera matric
		DMatrixRMaj P0 = new DMatrixRMaj(3,4);
		CommonOps_DDRM.setIdentity(P0);
		DMatrixRMaj P1 = MultiViewOps.fundamentalToProjective(F);

		// Triangulate the points
		TriangulateTwoViews triangulation = FactoryMultiView.triangulateTwoDLT();

		CalibrationObservation obs0 = observations.get(0);
		CalibrationObservation obs1 = observations.get(1);

		List<Point3D_F64 > locations = new ArrayList<>();
		Point4D_F64 p4 = new Point4D_F64();

		for (int i = 0; i < obs0.points.size(); i++) {
			Point2D_F64 pixelObs0 = obs0.points.get(i);
			Point2D_F64 pixelObs1 = obs1.points.get(i);

			if( triangulation.triangulate(pixelObs0,pixelObs1,P0,P1,p4)) {
				locations.add( new Point3D_F64(p4.x/p4.w, p4.y/p4.w, p4.z/p4.w));
			} else {
				throw new RuntimeException("Triangulation failed??? Real code would ignore or handle this");
			}
		}

		System.out.println("Got this far");
		// Estimate camera matrix for all the remaining views
//		FactoryMultiView.triangulatePoseFromPair()

		SelfCalibrationLinearDualQuadratic estimateK = new SelfCalibrationLinearDualQuadratic(1.0);
		estimateK.addProjectives(projectives);

		GeometricResult result = estimateK.solve();
		switch( result ) {
			case SUCCESS: break;
			default: throw new RuntimeException("Self calibration failed. Reason="+result);
		}

		SelfCalibrationLinearDualQuadratic.Intrinsic found = estimateK.getSolutions().get(0);

		CameraPinholeRadial initial = new CameraPinholeRadial(found.fx,found.fy,found.skew,cx,cy,obs.getWidth(),obs.getHeight());

		// TODO bundle adjustment

		return initial;
	}

	public static void main(String[] args) {
		DetectorFiducialCalibration detector;
		List<String> images;

		// Chessboard Example
		detector = FactoryFiducialCalibration.chessboard(new ConfigChessboard(7, 5, 30));
		images = UtilIO.listByPrefix(UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess"),"left");

		List<CalibrationObservation> observations = new ArrayList<>();

		for( String n : images ) {
			BufferedImage input = UtilImageIO.loadImage(n);
			if( input != null ) {
				GrayF32 image = ConvertBufferedImage.convertFrom(input,(GrayF32)null);
				if( detector.process(image)) {
					observations.add(detector.getDetectedPoints().copy());
				} else {
					System.err.println("Failed to detect target in " + n);
				}
			}
		}

		CameraPinholeRadial paramSelf = performSelfCalibration(observations);
		CameraPinholeRadial paramStandard = performStandardCalibration(detector.getLayout(),observations);

		paramSelf.print();
		paramStandard.print();
	}
}
