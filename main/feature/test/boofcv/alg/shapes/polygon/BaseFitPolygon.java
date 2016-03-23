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

package boofcv.alg.shapes.polygon;

import boofcv.abst.distort.FDistort;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.transform.affine.AffinePointOps_F64;
import org.junit.Before;

import java.util.Random;

/**
 * Base class for unit tests which fit binary polygons
 *
 * @author Peter Abeles
 */
public class BaseFitPolygon {
	boolean showRendered = false;

	Random rand = new Random(234);

	int width = 400, height = 500;
	ImageGray work; // original image before affine has been applied
	ImageGray image; // image after affine applied

	int x0 = 200, y0 = 160;
	int x1 = 260, y1 = 400; // that's exclusive

	int white = 200;

	Class imageTypes[] = new Class[]{GrayU8.class,GrayF32.class};

	@Before
	public void resetSettings() {
		width = 400; height = 500;

		x0 = 200; y0 = 160;
		x1 = 260; y1 = 400;

		white = 200;
	}

	/**
	 *
	 * @param affine affine transform to apply.  can be null for none
	 * @param black true = black rectangle and white background.  false = reverse
	 * @param imageType Type of image
	 */
	protected void setup( Affine2D_F64 affine, boolean black , Class imageType ) {
		work = GeneralizedImageOps.createSingleBand(imageType, width, height);
		image = GeneralizedImageOps.createSingleBand(imageType,width,height);

		int bg = black ? white : 0;
		int fg = black ? 0 : white;
		GImageMiscOps.fill(work, bg);
		GImageMiscOps.fillRectangle(work, fg, x0, y0, x1 - x0, y1 - y0);

		if( affine != null ) {
			new FDistort(work, image).border(bg).affine(affine).apply();
		} else {
			image.setTo(work);
		}

		if( showRendered ) {
			ListDisplayPanel panel = new ListDisplayPanel();
			panel.addImage(work, "Work");
			panel.addImage(image, "Image");

			ShowImages.showWindow(panel,"Rendered");
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	protected Polygon2D_F64 apply( Affine2D_F64 affine , Polygon2D_F64 input ) {
		Polygon2D_F64 out = new Polygon2D_F64(input.size());

		for (int i = 0; i < input.size(); i++) {
			AffinePointOps_F64.transform(affine, input.get(i), out.get(i));
		}

		return out;
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
}
