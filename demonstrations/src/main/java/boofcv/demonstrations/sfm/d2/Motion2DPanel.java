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

package boofcv.demonstrations.sfm.d2;

import boofcv.gui.feature.VisualizeFeatures;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Base class for visualizing 2d image stitching video demos
 *
 * @author Peter Abeles
 */
public abstract class Motion2DPanel extends JPanel
{
	// rendered distorted image
	BufferedImage stitched;
	// input image
	BufferedImage input;

	int windowWidth,windowHeight;
	int distortOffX;

	Homography2D_F64 currToWorld = new Homography2D_F64();

	// copies of feature location for GUI thread
	DogArray<Point2D_F64> inliers = new DogArray<>(300, Point2D_F64::new);
	DogArray<Point2D_F64> allTracks = new DogArray<>(300, Point2D_F64::new);

	boolean showImageView=true;
	boolean showAll=false;
	boolean showInliers=false;

	Quadrilateral_F64 corners = new Quadrilateral_F64();

	BasicStroke boundsStroke = new BasicStroke(5.0f);

	public void setImages(BufferedImage input , BufferedImage stitched)
	{
		this.input = input;
		this.stitched = stitched;
	}

	public void setCorners(Quadrilateral_F64 corners) {
		this.corners.setTo(corners);
	}

	public void setInliers(java.util.List<Point2D_F64> list) {
		inliers.reset();
		inliers.copyAll(list,(s,d)->d.setTo(s));
	}

	public synchronized void setAllTracks(@Nullable java.util.List<Point2D_F64> list) {
		allTracks.reset();
		allTracks.copyAll(list,(s,d)->d.setTo(s));
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		if( stitched == null )
			return;

		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int w = getWidth();
		int h = getHeight();

		double scaleX = w/(double) windowWidth;
		double scaleY = h/(double) windowHeight;

		double scale = Math.min(scaleX,scaleY);
		if( scale > 1 ) scale = 1;

		drawImages(scale,g2);

		drawFeatures((float)scale,g2);
//
		if(showImageView)
			drawImageBounds(g2,distortOffX,0,scale);
	}

	protected abstract void drawImages( double scale , Graphics2D g2 );

	protected abstract void drawFeatures( float scale, Graphics2D g2  );

	/**
	 * Draw features after applying a homography transformation.
	 */
	protected void drawFeatures( float scale , int offsetX , int offsetY ,
								 DogArray<Point2D_F64> all,
								 DogArray<Point2D_F64> inliers,
								 Homography2D_F64 currToGlobal, Graphics2D g2 ) {

		Point2D_F64 distPt = new Point2D_F64();

		if (showAll) {
			drawPoints(scale, offsetX, offsetY, all, currToGlobal, g2, distPt, Color.RED);
		}

		if (showInliers) {
			drawPoints(scale, offsetX, offsetY, inliers, currToGlobal, g2, distPt, Color.BLUE);
		}
	}

	private void drawPoints( float scale, int offsetX, int offsetY, DogArray<Point2D_F64> all, Homography2D_F64 currToGlobal, Graphics2D g2, Point2D_F64 distPt, Color red ) {
		for (int i = 0; i < all.size; i++) {
			HomographyPointOps_F64.transform(currToGlobal, all.get(i), distPt);

			distPt.x = offsetX + distPt.x*scale;
			distPt.y = offsetY + distPt.y*scale;

			VisualizeFeatures.drawPoint(g2, (int)distPt.x, (int)distPt.y, red);
		}
	}

	private void drawImageBounds( Graphics2D g2 , int tx , int ty , double scale ) {
		Quadrilateral_F64 c = corners;

		Stroke originalStroke = g2.getStroke();
		g2.setStroke(boundsStroke);
		g2.setColor(Color.BLUE);
		drawLine(g2,tx,ty,scale,c.a,c.b);
		drawLine(g2,tx,ty,scale,c.b,c.c);
		drawLine(g2,tx,ty,scale,c.c,c.d);
		drawLine(g2,tx,ty,scale,c.d,c.a);
		g2.setStroke(originalStroke);
	}

	private void drawLine( Graphics2D g2 , int tx , int ty , double scale , Point2D_F64 p0 , Point2D_F64 p1 )
	{
		g2.drawLine((int)(p0.x*scale)+tx,(int)(p0.y*scale)+ty,(int)(p1.x*scale)+tx,(int)(p1.y*scale)+ty);
	}

	public void setCurrToWorld(Homography2D_F64 currToWorld) {
		this.currToWorld.setTo(currToWorld);
	}

	public void setShowImageView(boolean showImageView) {
		this.showImageView = showImageView;
	}
}
