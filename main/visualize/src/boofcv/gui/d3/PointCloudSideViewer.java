/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.d3;

import boofcv.struct.FastQueue;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPoint3D_F64;
import georegression.struct.point.Point3D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Peter Abeles
 */
// todo support for ImageFloat32
// TODO add top control bar to select zoom and to reset to home
public class PointCloudSideViewer extends JPanel implements MouseListener , MouseMotionListener {
	FastQueue<ColorPoint3D> cloud = new FastQueue<ColorPoint3D>(200,ColorPoint3D.class,true);

	List<ColorPoint3D> orderedCloud = new ArrayList<ColorPoint3D>();
	// distance between the two camera centers
	double baseline;

	double focalLengthX;
	double focalLengthY;
	double centerX;
	double centerY;

	// minimum disparity
	int minDisparity;
	// maximum minus minimum disparity
	int rangeDisparity;

	double scale;

	double offsetX;
	double offsetY;

	// previous mouse location
	int prevX;
	int prevY;

	public PointCloudSideViewer() {
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void configure(double baseline,
						  double focalLengthX, double focalLengthY,
						  double centerX, double centerY,
						  int minDisparity, int maxDisparity) {
		this.baseline = baseline;
		this.focalLengthX = focalLengthX;
		this.focalLengthY = focalLengthY;
		this.centerX = centerX;
		this.centerY = centerY;
		this.minDisparity = minDisparity;

		this.rangeDisparity = maxDisparity-minDisparity;
	}
	public void process( ImageSingleBand disparity , BufferedImage color ) {
		if( disparity instanceof ImageUInt8 )
			process((ImageUInt8)disparity,color);
		else
			process((ImageFloat32)disparity,color);

		orderedCloud.clear();
		orderedCloud.addAll(cloud.toList());
		Collections.sort(orderedCloud,new Comparator<ColorPoint3D>() {
			@Override
			public int compare(ColorPoint3D o1, ColorPoint3D o2) {
				if( o1.y < o2.y )
					return 1;
				else if( o1.y == o2.y )
					return 0;
				else
					return -1;
			}
		});
	}

	private void process( ImageUInt8 disparity , BufferedImage color ) {

		scale = 0;

		cloud.reset();

		for( int y = 0; y < disparity.height; y++ ) {
			int index = disparity.startIndex + disparity.stride*y;

			for( int x = 0; x < disparity.width; x++ ) {
				int value = disparity.data[index++] & 0xFF;

				if( value >= rangeDisparity )
					continue;

				value += minDisparity;

				if( value == 0 )
					continue;

				ColorPoint3D p = cloud.pop();

				p.z = baseline*focalLengthX/value;
				p.x = p.z*(x - centerX)/focalLengthX;
				p.y = p.z*(y - centerY)/focalLengthY;
				p.rgb = color.getRGB(x,y);
			}
		}
	}

	private void process( ImageFloat32 disparity , BufferedImage color ) {

		scale = 0;

		cloud.reset();

		for( int y = 0; y < disparity.height; y++ ) {
			int index = disparity.startIndex + disparity.stride*y;

			for( int x = 0; x < disparity.width; x++ ) {
				float value = disparity.data[index++];

				if( value >= rangeDisparity )
					continue;

				value += minDisparity;

				if( value == 0 )
					continue;

				ColorPoint3D p = cloud.pop();

				p.z = baseline*focalLengthX/value;
				p.x = p.z*(x - centerX)/focalLengthX;
				p.y = p.z*(y - centerY)/focalLengthY;
				p.rgb = color.getRGB(x,y);
			}
		}
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		// auto set initial scale
		if( scale == 0 && getHeight() > 0 ) {
			double midRange = focalLengthY*baseline/(minDisparity+rangeDisparity*0.25);
			scale = getHeight()/midRange;
		}

		int centerX = getWidth()/2;
		int height = getHeight();

		Graphics2D g2 = (Graphics2D)g;

		for( int i = 0; i < orderedCloud.size(); i++ ) {
			ColorPoint3D p = orderedCloud.get(i);

			int x = (int)((p.x+offsetX)*scale ) + centerX;
			int y = height - (int)((p.z+offsetY)*scale) - 1;

			int r = 2 + (int)(0.01*p.z*scale);
			int w = r*2+1;

				g2.setColor(new Color(p.rgb));
				g2.fillOval(x-r,y-r,w,w);
		}
	}

	@Override
	public synchronized void mouseClicked(MouseEvent e) {

		if( e.isShiftDown())
			scale *= 0.75;
		else
			scale *= 1.5;

		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		prevX = e.getX();
		prevY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public synchronized void mouseDragged(MouseEvent e) {
		final int deltaX = e.getX()-prevX;
		final int deltaY = e.getY()-prevY;

		offsetX += deltaX/scale;
		offsetY += -deltaY/scale;

		prevX = e.getX();
		prevY = e.getY();

		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {}
}
