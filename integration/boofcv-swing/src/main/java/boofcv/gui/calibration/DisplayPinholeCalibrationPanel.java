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

package boofcv.gui.calibration;

import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.core.image.border.BorderType;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays information images of planar calibration targets.
 *
 * @author Peter Abeles
 */
public class DisplayPinholeCalibrationPanel extends DisplayCalibrationPanel<CameraPinholeRadial> {

	// which image is being displayed
	int selectedImage;
	// for displaying undistorted image
	BufferedImage distorted;
	BufferedImage undistorted;
	// true if the image has been undistorted
	boolean isUndistorted = false;

	// for displaying corrected image
	Planar<GrayF32> origMS = new Planar<>(GrayF32.class,1,1,3);
	Planar<GrayF32> correctedMS = new Planar<>(GrayF32.class,1,1,3);

	ImageDistort<GrayF32,GrayF32> undoRadial;
	Point2Transform2_F32 remove_p_to_p;

	// int horizontal line
	int lineY=-1;

	@Override
	public void setBufferedImage(BufferedImage image) {
		this.distorted = image;

		undoRadialDistortion(distorted);
	}

	@Override
	public void paintComponent(Graphics g) {
		if( showUndistorted ) {
			this.img = undistorted;
		} else {
			this.img = distorted;
		}

		super.paintComponent(g);
	}

	@Override
	protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
		if( features != null && features.size() > selectedImage ) {
			drawFeatures(g2, scale);
		}

