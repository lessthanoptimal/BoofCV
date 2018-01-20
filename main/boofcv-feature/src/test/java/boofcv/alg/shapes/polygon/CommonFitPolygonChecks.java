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

package boofcv.alg.shapes.polygon;

import boofcv.abst.distort.FDistort;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import georegression.transform.affine.AffinePointOps_F64;
import org.junit.Before;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Base class for unit tests which fit binary polygons
 *
 * @author Peter Abeles
 */
public class CommonFitPolygonChecks {

	Random rand = new Random(234);
	boolean showRendered = false;

	int width = 400, height = 500;

	Class imageTypes[] = new Class[]{GrayU8.class,GrayF32.class};
	ImageGray orig; // original image before affine has been applied
	ImageGray image; // image after affine applied

	int x0,y0,x1,y1;

	int white;
	int black;

	boolean fittingToBinaryImage = false; // adjust to squars to make the binary image fit work better

	// list of rectangles as rendered before distortion is applied
	List<Rectangle2D_I32> rectangles = new ArrayList<>();
	// The rectangle or polygon after distortion hsa been applied
	List<Polygon2D_F64> distorted = new ArrayList<>();

	Affine2D_F64 transform = new Affine2D_F64();

	double matchError;

	@Before
	public void resetSettings() {
		transform.reset();
		width = 400; height = 500;
		rectangles.clear();
		transform.reset();

		x0 = 200; y0 = 160;
		x1 = 260; y1 = 400;

		white = 200;
		black = 5;
	}

	public void renderDistortedRectangles( boolean blackShape , Class imageType ) {
		orig = GeneralizedImageOps.createSingleBand(imageType,width,height);
		image = GeneralizedImageOps.createSingleBand(imageType,width,height);

		int white = blackShape ? this.white : this.black;
		int black = blackShape ? this.black : this.white;

		GImageMiscOps.fill(orig, white);
		GImageMiscOps.fill(image, white);

		distorted.clear();
		for (Rectangle2D_I32 q : rectangles) {

			if(fittingToBinaryImage)
				GImageMiscOps.fillRectangle(orig,black,q.x0,q.y0,q.x1-q.x0+1,q.y1-q.y0+1);
			else
				GImageMiscOps.fillRectangle(orig,black,q.x0,q.y0,q.x1-q.x0,q.y1-q.y0);

			Polygon2D_F64 tran = new Polygon2D_F64(4);

			AffinePointOps_F64.transform(transform,q.x0,q.y0,tran.get(0));
			AffinePointOps_F64.transform(transform,q.x0,q.y1,tran.get(1));
			AffinePointOps_F64.transform(transform,q.x1,q.y1,tran.get(2));
			AffinePointOps_F64.transform(transform,q.x1,q.y0,tran.get(3));

			distorted.add(tran);
		}

		new FDistort(orig,image).border(white).affine(transform).apply();

		if( showRendered ) {
			ListDisplayPanel panel = new ListDisplayPanel();
			panel.addImage(orig, "Original");
			panel.addImage(image, "Image");

			ShowImages.showWindow(panel,"Rendered");
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void renderPolygons( List<Polygon2D_F64> polygons, Class imageType ) {
		BufferedImage work = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = work.createGraphics();

		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, width, height);
		g2.setColor(Color.BLACK);

		distorted.clear();
		for (int i = 0; i < polygons.size(); i++) {
			Polygon2D_F64 orig = polygons.get(i);

			int x[] = new int[ orig.size() ];
			int y[] = new int[ orig.size() ];

			for (int j = 0; j < orig.size(); j++) {
				x[j] = (int)orig.get(j).x;
				y[j] = (int)orig.get(j).y;
			}

			g2.fillPolygon(x,y,orig.size());

			distorted.add( orig );
		}

		orig = null;
		image = GeneralizedImageOps.createSingleBand(imageType,width,height);

		ConvertBufferedImage.convertFrom(work,image,true);

		if( showRendered ) {
			ListDisplayPanel panel = new ListDisplayPanel();
			panel.addImage(work, "Work");
			panel.addImage(image, "Image");
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Compare found rectangle against rectangles in the original undistorted image
	 */
	protected int findMatchesOriginal(Polygon2D_F64 found, double tol) {
		int match = 0;
		for (int i = 0; i < rectangles.size(); i++) {
			Rectangle2D_I32 ri = rectangles.get(i);
			Rectangle2D_F64 r = new Rectangle2D_F64(ri.x0,ri.y0,ri.x1,ri.y1);
			Polygon2D_F64 p = new Polygon2D_F64(4);
			UtilPolygons2D_F64.convert(r,p);
			if( p.isCCW() )
				p.flip();

			if(UtilPolygons2D_F64.isEquivalent(found,p,tol))
				match++;
		}
		return match;
	}

	protected Polygon2D_F64 apply( Affine2D_F64 affine , Polygon2D_F64 input ) {
		Polygon2D_F64 out = new Polygon2D_F64(input.size());

		for (int i = 0; i < input.size(); i++) {
			AffinePointOps_F64.transform(affine, input.get(i), out.get(i));
		}

		return out;
	}

	public static boolean checkPolygon( double[] expected , Polygon2D_F64 found  ) {
		for (int i = 0; i < found.size(); i++) {
			boolean matched = true;
			for (int j = 0; j < found.size(); j++) {
				double x = expected[j*2];
				double y = expected[j*2+1];

				Point2D_F64 p = found.get((i+j)%found.size());

				if( Math.abs(p.x-x) > 1e-5 || Math.abs(p.y-y) > 1e-5 ) {
					matched = false;
					break;
				}
			}
			if( matched )
				return true;
		}
		return false;
	}

	protected Polygon2D_F64 createFromSquare( Affine2D_F64 affine ) {
		Polygon2D_F64 input = new Polygon2D_F64(4);
		if( affine != null ) {
			AffinePointOps_F64.transform(affine, new Point2D_F64(x0, y0), input.get(0));
			AffinePointOps_F64.transform(affine, new Point2D_F64(x0, y1), input.get(1));
			AffinePointOps_F64.transform(affine, new Point2D_F64(x1, y1), input.get(2));
			AffinePointOps_F64.transform(affine, new Point2D_F64(x1, y0), input.get(3));
		} else {
			input.get(0).set(new Point2D_F64(x0, y0));
			input.get(1).set(new Point2D_F64(x0, y1));
			input.get(2).set(new Point2D_F64(x1, y1));
			input.get(3).set(new Point2D_F64(x1, y0));
		}
		return input;
	}

	protected int findMatches( Polygon2D_F64 found , double tol ) {
		int match = 0;
		for (int i = 0; i < distorted.size(); i++) {
			// contour fitting should only be pixel precise.
			if(UtilPolygons2D_F64.isEquivalent(found, distorted.get(i),tol)) {
				matchError = computeError(found, distorted.get(i));
				match++;
			}
		}
		return match;
	}

	protected double computeError( Polygon2D_F64 a , Polygon2D_F64 b ) {
		double best = Double.MAX_VALUE;

		int N = a.vertexes.size;

		for (int i = 0; i < N; i++) {
			double error = 0;
			for (int j = 0; j < N; j++) {
				error += a.get(j).distance(b.get((i+j)%N));
			}
			if( error < best )
				best = error;
		}
		return best/4;
	}

	protected void addNoise(Polygon2D_F64 input, double spread) {
		for (int i = 0; i < input.size(); i++) {
			Point2D_F64 v = input.get(i);
			v.x += rand.nextDouble()*spread - spread/2.0;
			v.y += rand.nextDouble()*spread - spread/2.0;
		}
	}
}
