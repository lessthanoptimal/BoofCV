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

package boofcv.gui.feature;

import boofcv.gui.BoofSwingUtil;
import boofcv.struct.geo.AssociatedTriple;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DGrowArray;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

/**
 * Visualizes associations between three views.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class AssociatedTriplePanel extends JPanel {

	// lock for all the data structures below
	final Object lock = new Object();

	BufferedImage image1, image2, image3;
	DGrowArray points = new DGrowArray();

	// color of each points. Randomly select at runtime
	Color[] colors;

	// separation between images
	int sep = 10;

	// pixel offset in original coordinates
	double offX, offY;

	Affine2D_F64 affine1 = new Affine2D_F64();
	Affine2D_F64 affine2 = new Affine2D_F64();
	Affine2D_F64 affine3 = new Affine2D_F64();

	Point2D_F64 p1 = new Point2D_F64();
	Point2D_F64 p2 = new Point2D_F64();
	Point2D_F64 p3 = new Point2D_F64();

	Mode mode = Mode.POINTS;
	int singlePoint = -1;

	public AssociatedTriplePanel() {
		addMouseListener(new MouseHandler());
	}

	public void setImages( BufferedImage image1, BufferedImage image2, BufferedImage image3 ) {
		synchronized (lock) {
			this.image1 = image1;
			this.image2 = image2;
			this.image3 = image3;

			// set the size so that it will fit inside the display
			int w = image2.getWidth() + sep + image3.getWidth();
			int h = Math.max(image2.getHeight(), image3.getHeight()) + sep + image1.getHeight();
			double scale = BoofSwingUtil.selectZoomToFitInDisplay(w, h);
			setPreferredSize(new Dimension((int)(w*scale), (int)(h*scale)));
			setMaximumSize(new Dimension(w, h));
		}
	}

	public void setAssociation( List<AssociatedTriple> triples ) {
		Random rand = new Random(234);

		synchronized (lock) {
			points.reshape(triples.size()*6);
			colors = new Color[triples.size()];

			int idx = 0;
			for (int i = 0; i < triples.size(); i++) {
				AssociatedTriple a = triples.get(i);
				points.data[idx++] = a.p1.x;
				points.data[idx++] = a.p1.y;
				points.data[idx++] = a.p2.x;
				points.data[idx++] = a.p2.y;
				points.data[idx++] = a.p3.x;
				points.data[idx++] = a.p3.y;

				colors[i] = new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
			}
		}
	}

	@Override
	public synchronized void paintComponent( Graphics g ) {
		super.paintComponent(g);

		BufferedImage image1, image2, image3;

		// see if the images have been set. If so save the reference to the images. If setImages is called
		// it will have new references and not change the images themselves.
		synchronized (lock) {
			if (this.image1 == null || this.image2 == null || this.image3 == null)
				return;
			image1 = this.image1;
			image2 = this.image2;
			image3 = this.image3;
		}

		Graphics2D g2 = BoofSwingUtil.antialiasing(g);

		int sep = 10;
		int w = image2.getWidth() + sep + image3.getWidth();
		int h = Math.max(image2.getHeight(), image3.getHeight()) + sep + image1.getHeight();

		double scale = Math.min(Math.min(getWidth()/(double)w, getHeight()/(double)h), 1);

		// draw first image middle of the top
		int x0 = (int)(getWidth()/2 - image1.getWidth()*scale*0.5);
		int x1 = (int)(x0 + image1.getWidth()*scale);
		int y1 = (int)(image1.getHeight()*scale);
		g2.drawImage(image1, x0, 0, x1, y1, 0, 0, image1.getWidth(), image1.getHeight(), null);

		// second image bottom left
		int x2 = (int)(getWidth()/2 - w*scale*0.5);
		int x3 = x2 + (int)(image2.getWidth()*scale);
		int y2 = y1 + sep;
		int y3 = y2 + (int)(image2.getHeight()*scale);

		g2.drawImage(image2, x2, y2, x3, y3, 0, 0, image2.getWidth(), image2.getHeight(), null);

		// third image bottom right

		int x4 = x3 + sep;
		int x5 = x4 + (int)(image3.getWidth()*scale);
		int y4 = y2 + (int)(image3.getHeight()*scale);

		g2.drawImage(image3, x4, y2, x5, y4, 0, 0, image3.getWidth(), image3.getHeight(), null);

		// Define affine transform for each image
		affine1.setTo(scale, 0, 0, scale, x0, 0);
		affine2.setTo(scale, 0, 0, scale, x2, y2);
		affine3.setTo(scale, 0, 0, scale, x4, y2);

		synchronized (lock) {
			if (singlePoint >= 0 && singlePoint < colors.length) {
				drawSingleLine(g2);
			} else {
				switch (mode) {
					case NONE:
						break;
					case LINES:
						drawLines(g2);
						break;
					case POINTS:
						drawPoints(g2);
				}
			}
		}
	}

	private void drawSingleLine( Graphics2D g2 ) {
		g2.setStroke(new BasicStroke(4));
		Line2D.Double line = new Line2D.Double();

		int i = singlePoint*6;
		p1.setTo(offX + points.data[i], offY + points.data[i + 1]);
		p2.setTo(offX + points.data[i + 2], offY + points.data[i + 3]);
		p3.setTo(offX + points.data[i + 4], offY + points.data[i + 5]);

		transform(affine1, p1);
		transform(affine2, p2);
		transform(affine3, p3);

		g2.setColor(colors[i/6]);
		line.x1 = p1.x;
		line.y1 = p1.y;
		line.x2 = p2.x;
		line.y2 = p2.y;
		g2.draw(line);
		line.x2 = p3.x;
		line.y2 = p3.y;
		g2.draw(line);
		line.x1 = p2.x;
		line.y1 = p2.y;
		g2.draw(line);
	}

	private void drawLines( Graphics2D g2 ) {
		// not drawing anti aliased lines because it's slow

		g2.setStroke(new BasicStroke(1));
		Line2D.Double line = new Line2D.Double();
		for (int i = 0; i < points.length; i += 6) {
			p1.setTo(offX + points.data[i], offY + points.data[i + 1]);
			p2.setTo(offX + points.data[i + 2], offY + points.data[i + 3]);
			p3.setTo(offX + points.data[i + 4], offY + points.data[i + 5]);

			transform(affine1, p1);
			transform(affine2, p2);
			transform(affine3, p3);

			g2.setColor(colors[i/6]);
			line.x1 = p1.x;
			line.y1 = p1.y;
			line.x2 = p2.x;
			line.y2 = p2.y;
			g2.draw(line);
			line.x2 = p3.x;
			line.y2 = p3.y;
			g2.draw(line);
			line.x1 = p2.x;
			line.y1 = p2.y;
			g2.draw(line);
		}
	}

	private void drawPoints( Graphics2D g2 ) {
		BoofSwingUtil.antialiasing(g2);

		g2.setStroke(new BasicStroke(3));
		Ellipse2D.Double circle = new Ellipse2D.Double();
		double r = 4;
		double w = r*2 + 1;
		for (int i = 0; i < points.length; i += 6) {
			p1.setTo(offX + points.data[i], offY + points.data[i + 1]);
			p2.setTo(offX + points.data[i + 2], offY + points.data[i + 3]);
			p3.setTo(offX + points.data[i + 4], offY + points.data[i + 5]);

			transform(affine1, p1);
			transform(affine2, p2);
			transform(affine3, p3);

			g2.setColor(colors[i/6]);
			circle.setFrame(p1.x - r, p1.y - r, w, w);
			g2.draw(circle);
			circle.setFrame(p2.x - r, p2.y - r, w, w);
			g2.draw(circle);
			circle.setFrame(p3.x - r, p3.y - r, w, w);
			g2.draw(circle);
		}
	}

	private class MouseHandler extends MouseAdapter {
		@Override
		public void mousePressed( MouseEvent e ) {
			if (BoofSwingUtil.isRightClick(e)) {
				mode = Mode.values()[(mode.ordinal() + 1)%Mode.values().length];
				repaint();
			} else {
				// let the user select a single point. pick the closest one within tolerance
				double x = e.getX();
				double y = e.getY();

				int bestIndex = -1;
				double bestDistance = Double.MAX_VALUE;
				for (int i = 0; i < points.length; i += 6) {
					p1.setTo(offX + points.data[i], offY + points.data[i + 1]);
					p2.setTo(offX + points.data[i + 2], offY + points.data[i + 3]);
					p3.setTo(offX + points.data[i + 4], offY + points.data[i + 5]);

					transform(affine1, p1);
					transform(affine2, p2);
					transform(affine3, p3);

					double d = p1.distance2(x, y);
					d = Math.min(d, p2.distance2(x, y));
					d = Math.min(d, p3.distance2(x, y));

					if (d < bestDistance) {
						bestDistance = d;
						bestIndex = i/6;
					}
				}

				if (bestIndex >= 0 && bestDistance < 10*10) {
					singlePoint = bestIndex;
					repaint();
				} else if (singlePoint != -1) {
					singlePoint = -1;
					repaint();
				}
			}
		}
	}

	private static void transform( Affine2D_F64 t, Point2D_F64 p ) {
		double x = p.x*t.a11 + p.y*t.a12 + t.tx;
		double y = p.x*t.a21 + p.y*t.a22 + t.ty;
		p.setTo(x, y);
	}

	public void setPixelOffset( double x, double y ) {
		this.offX = x;
		this.offY = y;
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode( Mode mode ) {
		this.mode = mode;
	}

	enum Mode {
		NONE,
		LINES,
		POINTS,
	}
}
