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

package boofcv.gui.calibration;

import boofcv.abst.geo.calibration.ImageResults;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.struct.distort.DoNothing2Transform2_F32;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;

import static boofcv.gui.calibration.UtilCalibrationGui.drawNumbers;
import static boofcv.gui.calibration.UtilCalibrationGui.renderOrder;

/**
 * Panel for displaying results from camera calibration. Controls and renders visuals.
 *
 * @author Peter Abeles
 */
public abstract class DisplayCalibrationPanel extends ImageZoomPanel {

	// number of pixels away at zoom=1 a corner can be selected
	double canonicalClickDistance = 15;

	// configures what is displayed or not
	public boolean showPoints = true;
	public boolean showErrors = true;
	public boolean showUndistorted = false;
	public boolean showAll = false;
	public boolean showNumbers = true;
	public boolean showOrder = true;
	public boolean showResiduals = false;
	public double errorScale;

	// Which observation in the current image has the user selected
	@Getter protected int selectedObservation = -1;

	// observed feature locations
	@Nullable @Getter CalibrationObservation observation = null;
	// results of calibration
	@Nullable @Getter public ImageResults results = null;
	@Nullable List<CalibrationObservation> allObservations = null;

	// Used to transform point coordinate system
	protected Point2Transform2_F32 pixelTransform = new DoNothing2Transform2_F32();

	// workspace
	protected Point2D_F32 adj = new Point2D_F32();
	protected Point2D_F32 adj2 = new Point2D_F32();
	protected Ellipse2D.Double ellipse = new Ellipse2D.Double();
	protected Line2D.Double line = new Line2D.Double();

	// Called after setScale has been called
	public SetScale setScale = ( s ) -> {};

	final Color lightRed = new Color(255, 150, 150);

