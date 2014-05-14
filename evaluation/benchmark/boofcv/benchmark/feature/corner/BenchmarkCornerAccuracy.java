/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.benchmark.feature.corner;

import boofcv.abst.feature.detect.interest.ConfigFast;
import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.filter.derivative.GradientThree;
import boofcv.alg.filter.derivative.HessianFromGradient;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder_I32;
import boofcv.factory.feature.detect.interest.FactoryDetectPoint;
import boofcv.gui.image.ShowImages;
import boofcv.struct.BoofDefaults;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
// todo add floating point images to test.  do it by abstracting the code
public class BenchmarkCornerAccuracy {

	int width = 250;
	int height = 300;
	int radius = 2;

	double distTol = 8;

	Random rand = new Random(234);

	ImageUInt8 image = new ImageUInt8(width, height);
	ImageSInt16 derivX = new ImageSInt16(width, height);
	ImageSInt16 derivY = new ImageSInt16(width, height);
	ImageSInt16 derivXX = new ImageSInt16(width, height);
	ImageSInt16 derivYY = new ImageSInt16(width, height);
	ImageSInt16 derivXY = new ImageSInt16(width, height);

	List<Point2D_F64> corners = new ArrayList<Point2D_F64>();

	public void detectCorners(String name, GeneralFeatureDetector<ImageUInt8, ImageSInt16> detector) {
		if (detector.getRequiresGradient()) {
			GradientThree.process(image, derivX, derivY, BoofDefaults.borderDerivative_I32());
		}
		if (detector.getRequiresHessian()) {
			HessianFromGradient.hessianThree(derivX, derivY, derivXX, derivYY, derivXY, BoofDefaults.borderDerivative_I32());
		}

		detector.process(image, derivX, derivY, derivXX, derivYY, derivXY);

		QueueCorner corners = detector.getMaximums();

		evaluate(corners, detector.getIntensity(), name);
	}

	public void evaluateAll() {

		createTestImage();

		ShowImages.showWindow(image, "Evaluation Image");
		ShowImages.showWindow(derivX, "DerivX");
		ShowImages.showWindow(derivY, "DerivY");

		// todo try different noise levels
		int maxFeatures = corners.size() * 2;

		detectCorners("FAST", FactoryDetectPoint.<ImageUInt8, ImageSInt16>createFast(
				new ConfigFast(11,9),new ConfigGeneralDetector(maxFeatures,10,11), ImageUInt8.class));
		detectCorners("Harris", FactoryDetectPoint.<ImageUInt8, ImageSInt16>
				createHarris(new ConfigGeneralDetector(maxFeatures,radius, 0.04f), false, ImageSInt16.class));
		detectCorners("KitRos", FactoryDetectPoint.<ImageUInt8, ImageSInt16>
				createKitRos(new ConfigGeneralDetector(maxFeatures,radius, 1f), ImageSInt16.class));
		detectCorners("KLT", FactoryDetectPoint.<ImageUInt8, ImageSInt16>
				createShiTomasi(new ConfigGeneralDetector(maxFeatures,radius,1f), false, ImageSInt16.class));
		detectCorners("Median", FactoryDetectPoint.<ImageUInt8, ImageSInt16>
				createMedian(new ConfigGeneralDetector(maxFeatures,radius, 1), ImageUInt8.class));
	}

	private void createTestImage() {
		BufferedImage workImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_BGR);
		Graphics2D g2 = workImg.createGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, width, height);
		g2.setColor(Color.BLACK);
		addRectangle(g2, new AffineTransform(), 40, 50, 60, 50);

		AffineTransform tran = new AffineTransform();
		tran.setToRotation(0.5);
		addRectangle(g2, tran, 120, 140, 60, 50);

		tran.setToRotation(-1.2);
		addRectangle(g2, tran, -120, 200, 60, 40);

		ConvertBufferedImage.convertFrom(workImg, image);
		ImageMiscOps.addUniform(image, rand, -2, 2);
		ImageBorder_I32<ImageUInt8> border = (ImageBorder_I32)FactoryImageBorder.general(image,BoofDefaults.DERIV_BORDER_TYPE);
		GradientSobel.process(image, derivX, derivY, border);
	}

	private void addRectangle(Graphics2D g2, AffineTransform tran, int x0, int y0, int w, int h) {
		g2.setTransform(tran);
		g2.fillRect(x0, y0, w, h);

		// -1 is added for w and h because it is drawn before that point
		corners.add(new Point2D_F64(x0, y0));
		corners.add(new Point2D_F64(x0 + w - 1, y0));
		corners.add(new Point2D_F64(x0 + w - 1, y0 + h - 1));
		corners.add(new Point2D_F64(x0, y0 + h - 1));
		for (int i = corners.size() - 4; i < corners.size(); i++) {
			Point2D_F64 c = corners.get(i);
			Point2D src = new Point2D.Double(c.x, c.y);
			Point2D dst = new Point2D.Double();
			tran.transform(src, dst);
			c.x = dst.getX();
			c.y = dst.getY();
		}
	}

	protected void evaluate(QueueCorner foundCorners, ImageFloat32 intensity, String name) {


		ShowImages.showWindow(intensity, "Intensity of " + name, true);

		int numMatched = 0;
		double error = 0;

		for (Point2D_F64 c : corners) {
			double bestDistance = -1;
			Point2D_I16 bestPoint = null;

			for (int i = 0; i < foundCorners.size(); i++) {
				Point2D_I16 p = foundCorners.get(i);

				double dist = UtilPoint2D_F64.distance(c.x, c.y, p.x, p.y);

				if (bestPoint == null || dist < bestDistance) {
					bestDistance = dist;
					bestPoint = p;
				}
			}

			if (bestDistance <= distTol) {
				error += bestDistance;
				numMatched++;
			}
		}
		if (numMatched > 0) {
			error /= numMatched;

			System.out.println(name + " num matched corners: " + numMatched + "  average error " + error);
		} else {
			System.out.println(name + " no corner matches");
		}
	}

	public static void main(String args[]) {
		BenchmarkCornerAccuracy benchmark = new BenchmarkCornerAccuracy();

		benchmark.evaluateAll();
	}
}
