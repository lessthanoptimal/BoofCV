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

package boofcv.gui.calibration;

import boofcv.abst.geo.calibration.ImageResults;
import boofcv.alg.distort.AdjustmentType;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.core.image.border.BorderType;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DenseMatrix64F;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays information images of planar calibration targets.
 *
 * @author Peter Abeles
 */
public class CalibratedImageGridPanel extends JPanel {

	// images of calibration target
	List<BufferedImage> images;
	// which image is being displayed
	int selectedImage;
	// for displaying undistorted image
	BufferedImage undistorted;
	// true if the image has been undistorted
	boolean isUndistorted = false;

	// observed feature locations
	List<CalibrationObservation> features = new ArrayList<CalibrationObservation>();
	// results of calibration
	List<ImageResults> results = new ArrayList<ImageResults>();

	// for displaying corrected image
	MultiSpectral<ImageFloat32> origMS;
	MultiSpectral<ImageFloat32> correctedMS;

	// configures what is displayed or not
	boolean showPoints = true;
	boolean showErrors = true;
	boolean showUndistorted = false;
	boolean showAll = false;
	boolean showNumbers = true;

	ImageDistort<ImageFloat32,ImageFloat32> undoRadial;
	PointTransform_F32 remove_p_to_p;

	// how much errors are scaled up
	double errorScale;

	// int horizontal line
	int lineY=-1;

	public void setDisplay( boolean showPoints , boolean showErrors ,
							boolean showUndistorted , boolean showAll , boolean showNumbers ,
							double errorScale )
	{
		this.showPoints = showPoints;
		this.showErrors = showErrors;
		this.showUndistorted = showUndistorted;
		this.showAll = showAll;
		this.showNumbers = showNumbers;
		this.errorScale = errorScale;
	}

	public void setSelected( int selected ) {
		this.selectedImage = selected;
		this.isUndistorted = false;

		if( origMS == null ) {
			BufferedImage image = images.get(selected);

			// the number of bands can be difficult to ascertain without digging deep into the data structure
			// so just declare a new one using convert
			origMS = ConvertBufferedImage.convertFromMulti(image,null,true,ImageFloat32.class);
			correctedMS = ConvertBufferedImage.convertFromMulti(image,null,true,ImageFloat32.class);
			undistorted = new BufferedImage(image.getWidth(),image.getHeight(),image.getType());
		}
	}

	public void setImages( List<BufferedImage> images ) {
		this.images = images;
	}

	public void setResults( List<CalibrationObservation> features , List<ImageResults> results ) {
		this.features = features;
		this.results = results;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;

		if( images == null || selectedImage >= images.size() )
			return;

		BufferedImage image = images.get(selectedImage);

		double scaleX = getWidth()/(double)image.getWidth();
		double scaleY = getHeight()/(double)image.getHeight();
		double scale = Math.min(1,Math.min(scaleX,scaleY));

		AffineTransform tranOrig = g2.getTransform();
		AffineTransform tran = g2.getTransform();
		tran.concatenate(AffineTransform.getScaleInstance(scale,scale));

		g2.setTransform(tran);

		if( showUndistorted) {
			if( undoRadial != null && !isUndistorted ) {
				undoRadialDistortion(image);
				isUndistorted = true;
			}
			g2.drawImage(undistorted,0,0,null);
		} else
			g2.drawImage(image,0,0,null);

		g2.setTransform(tranOrig);

		if( features.size() > selectedImage ) {
			drawFeatures(g2, scale);
		}

		if( lineY > -1 ) {
			g2.setColor(Color.RED);
			g2.setStroke(new BasicStroke(3));
			g2.drawLine(0,lineY,getWidth(),lineY);
		}
	}

	private void undoRadialDistortion(BufferedImage image) {
		ConvertBufferedImage.convertFromMulti(image, origMS,true, ImageFloat32.class);

		for( int i = 0; i < origMS.getNumBands(); i++ ) {
			ImageFloat32 in = origMS.getBand(i);
			ImageFloat32 out = correctedMS.getBand(i);

			undoRadial.apply(in,out);
		}
		if( correctedMS.getNumBands() == 3 )
			ConvertBufferedImage.convertTo(correctedMS,undistorted,true);
		else if( correctedMS.getNumBands() == 1 )
			ConvertBufferedImage.convertTo(correctedMS.getBand(0),undistorted);
		else
			throw new RuntimeException("What kind of image has "+correctedMS.getNumBands()+"???");
	}

