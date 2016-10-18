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

package boofcv.gui.learning;

import boofcv.gui.image.ShowImages;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.RandomMatrices;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Visualizes a confusion matrix.  Each element is assumed to have a value from 0 to 1.0
 *
 * @author Peter Abeles
 */
public class ConfusionMatrixPanel extends JPanel {

	DenseMatrix64F temp = new DenseMatrix64F(1,1);
	DenseMatrix64F confusion = new DenseMatrix64F(1,1);
	boolean dirty = false;

	boolean gray = false;

	boolean showNumbers = true;
	boolean showLabels = true;
	boolean showZeros = true;

	// fraction of the width that labels occupy
	double labelViewFraction = 0.30;
	List<String> labels;

	// if set to a valid category then that category will be highlighted
	int highlightCategory = -1;

	// internal variables used for rendering
	int viewHeight, viewWidth;
	int gridHeight, gridWidth;
	boolean renderLabels;

	/**
	 * Constructor that specifies the confusion matrix and width/height
	 * @param labels Optional labels for the confusion matrix.
	 * @param widthPixels preferred width and height of the panel in pixels
	 * @param gray Render gray scale or color image
	 */
	public ConfusionMatrixPanel( DenseMatrix64F M , List<String> labels, int widthPixels , boolean gray ) {
		this(widthPixels,labels!=null);

		setLabels(labels);
		setMatrix(M);
		this.gray = gray;
	}

	/**
	 * Constructor in which the prefered width and height is specified in pixels
	 * @param widthPixels preferred width and height
	 */
	public ConfusionMatrixPanel(int widthPixels, boolean hasLabels ) {

		int heightPixels = widthPixels;
		if( hasLabels ) {
			heightPixels *= 1.0-labelViewFraction;
		}

		setPreferredSize(new Dimension(widthPixels,heightPixels));
	}

	public void setMatrix( DenseMatrix64F A ) {
		synchronized ( this ) {
			temp.set(A);
			dirty = true;
		}
		repaint();
	}

	public boolean isGray() {
		return gray;
	}

	public void setGray(boolean gray) {
		this.gray = gray;
	}

	public boolean isShowNumbers() {
		return showNumbers;
	}

	public void setShowNumbers(boolean showNumbers) {
		this.showNumbers = showNumbers;
	}

	public boolean isShowZeros() {
		return showZeros;
	}

	public void setShowZeros(boolean showZeros) {
		this.showZeros = showZeros;
	}

	public boolean isShowLabels() {
		return showLabels;
	}

	public void setShowLabels(boolean showLabels) {
		this.showLabels = showLabels;
	}

	public void setLabels(List<String> labels) {
		this.labels = new ArrayList<>(labels);
	}

	public int getHighlightCategory() {
		return highlightCategory;
	}

	public void setHighlightCategory(int highlightCategory) {
		this.highlightCategory = highlightCategory;
	}

	@Override
	public synchronized void paint( Graphics g ) {
		synchronized ( this ) {
			if (dirty) {
				confusion.set(temp);
				dirty = false;
			}
		}

		Graphics2D g2 = (Graphics2D)g;

		int numCategories = confusion.getNumRows();

		synchronized ( this ) {
			viewHeight = getHeight();
			viewWidth = getWidth();

			gridHeight = viewHeight;
			gridWidth = viewWidth;

			renderLabels = this.showLabels && labels != null;
			if (renderLabels) {
//			gridHeight *= 1.0-labelViewFraction;
				gridWidth *= 1.0 - labelViewFraction;
			}
		}

		double fontSize = Math.min(gridWidth/numCategories,gridHeight/numCategories);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if(renderLabels) {
			renderLabels(g2, fontSize);
		}

		renderMatrix(g2, fontSize);

		if( highlightCategory >= 0 && highlightCategory < numCategories ) {
			g2.setColor(new Color(255,255,0,100));

			int ry = (int)(0.1*gridHeight / numCategories);
			int rx = (int)(0.1*gridWidth / numCategories);


			int y0 = highlightCategory * gridHeight / numCategories;
			int y1 = (highlightCategory + 1) * gridHeight / numCategories;

			int x0 = highlightCategory * gridWidth / numCategories;
			int x1 = (highlightCategory + 1) * gridWidth / numCategories;

			g2.fillRect(x0+rx,0,x1-x0-2*rx,gridHeight);
			g2.fillRect(0,y0+ry,viewWidth,y1-y0-2*ry);
		}
	}

