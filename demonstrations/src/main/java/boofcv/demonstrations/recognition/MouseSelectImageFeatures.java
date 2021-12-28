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

package boofcv.demonstrations.recognition;

import boofcv.misc.BoofLambdas;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.UtilEjml;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;

/**
 * Lets the user select point image features by either clicking on or dragging a rectangle
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class MouseSelectImageFeatures extends MouseAdapter {

	final JComponent owner;

	/** Converts a screen pixel into an image pixel */
	public ScreenToImage screenToImage;
	/** Converts an image pixel into a screen pixel */
	public ImageToScreen imageToScreen;
	/** Gets the feature's pixel coordinate in the image */
	public FeatureLocation featureLocation;
	/** Looks up feature color by index */
	public FeatureColor featureColor = ( idx ) -> 0xFF0000;
	/** Checks to see if the feature should be skipped */
	public FeatureSkip featureSkip = ( idx ) -> false;
	/** Called after the user has selected a region */
	public BoofLambdas.ProcessCall handleSelected = () -> {};
	/** Specifies how many features there are */
	public int numFeatures;

	/** How far it will search in screen pixels when user clicks */
	public int searchRadiusPixels = 20;

	DogArray_I32 selected = new DogArray_I32();
	DogArray_B selectedMask = new DogArray_B();

	// can it select more than one?
	protected boolean selectRegion = true;

	// where it first clicked when selecting a region
	protected @Nullable Point2D_I32 selectedScreen0;
	// current position of the mouse while being dragged
	protected Point2D_I32 selectedScreen1 = new Point2D_I32();

	// Internal work space
	Point2D_F64 screenPixel = new Point2D_F64();
	Point2D_F64 imagePixel0 = new Point2D_F64();
	Point2D_F64 imagePixel1 = new Point2D_F64();

	Ellipse2D.Double ellipse = new Ellipse2D.Double();

	public final BasicStroke borderStroke = new BasicStroke(2.0f);

	public MouseSelectImageFeatures( JComponent owner ) {
		this.owner = owner;
	}

	public void paint( Graphics2D g2 ) {
		double circleRadius = 5.0;
		double circleWidth = circleRadius*2;

		if (selected.isEmpty()) {
			for (int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
				renderFeature(g2, circleRadius, circleWidth, featureIdx);
			}
		} else {
			for (int selectedIdx = 0; selectedIdx < selected.size; selectedIdx++) {
				renderFeature(g2, circleRadius, circleWidth, selected.get(selectedIdx));
			}
		}

		// draw the selected region as a rectangle
		if (selectRegion && selectedScreen0 != null) {
			int x0 = Math.min(selectedScreen1.getX(), selectedScreen0.x);
			int x1 = Math.max(selectedScreen1.getX(), selectedScreen0.x);
			int y0 = Math.min(selectedScreen1.getY(), selectedScreen0.y);
			int y1 = Math.max(selectedScreen1.getY(), selectedScreen0.y);

			g2.setColor(Color.WHITE);
			g2.setStroke(new BasicStroke(3));
			g2.drawRect(x0, y0, x1 - x0, y1 - y0);
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(1));
			g2.drawRect(x0, y0, x1 - x0, y1 - y0);
		}
	}

	/**
	 * True if the user has selected a subset of the features to shw
	 */
	public boolean featuresSelected() {
		return selectRegion && selectedMask.size == numFeatures && numFeatures > 0;
	}

	/**
	 * Returns if only a single feature is selected
	 */
	public boolean isSingleSelected() {
		return selectRegion && selectedMask.size == numFeatures && numFeatures > 0 && selected.size == 1;
	}

	public void reset() {
		selectedMask.reset();
		selected.reset();
		numFeatures = 0;
	}

	private void renderFeature( Graphics2D g2, double circleRadius, double circleWidth, int featureIdx ) {
		if (featureSkip.skip(featureIdx))
			return;
		featureLocation.lookupPixel(featureIdx, imagePixel0);
		imageToScreen.convert(imagePixel0.x, imagePixel0.y, screenPixel);
		double x = screenPixel.x;
		double y = screenPixel.y;
		ellipse.setFrame(x - circleRadius, y - circleRadius, circleWidth, circleWidth);
		g2.setColor(new Color(featureColor.lookupColor(featureIdx)));
		g2.fill(ellipse);

		// Save the previous stroke so that it can be changed back to this
		Stroke stroke = g2.getStroke();
		// Draw the black border around the feature
		g2.setStroke(borderStroke);
		g2.setColor(Color.BLACK);
		g2.draw(ellipse);
		// revert back to original stroke
		g2.setStroke(stroke);
	}

	@Override public void mousePressed( MouseEvent e ) {
		if (!selectRegion) {
			return;
		}
		selectedScreen0 = new Point2D_I32(e.getX(), e.getY());
		selectedScreen1.setTo(selectedScreen0);

		// this lets it know it shouldn't render filtered
		selectedMask.reset();
		selected.reset();
	}

	@Override public void mouseReleased( MouseEvent e ) {
		if (!selectRegion || selectedScreen0 == null) {
			return;
		}

		screenToImage.convert(selectedScreen0.getX(), selectedScreen0.getY(), imagePixel0);
		screenToImage.convert(selectedScreen1.getX(), selectedScreen1.getY(), imagePixel1);

		// Make sure (x0,y0) is the lower extent
		double x0 = Math.min(imagePixel0.x, imagePixel1.x);
		double x1 = Math.max(imagePixel0.x, imagePixel1.x);
		double y0 = Math.min(imagePixel0.y, imagePixel1.y);
		double y1 = Math.max(imagePixel0.y, imagePixel1.y);

		// See if the user clicked. this is a bit of a hack
		if (Math.abs(x1 - x0) <= 2 || Math.abs(y1 - y0) <= 2)
			return;

		// Find all the features inside this region
		findPointsInRegion(x0, y0, x1, y1);

		// reset the selector
		selectedScreen0 = null;
		owner.repaint();
	}

	@Override public void mouseDragged( MouseEvent e ) {
		if (selectRegion) {
			selectedScreen1.x = e.getX();
			selectedScreen1.y = e.getY();
			owner.repaint();
		}
	}

	/**
	 * If clicked then select the closest point
	 */
	@Override public void mouseClicked( MouseEvent e ) {
		int pixelX = e.getX();
		int pixelY = e.getY();

		// Find the closest feature to where the user clicked that's within the maximum distance
		double bestDistance = searchRadiusPixels + UtilEjml.EPS;
		int bestIdx = -1;

		for (int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
			featureLocation.lookupPixel(featureIdx, screenPixel);
			imageToScreen.convert(screenPixel.x, screenPixel.y, screenPixel);

			double d = screenPixel.distance(pixelX, pixelY);
			if (d < bestDistance) {
				bestDistance = d;
				bestIdx = featureIdx;
			}
		}

		// Reset the selected features
		selected.reset();
		selectedMask.resize(0);

		if (bestIdx == -1) {
			// No points were selected
			handleSelected.process();
			return;
		}

		// Only select this single feature
		selectedMask.resize(numFeatures, false);
		selectedMask.set(bestIdx, true);
		selected.add(bestIdx);
		handleSelected.process();
	}

	private void findPointsInRegion( double x0, double y0, double x1, double y1 ) {
		selected.reset();
		for (int i = 0; i < numFeatures; i++) {
			featureLocation.lookupPixel(i, imagePixel0);
			Point2D_F64 p = imagePixel0;

			if (p.x >= x0 && p.x < x1 && p.y >= y0 && p.y < y1) {
				selected.add(i);
			}
		}

		// The user has selected some points. Configure the mask so that they will be displayed
		if (!selected.isEmpty()) {
			selectedMask.resize(numFeatures, false);
			selected.forEach(idx -> selectedMask.set(idx, true));
		}

		handleSelected.process();
	}

	@FunctionalInterface
	public interface ScreenToImage {
		void convert( int x, int y, Point2D_F64 imagePixel );
	}

	@FunctionalInterface
	public interface ImageToScreen {
		void convert( double x, double y, Point2D_F64 screenPixel );
	}

	@FunctionalInterface
	public interface FeatureLocation {
		void lookupPixel( int index, Point2D_F64 imagePixel );
	}

	@FunctionalInterface
	public interface FeatureSkip {
		boolean skip( int index );
	}

	@FunctionalInterface
	public interface FeatureColor {
		int lookupColor( int index );
	}
}