	private void drawFeatures(Graphics2D g2 , double scale) {

		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		CalibrationObservation set = features.get(selectedImage);

		Point2D_F32 adj = new Point2D_F32();

		if( showPoints ) {

			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(3));
			for( Point2D_F64 p : set.observations ) {
				if( showUndistorted ) {
					remove_p_to_p.compute((float)p.x,(float)p.y,adj);
				} else {
					adj.set((float)p.x,(float)p.y);
				}
				VisualizeFeatures.drawCross(g2, adj.x*scale, adj.y*scale, 4);
			}
			g2.setStroke(new BasicStroke(1));
			g2.setColor(Color.RED);
			for( Point2D_F64 p : set.observations ) {
				if( showUndistorted ) {
					remove_p_to_p.compute((float)p.x,(float)p.y,adj);
				} else {
					adj.set((float)p.x,(float)p.y);
				}
				VisualizeFeatures.drawCross(g2, adj.x*scale, adj.y*scale, 4);
			}
		}

		if( showAll ) {
			for( CalibrationObservation l : features ) {
				for( Point2D_F64 p : l.observations ) {
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

		if( showErrors && results != null && results.size() > selectedImage ) {
			ImageResults result = results.get(selectedImage);

			Stroke before = g2.getStroke();
			g2.setStroke(new BasicStroke(4));
			g2.setColor(Color.BLACK);
			for( int i = 0; i < set.size(); i++ ) {
				Point2D_F64 p = set.observations.get(i);

				if( showUndistorted ) {
					remove_p_to_p.compute((float)p.x,(float)p.y,adj);
				} else {
					adj.set((float)p.x,(float)p.y);
				}

				double r = errorScale*result.pointError[i];
				if( r < 1 )
					continue;

				VisualizeFeatures.drawCircle(g2, adj.x * scale, adj.y * scale, r);
			}

			g2.setStroke(before);
			g2.setColor(Color.ORANGE);
			for( int i = 0; i < set.size(); i++ ) {
				Point2D_F64 p = set.observations.get(i);

				if( showUndistorted ) {
					remove_p_to_p.compute((float)p.x,(float)p.y,adj);
				} else {
					adj.set((float)p.x,(float)p.y);
				}

				double r = errorScale*result.pointError[i];
				if( r < 1 )
					continue;


				VisualizeFeatures.drawCircle(g2, adj.x * scale, adj.y * scale, r);
			}
		}
	}

	public void setDistorted ( IntrinsicParameters param , DenseMatrix64F rect ) {
		if( rect == null ) {
			this.undoRadial = LensDistortionOps.imageRemoveDistortion(
					AdjustmentType.FULL_VIEW, BorderType.VALUE, param, null, ImageType.single(ImageFloat32.class));
			this.remove_p_to_p = LensDistortionOps.transform_F32(AdjustmentType.FULL_VIEW, param, null, false);
		} else {
			this.undoRadial = RectifyImageOps.rectifyImage(param, rect, BorderType.VALUE, ImageFloat32.class);
			this.remove_p_to_p = RectifyImageOps.transformPixelToRect_F32(param, rect);
		}
	}

	public void setLine( int y ) {
		this.lineY = y;
	}

	public static void drawNumbers( Graphics2D g2 , CalibrationObservation foundTarget ,
									PointTransform_F32 transform ,
									double scale ) {

		Font regular = new Font("Serif", Font.PLAIN, 16);
		g2.setFont(regular);

		Point2D_F32 adj = new Point2D_F32();

		AffineTransform origTran = g2.getTransform();
		for( int i = 0; i < foundTarget.size(); i++ ) {
			Point2D_F64 p = foundTarget.observations.get(i);
			int gridIndex = foundTarget.indexes.get(i);

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