		if( lineY > -1 ) {
			g2.setColor(Color.RED);
			g2.setStroke(new BasicStroke(3));
			g2.drawLine(0,lineY,getWidth(),lineY);
		}
	}

	private void undoRadialDistortion(BufferedImage image) {
		if( undoRadial == null )
			return;

		ConvertBufferedImage.convertFrom(image,origMS,true);
		if( correctedMS.getNumBands() != origMS.getNumBands() )
			correctedMS.setNumberOfBands(origMS.getNumBands());
		correctedMS.reshape(origMS.width,origMS.height);

		for( int i = 0; i < origMS.getNumBands(); i++ ) {
			GrayF32 in = origMS.getBand(i);
			GrayF32 out = correctedMS.getBand(i);

			undoRadial.apply(in,out);
		}
		undistorted = ConvertBufferedImage.checkDeclare(origMS.width,origMS.height,undistorted,BufferedImage.TYPE_INT_RGB);

		ConvertBufferedImage.convertTo(correctedMS,undistorted,true);
	}

	private void drawFeatures(Graphics2D g2 , double scale) {

		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		CalibrationObservation set = features;

		Point2D_F32 adj = new Point2D_F32();

		if( showOrder ) {
			List<Point2D_F64> adjusted;
			if( showUndistorted ) {
				adjusted = new ArrayList<>();
				for( PointIndex2D_F64 p : set.points ) {
					remove_p_to_p.compute((float)p.x,(float)p.y,adj);
					adjusted.add(new Point2D_F64(adj.x,adj.y));
				}
			} else {
				adjusted = (List)set.points;
			}
			renderOrder(g2,scale, adjusted);
		}

		if( showPoints ) {
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(3));
			for( PointIndex2D_F64 p : set.points ) {
				if( showUndistorted ) {
					remove_p_to_p.compute((float)p.x,(float)p.y,adj);
				} else {
					adj.set((float)p.x,(float)p.y);
				}
				VisualizeFeatures.drawCross(g2, adj.x*scale, adj.y*scale, 4);
			}
			g2.setStroke(new BasicStroke(1));
			g2.setColor(Color.RED);
			for( PointIndex2D_F64 p : set.points ) {
				if( showUndistorted ) {
					remove_p_to_p.compute((float)p.x,(float)p.y,adj);
				} else {
					adj.set((float)p.x,(float)p.y);
				}
				VisualizeFeatures.drawCross(g2, adj.x*scale, adj.y*scale, 4);
			}
		}

		if( showAll ) {
			for( CalibrationObservation l : allFeatures ) {
				for( PointIndex2D_F64 p : l.points ) {
					if( showUndistorted ) {
						remove_p_to_p.compute((float)p.x,(float)p.y,adj);
					} else {
						adj.set((float)p.x,(float)p.y);
					}
					VisualizeFeatures.drawPoint(g2,adj.x*scale,adj.y*scale,2,Color.BLUE,false);
				}
			}
		}

		if( showNumbers ) {
			if( showUndistorted )
				drawNumbers(g2, set,remove_p_to_p,scale);
			else
				drawNumbers(g2, set,null,scale);
		}

		if( showErrors && results != null ) {


			for( int i = 0; i < set.size(); i++ ) {
				PointIndex2D_F64 p = set.get(i);

				if( showUndistorted ) {
					remove_p_to_p.compute((float)p.x,(float)p.y,adj);
				} else {
					adj.set((float)p.x,(float)p.y);
				}

				double r = scale*errorScale*results.pointError[i];
				if( r < 1 )
					continue;

				g2.setStroke(new BasicStroke(4));
				g2.setColor(Color.BLACK);
				VisualizeFeatures.drawCircle(g2, adj.x * scale, adj.y * scale, r);

				g2.setStroke(new BasicStroke(2.5f));
				g2.setColor(Color.ORANGE);
				VisualizeFeatures.drawCircle(g2, adj.x * scale, adj.y * scale, r);
			}
		}
	}

	public static void renderOrder(Graphics2D g2, double scale , List<Point2D_F64> points ) {
		g2.setStroke(new BasicStroke(5));

		Line2D.Double l = new Line2D.Double();

		for (int i = 0,j = 1; j < points.size(); i=j,j++) {
			Point2D_F64 p0 = points.get(i);
			Point2D_F64 p1 = points.get(j);

			double fraction = i / ((double) points.size() - 2);
//			fraction = fraction * 0.8 + 0.1;

			int red   = (int)(0xFF*fraction) + (int)(0x00*(1-fraction));
			int green = 0x00;
			int blue  = (int)(0x00*fraction) + (int)(0xff*(1-fraction));

			int lineRGB = red << 16 | green << 8 | blue;

			l.setLine(scale * p0.x , scale * p0.y, scale * p1.x, scale * p1.y );

			g2.setColor(new Color(lineRGB));
			g2.draw(l);
		}
	}

	@Override
	public void setCalibration (CameraPinholeRadial param  ) {
		CameraPinhole undistorted = new CameraPinhole(param);
		this.undoRadial = LensDistortionOps.changeCameraModel(
				AdjustmentType.FULL_VIEW, BorderType.ZERO, param, undistorted,null, ImageType.single(GrayF32.class));
		this.remove_p_to_p = LensDistortionOps.transformChangeModel_F32(AdjustmentType.FULL_VIEW, param, undistorted, false,null);

		undoRadialDistortion(distorted);
	}

	public void setCalibration (CameraPinholeRadial param , DMatrixRMaj rect ) {
		FMatrixRMaj rect_f32 = new FMatrixRMaj(3,3);
		ConvertMatrixData.convert(rect,rect_f32);

		this.undoRadial = RectifyImageOps.rectifyImage(
				param, rect_f32, BorderType.ZERO, ImageType.single(GrayF32.class));
		this.remove_p_to_p = RectifyImageOps.transformPixelToRect(param, rect_f32);
	}

	public void setLine( int y ) {
		this.lineY = y;
	}

	public static void drawNumbers( Graphics2D g2 , CalibrationObservation foundTarget ,
									Point2Transform2_F32 transform ,
									double scale ) {

		Font regular = new Font("Serif", Font.PLAIN, 16);
		g2.setFont(regular);

		Point2D_F32 adj = new Point2D_F32();

		AffineTransform origTran = g2.getTransform();
		for( int i = 0; i < foundTarget.size(); i++ ) {
			Point2D_F64 p = foundTarget.get(i);
			int gridIndex = foundTarget.get(i).index;

			if( transform != null ) {
				transform.compute((float)p.x,(float)p.y,adj);
			} else {
				adj.set((float)p.x,(float)p.y);
			}

			String text = String.format("%2d",gridIndex);

			int x = (int)(adj.x*scale);
			int y = (int)(adj.y*scale);

			g2.setColor(Color.BLACK);
			g2.drawString(text,x-1,y);
			g2.drawString(text,x+1,y);
			g2.drawString(text,x,y-1);
			g2.drawString(text,x,y+1);
			g2.setTransform(origTran);
			g2.setColor(Color.GREEN);
			g2.drawString(text,x,y);
		}
	}
}