	/**
	 * Renders the names on each category to the side of the confusion matrix
	 */
	private void renderLabels(Graphics2D g2, double fontSize) {
		int numCategories = confusion.getNumRows();

		int longestLabel = 0;
		if(renderLabels) {
			for (int i = 0; i < numCategories; i++) {
				longestLabel = Math.max(longestLabel,labels.get(i).length());
			}
		}

		Font fontLabel = new Font("monospaced", Font.BOLD, (int)(0.055*longestLabel*fontSize + 0.5));
		g2.setFont(fontLabel);
		FontMetrics metrics = g2.getFontMetrics(fontLabel);

		// clear the background
		g2.setColor(Color.WHITE);
		g2.fillRect(gridWidth,0,viewWidth-gridWidth,viewHeight);

		// draw the text
		g2.setColor(Color.BLACK);
		for (int i = 0; i < numCategories; i++) {
			String label = labels.get(i);

			int y0 = i * gridHeight / numCategories;
			int y1 = (i + 1) * gridHeight / numCategories;

			Rectangle2D r = metrics.getStringBounds(label,null);

			float adjX = (float)(r.getX()*2 + r.getWidth())/2.0f;
			float adjY = (float)(r.getY()*2 + r.getHeight())/2.0f;

			float x = ((viewWidth+gridWidth)/2f-adjX);
			float y = ((y1+y0)/2f-adjY);

			g2.drawString(label, x, y);
		}
	}

	/**
	 * Renders the confusion matrix and visualizes the value in each cell with a color and optionally a color.
	 */
	private void renderMatrix(Graphics2D g2, double fontSize) {
		int numCategories = confusion.getNumRows();

		Font fontNumber = new Font("Serif", Font.BOLD, (int)(0.6*fontSize + 0.5));
		g2.setFont(fontNumber);
		FontMetrics metrics = g2.getFontMetrics(fontNumber);
		for (int i = 0; i < numCategories; i++) {
			int y0 = i*gridHeight/numCategories;
			int y1 = (i+1)*gridHeight/numCategories;

			for (int j = 0; j < numCategories; j++) {
				int x0 = j*gridWidth/numCategories;
				int x1 = (j+1)*gridWidth/numCategories;

				double value = confusion.unsafe_get(i,j);

				int red,green,blue;
				if( gray ) {
					red = green = blue = (int)(255*(1.0-value));
				} else {
					green = 0;
					red = (int)(255*value);
					blue = (int)(255*(1.0-value));
				}
				g2.setColor(new Color(red, green, blue));

				g2.fillRect(x0,y0,x1-x0,y1-y0);

				// Render numbers inside the squares.  Pick a color so that the number is visible no matter what
				// the color of the square is
				if( showNumbers && (showZeros || value != 0 )) {
					int a = (red+green+blue)/3;

					String text = ""+(int)(value*100.0+0.5);
					Rectangle2D r = metrics.getStringBounds(text,null);

					float adjX = (float)(r.getX()*2 + r.getWidth())/2.0f;
					float adjY = (float)(r.getY()*2 + r.getHeight())/2.0f;

					float x = ((x1+x0)/2f-adjX);
					float y = ((y1+y0)/2f-adjY);

					int gray = a > 127 ? 0 : 255;

					g2.setColor(new Color(gray,gray,gray));
					g2.drawString(text,x,y);
				}
			}
		}
	}

	/**
	 * Use to sample the panel to see what is being displayed at the location clicked.  All coordinates
	 * are in panel coordinates.
	 *
	 * @param pixelX x-axis in panel coordinates
	 * @param pixelY y-axis in panel coordinates
	 * @param output (Optional) storage for output.
	 * @return Information on what is at the specified location
	 */
	public LocationInfo whatIsAtPoint( int pixelX , int pixelY , LocationInfo output ) {
		if( output == null )
			output = new LocationInfo();

		int numCategories = confusion.getNumRows();

		synchronized ( this ) {
			if( pixelX >= gridWidth ) {
				output.insideMatrix = false;
				output.col = output.row = pixelY*numCategories/gridHeight;
			} else {
				output.insideMatrix = true;
				output.row = pixelY*numCategories/gridHeight;
				output.col = pixelX*numCategories/gridWidth;
			}
		}

		return output;
	}

	/**
	 * Contains information on what was at the point
	 */
	public static class LocationInfo {
		public boolean insideMatrix;
		public int row,col;
	}

	public static void main(String[] args) {
		DenseMatrix64F m = RandomMatrices.createRandom(5,5,0,1,new Random(234));

		List<String> labels = new ArrayList<>();
		for (int i = 0; i < m.numRows; i++) {
			labels.add("Label "+i);
		}

		ConfusionMatrixPanel confusion = new ConfusionMatrixPanel(m,labels,300,false);
		confusion.setHighlightCategory(2);
		ShowImages.showWindow(confusion,"Window",true);
	}

}
