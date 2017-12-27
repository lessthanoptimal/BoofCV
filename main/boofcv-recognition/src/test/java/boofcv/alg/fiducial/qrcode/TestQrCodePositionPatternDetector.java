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

package boofcv.alg.fiducial.qrcode;


import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.shapes.polygon.DetectPolygonBinaryGrayRefine;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.metric.UtilAngle;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestQrCodePositionPatternDetector {
	@Test
	public void easy() {
		GrayF32 image = render(null,
				new PP(40,60,70), new PP(140,60,70),new PP(40,150,70));

		GrayU8 binary = new GrayU8(image.width,image.height);

		ThresholdImageOps.threshold(image,binary,100,true);

		QrCodePositionPatternDetector<GrayF32> alg = createAlg();

		alg.process(image,binary);

		List<PositionPatternNode> list = alg.getPositionPatterns().toList();
		assertEquals(3,list.size());

		checkNode(40+35,60+35,2,list);
		checkNode(140+35,60+35,1,list);
		checkNode(40+35,150+35,1,list);
	}

	private void checkNode( double cx , double cy , int numEdges ,
							List<PositionPatternNode> list )
	{
		for (int i = 0; i < list.size(); i++) {
			PositionPatternNode p = list.get(i);
			if( p.center.distance(cx,cy) < 3 ) {
				assertEquals(numEdges,p.getNumberOfConnections());
				return;
			}
		}
		fail("No match");
	}

	/**
	 * Simple positive example
	 */
	@Test
	public void considerConnect_positive() {
		QrCodePositionPatternDetector<GrayF32> alg = createAlg();

		SquareNode n0 = squareNode(40,60,70);
		SquareNode n1 = squareNode(140,60,70);

		alg.considerConnect(n0,n1);

		assertEquals(1,n0.getNumberOfConnections());
		assertEquals(1,n1.getNumberOfConnections());
	}

	/**
	 * The two patterns are rotated 45 degrees relative to each other
	 */
	@Test
	public void considerConnect_negative_rotated() {
		QrCodePositionPatternDetector<GrayF32> alg = createAlg();

		SquareNode n0 = squareNode(40,60,70);
		SquareNode n1 = squareNode(140,60,70);

		Se2_F64 translate = new Se2_F64(-175,-95,0);
		Se2_F64 rotate = new Se2_F64(0,0, UtilAngle.radian(45));

		Se2_F64 tmp = translate.concat(rotate,null);
		Se2_F64 combined = tmp.concat(translate.invert(null),null);

		for (int i = 0; i < 4; i++) {
			SePointOps_F64.transform(combined,n1.square.get(i),n1.square.get(i));
		}
		SePointOps_F64.transform(combined,n1.center,n1.center);

		alg.considerConnect(n0,n1);

		assertEquals(0,n0.getNumberOfConnections());
		assertEquals(0,n1.getNumberOfConnections());
	}

	@Test
	public void checkPositionPatternAppearance() {
		GrayF32 image = render(null,new PP(40,60,70));

		QrCodePositionPatternDetector<GrayF32> alg = createAlg();

		Polygon2D_F64 square = square(40,60,70);

		alg.interpolate.setImage(image);

		assertTrue(alg.checkPositionPatternAppearance(square,100));

		// fill in the inner "stone"
		ImageMiscOps.fillRectangle(image,0,40,60,70,70);
		assertFalse(alg.checkPositionPatternAppearance(square,100));
	}

	public static SquareNode squareNode( int x0 , int y0 , int width ) {
		Polygon2D_F64 square = square(x0,y0,width);

		SquareNode node = new SquareNode();
		node.square = square;
		node.largestSide = width;
		node.smallestSide = width;
		node.center.set(x0+width/2,y0+width/2);
		for (int i = 0; i < 4; i++) {
			node.sideLengths[i] = width;
		}

		return node;
	}

	public static Polygon2D_F64 square( int x0 , int y0 , int width ) {
		Polygon2D_F64 square = new Polygon2D_F64(4);
		square.get(0).set(x0,y0);
		square.get(1).set(x0+width,y0);
		square.get(2).set(x0+width,y0+width);
		square.get(3).set(x0,y0+width);
		return square;
	}

	@Test
	public void positionSquareIntensityCheck() {

		float positive[] = new float[]{10,200,10,10,10,200,10};

		assertTrue(QrCodePositionPatternDetector.positionSquareIntensityCheck(positive,100));

		for (int i = 0; i < 7; i++) {
			float negative[] = positive.clone();

			negative[i] = negative[i] < 100 ? 200 : 10;
			assertFalse(QrCodePositionPatternDetector.positionSquareIntensityCheck(negative,100));
		}
	}

	private QrCodePositionPatternDetector<GrayF32> createAlg() {

		ConfigPolygonDetector config = new ConfigPolygonDetector(4,4);
		config.detector.clockwise = false;
		DetectPolygonBinaryGrayRefine<GrayF32> squareDetector =
				FactoryShapeDetector.polygon(config, GrayF32.class);

		return new QrCodePositionPatternDetector<>(squareDetector,2);
	}

	private GrayF32 render(Affine2D_F64 affine , PP ...pps ) {

		BufferedImage image = new BufferedImage(300,400,BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = image.createGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0,0,image.getWidth(),image.getHeight());

		if( affine != null ) {
			g2.setTransform(new AffineTransform(affine.a11,affine.a12,affine.a21,affine.a22,affine.tx,affine.ty));
		}

		for( PP p : pps ) {
			renderPP(g2,p.x0,p.y0,p.width);
		}

		GrayF32 out = new GrayF32(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFrom(image,out);

//		ShowImages.showWindow(image,"Rendered", true);
//		try {
//			Thread.sleep(2000);
//		} catch (InterruptedException ignore) {}

		return out;
	}

	private void renderPP(Graphics2D g2 , int x0 , int y0 , int width ) {
		g2.setColor(Color.BLACK);
		g2.fillRect(x0,y0,width,width);
		g2.setColor(Color.WHITE);
		g2.fillRect(x0+width/7,y0+width/7,width*5/7,width*5/7);
		g2.setColor(Color.BLACK);
		g2.fillRect(x0+width*2/7,y0+width*2/7,width*3/7,width*3/7);
	}

	private static class PP {
		int x0,y0,width;

		public PP(int x0, int y0, int width) {
			this.x0 = x0;
			this.y0 = y0;
			this.width = width;
		}
	}

}