/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.gui.feature;

import gecv.struct.FastArray;
import gecv.struct.feature.AssociatedIndex;
import jgrl.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;


/**
 * Shows which two features are associated with each other.
 *
 * @author Peter Abeles
 */
public class AssociationPanel extends JPanel implements MouseListener {

	// size of the border between the images
	int borderSize;

	// maximum size of sub-images inside of panel
	int maxWidth;
	int maxHeight;

	// left and right window information
	BufferedImage leftImage,rightImage;
	double scaleLeft,scaleRight;

	// list of features in both images
	List<Point2D_I32> leftPts,rightPts;

	// which features are associated with each other
	int assocLeft[],assocRight[];


	public synchronized void setImages(BufferedImage leftImage , BufferedImage rightImage ) {
		this.leftImage = leftImage;
		this.rightImage = rightImage;

		// todo compute visualization scale

		// TODO set preferred size
	}

	public synchronized void setAssociation( List<Point2D_I32> leftPts , List<Point2D_I32> rightPts,
								FastArray<AssociatedIndex> matches ) {
		this.leftPts = leftPts;
		this.rightPts = rightPts;

		assocLeft = new int[ leftPts.size() ];
		assocRight = new int[ rightPts.size() ];

		for( int i = 0; i < assocLeft.length; i++ )
			assocLeft[i] = -1;
		for( int i = 0; i < assocRight.length; i++ )
			assocRight[i] = -1;

		for( int i = 0; i < matches.size; i++ ) {
			AssociatedIndex a = matches.get(i);
			assocLeft[a.src] = a.dst;
			assocRight[a.dst] = a.src;
		}
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}
}
