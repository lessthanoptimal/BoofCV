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

package boofcv.gui.learning;

import boofcv.gui.image.ShowImages;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.RandomMatrices;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
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

	boolean showZeros = true;

	/**
	 * Constructor that specifies the confusion matrix and width/height
	 * @param width preferred width and height
	 * @param gray Render gray scale or color image
	 */
	public ConfusionMatrixPanel( DenseMatrix64F M , int width , boolean gray ) {
		this(width, width);
		setMatrix(M);
		this.gray = gray;
	}

	/**
	 * Constructor in which the prefered width and height is specified in pixels
	 * @param width preferred width
	 * @param height preferred height
	 */
	public ConfusionMatrixPanel(int width, int height) {
		setPreferredSize(new Dimension(width,height));
		setMinimumSize(new Dimension(width, height));
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

	@Override
	public synchronized void paint( Graphics g ) {
		synchronized ( this ) {
			if (dirty) {
				confusion.set(temp);
				dirty = false;
			}
		}

		Graphics2D g2 = (Graphics2D)g;

		int numRows = confusion.getNumRows();
		int numCols = confusion.getNumCols();

		int height = getHeight();
		int width = getWidth();

		Font font = new Font("Serif", Font.BOLD, (int)(0.6*width/numCols));
		g2.setFont(font);
		FontMetrics metrics = g2.getFontMetrics(font);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		for (int i = 0; i < numRows; i++) {
			int y0 = i*height/(numRows-1);
			int y1 = (i+1)*height/(numRows-1);

			for (int j = 0; j < numCols; j++) {
				int x0 = j*width/(numCols-1);
				int x1 = (j+1)*width/(numCols-1);

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

	public static void main(String[] args) {
		DenseMatrix64F m = RandomMatrices.createRandom(5,5,0,1,new Random(234));

		ShowImages.showWindow(new ConfusionMatrixPanel(m,300,true),"Window",true);
	}

}
