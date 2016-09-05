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

package boofcv.gui.tracker;

import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Area2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.RectangleLength2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

/**
 * Panel for displaying results of {@link boofcv.abst.tracker.TrackerObjectQuad}.
 *
 * @author Peter Abeles
 */
public class TrackerObjectQuadPanel extends JPanel implements MouseListener, MouseMotionListener {

	int numSelected = 0;

	Quadrilateral_F64 quad = new Quadrilateral_F64();
	boolean targetVisible;

	Listener listener;

	BufferedImage bg;

	Mode mode = Mode.IDLE;

	public TrackerObjectQuadPanel(Listener listener) {
		super(new BorderLayout());
		this.listener = listener;
		if( listener != null ){
			addMouseListener(this);
			addMouseMotionListener(this);
			grabFocus();
		}
	}

	public synchronized void setBackGround( BufferedImage image ) {
		if( bg == null || bg.getWidth() != image.getWidth() || bg.getHeight() != image.getHeight() ) {
			bg = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_RGB);
		}
		bg.createGraphics().drawImage(image,0,0,null);
	}

	public synchronized void enterIdleMode() {
		mode = Mode.IDLE;
		repaint();
	}

	public synchronized void enterSelectMode() {
		mode = Mode.PLACING_POINTS;
		numSelected = 0;
		repaint();
	}

	public synchronized void setTarget( Quadrilateral_F64 quad , boolean visible ) {
		if( quad != null )
			this.quad.set(quad);
		mode = Mode.TRACKING;
		targetVisible = visible;
		repaint();
	}

	public synchronized void setTarget( RectangleLength2D_I32 rect , boolean visible ) {
		if( quad != null )
			UtilPolygons2D_F64.convert(rect, quad);
		mode = Mode.TRACKING;
		targetVisible = visible;
		repaint();
	}

	@Override
	protected synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;

		g2.drawImage(bg, 0, 0, null);

		g2.setStroke(new BasicStroke(5));
		g2.setColor(Color.gray);

		if (mode == Mode.PLACING_POINTS) {
			if (numSelected == 1) {
				drawCorner(g2, quad.a, Color.RED);
			} else if (numSelected == 2) {
				drawLine(g2, quad.a, quad.b);
				drawCorner(g2, quad.a, Color.RED);
				drawCorner(g2, quad.b, Color.ORANGE);
			} else if (numSelected == 3) {
				drawLine(g2, quad.a, quad.b);
				drawLine(g2, quad.b, quad.c);
				drawCorner(g2, quad.a, Color.RED);
				drawCorner(g2, quad.b, Color.ORANGE);
				drawCorner(g2, quad.c, Color.CYAN);
			} else if (numSelected == 4) {
				VisualizeShapes.drawQuad(quad, g2, true, Color.gray, Color.CYAN);
				drawCorner(g2, quad.a, Color.RED);
				drawCorner(g2, quad.b, Color.ORANGE);
				drawCorner(g2, quad.c, Color.CYAN);
				drawCorner(g2, quad.d, Color.BLUE);
			}
		} else if (mode == Mode.DRAGGING_RECT) {
			VisualizeShapes.drawQuad(quad, g2, true, Color.gray, Color.gray);
			drawCorner(g2, quad.a, Color.RED);
			drawCorner(g2, quad.b, Color.RED);
			drawCorner(g2, quad.c, Color.RED);
			drawCorner(g2, quad.d, Color.RED);
		} else if (mode == Mode.TRACKING) {
			if (targetVisible) {
				VisualizeShapes.drawQuad(quad, g2, true, Color.gray, Color.CYAN);
				drawCorner(g2, quad.a, Color.RED);
				drawCorner(g2, quad.b, Color.RED);
				drawCorner(g2, quad.c, Color.RED);
				drawCorner(g2, quad.d, Color.RED);
			}
		}
	}

	private void drawLine( Graphics2D g2 , Point2D_F64 a , Point2D_F64 b ) {
		g2.drawLine((int)(a.x+0.5),(int)(a.y+0.5),(int)(b.x+0.5),(int)(b.y+0.5));
	}

	private void drawCorner( Graphics2D g2 , Point2D_F64 a , Color color ) {
		VisualizeFeatures.drawPoint(g2,a.x,a.y,6,color,false);
	}

	@Override
	public synchronized void mouseClicked(MouseEvent e) {
		if( mode == Mode.IDLE ) {
			mode = Mode.PLACING_POINTS;
			numSelected = 0;
			listener.pauseTracker();
		} else if( mode != Mode.PLACING_POINTS ) {
			return;
		}

		if( numSelected == 0 ) {
			quad.a.set(e.getX(),e.getY());
		} else if( numSelected == 1 ) {
			quad.b.set(e.getX(),e.getY());
		} else if( numSelected == 2 ) {
			quad.c.set(e.getX(),e.getY());
		} else if( numSelected == 3 ) {
			quad.d.set(e.getX(),e.getY());
		}
		numSelected++;
		repaint();

		if( numSelected == 4 ) {
			mode = Mode.TRACKING;
			listener.selectedTarget(quad);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if( mode == Mode.PLACING_POINTS )
			return;
		mode = Mode.DRAGGING_RECT;
		quad.a.set(e.getX(),e.getY());
		setCornerC(e.getX(),e.getY());

		listener.pauseTracker();
		repaint();
	}

	private void setCornerC( double x , double y ) {
		quad.b.set(quad.a.x,y);
		quad.c.set(x,y);
		quad.d.set(x,quad.a.y);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if( mode != Mode.DRAGGING_RECT )
			return;
		setCornerC(e.getX(),e.getY());

		double area = Area2D_F64.quadrilateral(quad);
		if( area < 10*10 ) {
			mode = Mode.IDLE;
			repaint();
		} else {
			mode = Mode.TRACKING;
			listener.selectedTarget(quad);
			repaint();
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	public synchronized void setDefaultTarget(Quadrilateral_F64 target) {
		this.quad.set(target);
		numSelected = 4;
		mode = Mode.TRACKING;
		targetVisible = true;
		repaint();
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if( mode != Mode.DRAGGING_RECT )
			return;
		setCornerC(e.getX(),e.getY());
		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	public enum Mode {
		IDLE,
		DRAGGING_RECT,
		PLACING_POINTS,
		TRACKING
	}

	public static interface Listener
	{
		public void selectedTarget( Quadrilateral_F64 target );

		public void pauseTracker();
	}
}
