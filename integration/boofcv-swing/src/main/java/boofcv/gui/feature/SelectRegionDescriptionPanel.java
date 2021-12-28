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
import georegression.geometry.UtilPoint2D_I32;
import georegression.struct.point.Point2D_I32;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Allows the user to select a point, its size, and orientation. First the user clicks on a point
 * then drags the mouse out. The distance away from the first point is the size and the vector
 * is its orientation.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class SelectRegionDescriptionPanel extends JPanel implements MouseListener, MouseMotionListener {

	double clickTolerance = 10;

	BufferedImage background;
	@Nullable Point2D_I32 target;
	@Nullable Point2D_I32 current;

	// scale of background to view
	double imageScale;
	@Setter Listener listener;
	// is the region being dragged around?
	// In normal mode the current mouse position sets the size and orientation.
	// In drag mode it sets the region's center.
	boolean dragMode = false;

	public SelectRegionDescriptionPanel() {
		super(new BorderLayout());

		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void setBackground( BufferedImage image ) {
		this.background = image;
		setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
	}

	public void reset() {
		target = null;
		current = null;
	}

	public boolean hasTarget() {
		return target != null;
	}

	public Point2D_I32 getTarget() {
		return Objects.requireNonNull(target).copy();
	}

	@SuppressWarnings("NullAway")
	public double getFeatureRadius() {
		double r = UtilPoint2D_I32.distance(target, current);
		if (r < 1)
			return 1;
		return r;
	}

	@SuppressWarnings("NullAway")
	public double getFeatureOrientation() {
		double dy = current.y - target.y;
		double dx = current.x - target.x;

		return Math.atan2(dy, dx);
	}

	@Override
	public synchronized void paintComponent( Graphics g ) {
		super.paintComponent(g);

		if (background == null)
			return;

		computeScale();

		Graphics2D g2 = BoofSwingUtil.antialiasing(g);

		int dstWidth = (int)(imageScale*background.getWidth());
		int dstHeight = (int)(imageScale*background.getHeight());

		g2.drawImage(background, 0, 0, dstWidth, dstHeight, 0, 0, background.getWidth(), background.getHeight(), null);

		if (target != null && current != null) {
			int x1 = (int)(imageScale*target.x);
			int y1 = (int)(imageScale*target.y);
			int x2 = (int)(imageScale*current.x);
			int y2 = (int)(imageScale*current.y);

			int r, w;

			// draw feature orientation
			g2.setStroke(new BasicStroke(2));
			g2.setColor(Color.BLUE);
			g2.drawLine(x1, y1, x2, y2);

			// draw feature size
			r = (int)(imageScale*UtilPoint2D_I32.distance(target, current));
			g2.setStroke(new BasicStroke(6));
			drawOval(g2, x1, y1, r, Color.black);
			g2.setStroke(new BasicStroke(2));
			drawOval(g2, x1, y1, r, new Color(180, 180, 255));

			// draw the feature location
			r = 5;
			w = r*2 + 1;
			g2.setColor(Color.black);
			g2.fillOval(x1 - r, y1 - r, w, w);
			r = 3;
			w = r*2 + 1;
			g2.setColor(Color.red);
			g2.fillOval(x1 - r, y1 - r, w, w);
		}
	}

	private void drawOval( Graphics2D g2, int x, int y, int r, Color color ) {
		int w = r*2 + 1;
		g2.setColor(color);
		g2.drawOval(x - r, y - r, w, w);
	}

	private void computeScale() {
		double scaleX = getWidth()/(double)background.getWidth();
		double scaleY = getHeight()/(double)background.getHeight();

		imageScale = Math.min(scaleX, scaleY);
	}

	@Override
	public void mousePressed( MouseEvent e ) {
		int x = (int)(e.getX()/imageScale);
		int y = (int)(e.getY()/imageScale);

		// see if the mouse click was on the image
		if (x < 0 || x >= background.getWidth() || y < 0 || y >= background.getHeight()) {
			if (target != null) {
				repaint();
				if (listener != null) {
					listener.descriptionChanged(null, 0, 0);
				}
			}
			target = null;
			current = null;
		} else {
			boolean adjusted = false;
			if (target != null && current != null) {
				// if the user clicked on the center point enter drag mode
				int targetX = (int)(target.x*imageScale);
				int targetY = (int)(target.y*imageScale);
				double distance = UtilPoint2D_I32.distance(e.getX(), e.getY(), targetX, targetY);
				if (distance < clickTolerance) {
					// adjust the target to be at the cursor
					dragMode = true;
					current.x += x - target.x;
					current.y += y - target.y;
					target.setTo(x, y);
					adjusted = true;
				} else {
					// see if the user clicked on the circle, if so go into adjustment mode
					double radiusCircle = imageScale*UtilPoint2D_I32.distance(target, current);
					distance = Math.abs(distance - radiusCircle);
					if (distance < clickTolerance) {
						current.setTo(x, y);
						adjusted = true;
					}
				}
			}
			if (adjusted) {
				repaint();

				if (listener != null) {
					listener.descriptionChanged(target, getFeatureRadius(), getFeatureOrientation());
				}
			} else {
				dragMode = false;
				target = new Point2D_I32(x, y);
				current = target.copy();
				repaint();
			}
		}
	}

	@Override
	public void mouseDragged( MouseEvent e ) {
		if (current == null || target == null) {
			return;
		}
		int x = (int)(e.getX()/imageScale);
		int y = (int)(e.getY()/imageScale);

		if (dragMode) {
			current.x += x - target.x;
			current.y += y - target.y;
			target.setTo(x, y);
		} else {
			current.setTo(x, y);
		}
		repaint();

		if (listener != null) {
			listener.descriptionChanged(target, getFeatureRadius(), getFeatureOrientation());
		}
	}

	@Override public void mouseReleased( MouseEvent e ) {}
	@Override public void mouseEntered( MouseEvent e ) {}
	@Override public void mouseExited( MouseEvent e ) {}
	@Override public void mouseMoved( MouseEvent e ) {}
	@Override public void mouseClicked( MouseEvent e ) {}

	@FunctionalInterface
	public interface Listener {
		void descriptionChanged( @Nullable Point2D_I32 pt, double radius, double orientation );
	}
}