	protected DisplayCalibrationPanel() {
		panel.addMouseWheelListener(e -> setScale(BoofSwingUtil.mouseWheelImageZoom(scale, e)));

		// navigate using left mouse clicks
		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e ) {
				panel.requestFocus();
				if (!SwingUtilities.isRightMouseButton(e))
					return;
				CalibrationObservation features = DisplayCalibrationPanel.this.observation;
				if (features == null)
					return;
				selectedObservation = findClickedPoint(e, features);
				repaint();
			}
		});
	}

	/**
	 * Finds the landmark which is the closest to the point clicked by the user. Maximum distance is determined
	 * in image pixels
	 */
	protected int findClickedPoint( MouseEvent e, CalibrationObservation features ) {
		// use square distance since it's faster
		double bestDistanceSq = canonicalClickDistance/scale;
		bestDistanceSq *= bestDistanceSq;

		int bestIndex = -1;
		Point2D_F64 p = pixelToPoint(e.getX(), e.getY());
		for (int i = 0; i < features.points.size(); i++) {
			Point2D_F64 f = features.points.get(i).p;
			pixelTransform.compute((float)f.x, (float)f.y, adj);
			double d = p.distance2(adj.x, adj.y);
			if (d <= bestDistanceSq) {
				bestDistanceSq = d;
				bestIndex = i;
			}
		}
		return bestIndex;
	}

	public void setResults( CalibrationObservation features, @Nullable ImageResults results,
							List<CalibrationObservation> allFeatures ) {
		BoofSwingUtil.checkGuiThread();

		this.observation = features;
		this.results = results;
		this.allObservations = allFeatures;
		this.selectedObservation = -1;
	}

	public void clearResults() {
		BoofSwingUtil.checkGuiThread();

		observation = null;
		results = null;
		allObservations = null;
		selectedObservation = -1;
	}

	public void setDisplay( boolean showPoints, boolean showErrors,
							boolean showUndistorted, boolean showAll, boolean showNumbers,
							boolean showOrder,
							double errorScale ) {
		this.showPoints = showPoints;
		this.showErrors = showErrors;
		this.showUndistorted = showUndistorted;
		this.showAll = showAll;
		this.showNumbers = showNumbers;
		this.showOrder = showOrder;
		this.errorScale = errorScale;
	}

	@Override public synchronized void setScale( double scale ) {
		// Avoid endless loops by making sure it's changing
		if (this.scale == scale)
			return;
		super.setScale(scale);
		setScale.setScale(scale);
	}

	/**
	 * Forgets the previously passed in calibration
	 */
	public abstract void clearCalibration();

	public void deselectPoint() {
		selectedObservation = -1;
	}

	/**
	 * Visualizes calibration information, such as feature location and order.
	 */
	protected void drawFeatures( Graphics2D g2, double scale ) {
		if (observation == null || allObservations == null)
			return;

		BoofSwingUtil.antialiasing(g2);

		final CalibrationObservation set = observation;

		if (showOrder) {
			renderOrder(g2, pixelTransform, scale, set.points);
		}

		if (showResiduals && results != null) {
			// draw a line showing the difference between observed and reprojected points
			// Draw this before points so that the observed points are drawn on top and you can see the delta
			g2.setStroke(new BasicStroke(4));
			g2.setColor(Color.GREEN);
			for (int i = 0; i < set.size(); i++) {
				PointIndex2D_F64 p = set.get(i);
				float dx = (float)results.residuals[i*2];
				float dy = (float)results.residuals[i*2 + 1];

				pixelTransform.compute((float)p.p.x, (float)p.p.y, adj);
				pixelTransform.compute((float)p.p.x + dx, (float)p.p.y + dy, adj2);
				line.setLine(scale*adj.x, scale*adj.y, scale*adj2.x, scale*adj2.y);
				g2.draw(line);
			}
		}

		if (showPoints) {
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(5));
			for (PointIndex2D_F64 p : set.points) {
				pixelTransform.compute((float)p.p.x, (float)p.p.y, adj);
				VisualizeFeatures.drawCross(g2, adj.x*scale, adj.y*scale, 5);
			}
			g2.setStroke(new BasicStroke(2));
			g2.setColor(lightRed);
			for (PointIndex2D_F64 p : set.points) {
				pixelTransform.compute((float)p.p.x, (float)p.p.y, adj);
				VisualizeFeatures.drawCross(g2, adj.x*scale, adj.y*scale, 5);
			}
		}

		if (showAll) {
			for (CalibrationObservation l : allObservations) {
				for (PointIndex2D_F64 p : l.points) {
					pixelTransform.compute((float)p.p.x, (float)p.p.y, adj);
					VisualizeFeatures.drawPoint(g2, adj.x*scale, adj.y*scale, 3, Color.BLUE, Color.WHITE, ellipse);
				}
			}
		}

		if (showNumbers) {
			drawNumbers(g2, set.points, pixelTransform, scale);
		}

		if (showErrors && results != null) {
			g2.setStroke(new BasicStroke(4));
			g2.setColor(Color.BLACK);
			for (int i = 0; i < set.size(); i++) {
				PointIndex2D_F64 p = set.get(i);
				pixelTransform.compute((float)p.p.x, (float)p.p.y, adj);

				double r = errorScale*results.pointError[i];
				if (r < 1)
					continue;

				VisualizeFeatures.drawCircle(g2, adj.x*scale, adj.y*scale, r, ellipse);
			}

			g2.setStroke(new BasicStroke(2.5f));
			g2.setColor(Color.ORANGE);
			for (int i = 0; i < set.size(); i++) {
				PointIndex2D_F64 p = set.get(i);
				pixelTransform.compute((float)p.p.x, (float)p.p.y, adj);

				double r = errorScale*results.pointError[i];
				if (r < 1)
					continue;

				VisualizeFeatures.drawCircle(g2, adj.x*scale, adj.y*scale, r, ellipse);
			}
		}

		// Draw the selected feature
		if (selectedObservation >= 0 && selectedObservation < set.size()) {
			PointIndex2D_F64 p = set.get(selectedObservation);
			pixelTransform.compute((float)p.p.x, (float)p.p.y, adj);
			VisualizeFeatures.drawPoint(g2, adj.x*scale, adj.y*scale, 10.0, Color.GREEN, true, ellipse);
		}
	}

	@FunctionalInterface public interface SetScale {
		void setScale( double scale );
	}
}
