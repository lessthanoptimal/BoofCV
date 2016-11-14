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

package boofcv.demonstrations.calibration;

import boofcv.alg.fiducial.calib.squares.SquareEdge;
import boofcv.alg.fiducial.calib.squares.SquareGrid;
import boofcv.alg.fiducial.calib.squares.SquareNode;
import boofcv.alg.filter.binary.Contour;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.EllipseRotated_F64;
import georegression.struct.shapes.Polygon2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static boofcv.gui.fiducial.VisualizeFiducial.drawLine;

/**
 * base class for chessboard and square grid calibration target detection
 *
 * @author Peter Abeles
 */
public abstract class CommonDetectCalibrationApp extends DemonstrationBase<GrayF32>
		implements DetectCalibrationPanel.Listener
{
	boolean success;

	DetectCalibrationPanel controlPanel;

	VisualizePanel imagePanel = new VisualizePanel();
	BufferedImage input;
	BufferedImage binary;
	GrayF32 grayPrev = new GrayF32(1,1);

	public CommonDetectCalibrationApp(int numRows , int numColumns , List<String> exampleInputs ) {
		super(exampleInputs, ImageType.single(GrayF32.class));
		controlPanel = new DetectCalibrationPanel(numRows,numColumns,true);
		add(imagePanel,BorderLayout.CENTER);
		add(controlPanel,BorderLayout.WEST);

		controlPanel.setListener(this);

		imagePanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				double scale = imagePanel.getScale();
				System.out.println("clicked at " + (e.getX()/scale) + " " + (e.getY()/scale));
			}
		});

		imagePanel.addMouseWheelListener(controlPanel);
	}

	protected abstract void declareDetector();

	protected abstract boolean process( GrayF32 image );

	protected abstract GrayU8 getBinaryImage();

	protected abstract List<List<SquareNode>> getClusters();

	protected abstract List<Point2D_F64> getCalibrationPoints();

	protected abstract List<Contour> getContours();

	protected abstract List<Polygon2D_F64> getFoundPolygons();

	protected abstract List<EllipseRotated_F64> getFoundEllipses();

	protected abstract List<SquareGrid> getGrids();

	@Override
	public void processImage( final BufferedImage buffered , GrayF32 gray ) {
		this.input = buffered;

		synchronized ( this ) {
			binary = conditionalDeclare(buffered, binary, BufferedImage.TYPE_INT_RGB);
			grayPrev.setTo(gray);
		}

		processFrame();
	}

	protected void renderGraph( Graphics2D g2 , double scale ) {
		List<List<SquareNode>> graphs = getClusters();

		BasicStroke strokeWide = new BasicStroke(3);
		BasicStroke strokeNarrow = new BasicStroke(2);

		Line2D.Double l = new Line2D.Double();

		g2.setStroke(new BasicStroke(3));
		for( int i = 0; i < graphs.size(); i++ ) {

			List<SquareNode> graph = graphs.get(i);

			int key = graphs.size() == 1 ? 0 : 255 * i / (graphs.size() - 1);

			int rgb = key << 8 | (255 - key);
			g2.setColor(new Color(rgb));

			List<SquareEdge> edges = new ArrayList<>();

			for( SquareNode n : graph ) {
				for (int j = 0; j < n.edges.length; j++) {
					if( n.edges[j] != null && !edges.contains(n.edges[j])) {
						edges.add( n.edges[j]);
					}
				}
			}

			for( SquareEdge e : edges ) {
				Point2D_F64 a = e.a.center;
				Point2D_F64 b = e.b.center;

				l.setLine(a.x*scale,a.y*scale,b.x*scale,b.y*scale);

				g2.setColor(Color.CYAN);
				g2.setStroke(strokeWide);
				g2.draw(l);
				g2.setColor(new Color(rgb));
				g2.setStroke(strokeNarrow);
				g2.draw(l);
			}
		}
	}

	@Override
	public void calibEventGUI() {
		if( controlPanel.getSelectedView() == 0 ) {
			imagePanel.setBufferedImage(input);
		} else if( controlPanel.getSelectedView() == 1 ){
			imagePanel.setBufferedImage(binary);
		} else {
			throw new RuntimeException("Unknown");
		}

		imagePanel.setScale(controlPanel.getScale());
		imagePanel.repaint();
	}

	@Override
	public void calibEventDetectorModified() {
		synchronized ( this ) {
			declareDetector();
		}

		processFrame();
	}

	public void processFrame() {
		synchronized ( this ) {
			success = process(grayPrev);
			VisualizeBinaryData.renderBinary(getBinaryImage(), false, binary);
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (success)
					controlPanel.setSuccessMessage("FOUND", true);
				else
					controlPanel.setSuccessMessage("FAILED", false);
				imagePanel.setPreferredSize(new Dimension(input.getWidth()+5, input.getHeight()+5));
				calibEventGUI();
				imagePanel.repaint();
			}
		});
	}

	class VisualizePanel extends ImageZoomPanel {
		@Override
		protected void paintInPanel(AffineTransform tran,Graphics2D g2 ) {
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			synchronized ( CommonDetectCalibrationApp.this ) {

				if( success ) {
					if( controlPanel.isShowOrder() ) {
						renderOrder(g2,scale);
					}

					if ( controlPanel.isShowPoints() ) {
						List<Point2D_F64> candidates = getCalibrationPoints();
						for (Point2D_F64 c : candidates) {
							VisualizeFeatures.drawPoint(g2, (int) (scale * c.x + 0.5), (int) (scale * c.y + 0.5), 1, Color.RED);
						}
					}
					if( controlPanel.isShowNumbers() ) {
						drawNumbers(g2, getCalibrationPoints(),null,scale);
					}
				}

				if( controlPanel.doShowContour ) {
					List<Contour> contour = getContours();

					g2.setStroke(new BasicStroke(1));
					g2.setColor(Color.RED);
					VisualizeBinaryData.renderExternal(contour,false,true,scale,g2);
				}

				if( controlPanel.isShowGraph() ) {
					renderGraph(g2, scale);
				}

				if( controlPanel.isShowShapes() ) {
					renderShapes(g2, scale);
				}

				if( controlPanel.isShowGrids() ) {
					renderGrid(g2,scale);
				}
			}
		}
	}

	protected void renderShapes(Graphics2D g2, double scale) {
		List<Polygon2D_F64> squares =  getFoundPolygons();

		for (int i = 0; i < squares.size(); i++) {
			Polygon2D_F64 p = squares.get(i);
//						if (isInGrids(p)) TODO fix broken method isInGrids
//							continue;
			g2.setColor(Color.cyan);
			g2.setStroke(new BasicStroke(4));
			drawPolygon(p, g2, scale);
			g2.setColor(Color.blue);
			g2.setStroke(new BasicStroke(2));
			drawPolygon(p, g2, scale);

			drawCornersInside(g2,scale,p);
		}

		List<EllipseRotated_F64> ellipses =  getFoundEllipses();
		AffineTransform rotate = new AffineTransform();

		for (int i = 0; i < ellipses.size(); i++) {
			EllipseRotated_F64 ellipse = ellipses.get(i);

			rotate.setToIdentity();
			rotate.rotate(ellipse.phi);

			double w = scale*ellipse.a*2;
			double h = scale*ellipse.b*2;

			Shape shape = rotate.createTransformedShape(new Ellipse2D.Double(-w/2,-h/2,w,h));
			shape = AffineTransform.getTranslateInstance(scale*ellipse.center.x,scale*ellipse.center.y).
					createTransformedShape(shape);

			g2.setColor(Color.cyan);
			g2.setStroke(new BasicStroke(4));
			g2.draw(shape);
			g2.setColor(Color.blue);
			g2.setStroke(new BasicStroke(2));
			g2.draw(shape);
		}
	}

	protected void renderGrid(Graphics2D g2, double scale) {
		List<SquareGrid> grids = getGrids();

		for (int i = 0; i < grids.size(); i++) {
			SquareGrid g = grids.get(i);
			int a = grids.size() == 1 ? 0 : 255 * i / (grids.size() - 1);

			int rgb = a << 16 | (255 - a) << 8;

			g2.setStroke(new BasicStroke(3));

			Color color = new Color(rgb);
			for (int j = 0; j < g.nodes.size(); j++) {
				SquareNode n = g.nodes.get(j);
				if( n == null )
					continue;
				g2.setColor(color);
				VisualizeShapes.drawPolygon(n.corners, true, scale, g2);

				drawCornersInside(g2,scale,n.corners);
			}
		}
	}

	private void renderOrder(Graphics2D g2, double scale ) {
		List<Point2D_F64> points = getCalibrationPoints();
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

	public static void drawPolygon( Polygon2D_F64 polygon, Graphics2D g2 , double scale ) {
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Line2D.Double l = new Line2D.Double();

		for( int i = 0; i < polygon.size()-1; i++ ) {
			Point2D_F64 p0 = polygon.get(i);
			Point2D_F64 p1 = polygon.get(i+1);
			drawLine(g2, l, p0.x*scale,  p0.y*scale,  p1.x*scale,  p1.y*scale);
		}
		if(  polygon.size() > 0) {
			Point2D_F64 p0 = polygon.get(0);
			Point2D_F64 p1 = polygon.get(polygon.size()-1);
			drawLine(g2, l, p0.x*scale, p0.y*scale, p1.x*scale, p1.y*scale);
		}

	}

	private boolean isInGrids( Polygon2D_F64 p ) {
		List<SquareGrid> grids = getGrids();

		for (int i = 0; i < grids.size(); i++) {
			SquareGrid g = grids.get(i);
			for (int j = 0; j < g.nodes.size(); j++) {
				if( g.nodes.get(j) != null && g.nodes.get(j).corners == p )
					return true;
			}
		}
		return false;
	}

	private void drawCornersInside( Graphics2D g2 , double scale, Polygon2D_F64 poly ) {
		Color colors[] = new Color[]{Color.RED,new Color(190, 0, 0),Color.GREEN,new Color(0, 190, 0)};

		Point2D_F64 center = new Point2D_F64();
		UtilPolygons2D_F64.vertexAverage(poly,center);

		for (int i = 0; i < poly.size(); i++) {
			Point2D_F64 p = poly.get(i);
			Color c = i < 4 ? colors[i] : Color.BLUE;

			double dx = p.x - center.x;
			double dy = p.y - center.y;

			double x = (center.x + dx*0.75)*scale;
			double y = (center.y + dy*0.75)*scale;

			VisualizeFeatures.drawPoint(g2, x, y, 3, c, false);
		}
	}

	public static void drawNumbers( Graphics2D g2 , List<Point2D_F64> foundTarget ,
									Point2Transform2_F32 transform ,
									double scale ) {

		Font regular = new Font("Serif", Font.PLAIN, 16);
		g2.setFont(regular);

		Point2D_F32 adj = new Point2D_F32();

		AffineTransform origTran = g2.getTransform();
		for (int i = 0; i < foundTarget.size(); i++) {
			Point2D_F64 p = foundTarget.get(i);

			if (transform != null) {
				transform.compute((float) p.x, (float) p.y, adj);
			} else {
				adj.set((float) p.x, (float) p.y);
			}

			String text = String.format("%2d", i);

			int x = (int) (adj.x * scale);
			int y = (int) (adj.y * scale);

			g2.setColor(Color.BLACK);
			g2.drawString(text, x - 1, y);
			g2.drawString(text, x + 1, y);
			g2.drawString(text, x, y - 1);
			g2.drawString(text, x, y + 1);
			g2.setTransform(origTran);
			g2.setColor(Color.GREEN);
			g2.drawString(text, x, y);
		}
	}
}
