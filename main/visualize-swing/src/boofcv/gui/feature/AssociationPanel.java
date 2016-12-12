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

package boofcv.gui.feature;

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedPair;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * Shows which two features are associated with each other.  An individual feature
 * can be shown alone by clicking on it.
 *
 * @author Peter Abeles
 */
public class AssociationPanel extends CompareTwoImagePanel implements MouseListener {

	// which features are associated with each other
	private int assocLeft[],assocRight[];

	// color of each points.  Randomly select at runtime
	Color colors[];

	public AssociationPanel(int borderSize ) {
		super(borderSize,true);
	}

	public synchronized void setAssociation( List<Point2D_F64> leftPts , List<Point2D_F64> rightPts,
											 FastQueue<AssociatedIndex> matches ) {

		List<Point2D_F64> allLeft = new ArrayList<>();
		List<Point2D_F64> allRight = new ArrayList<>();

		assocLeft = new int[ matches.size() ];
		assocRight = new int[ matches.size() ];

		for (int i = 0; i < matches.size(); i++) {
			AssociatedIndex a = matches.get(i);

			allLeft.add( leftPts.get(a.src));
			allRight.add( rightPts.get(a.dst));

			assocLeft[i] = i;
			assocRight[i] = i;
		}

		setLocation(allLeft,allRight);

//		assocLeft = new int[ leftPts.size() ];
//		assocRight = new int[ rightPts.size() ];
//
//		for( int i = 0; i < assocLeft.length; i++ )
//			assocLeft[i] = -1;
//		for( int i = 0; i < assocRight.length; i++ )
//			assocRight[i] = -1;
//
//		for( int i = 0; i < matches.size; i++ ) {
//			AssociatedIndex a = matches.get(i);
//			assocLeft[a.src] = a.dst;
//			assocRight[a.dst] = a.src;
//		}

		Random rand = new Random(234);
		colors = new Color[ leftPts.size() ];
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = new Color(rand.nextInt() | 0xFF000000 );
		}
	}

	public synchronized void setAssociation( List<AssociatedPair> matches ) {
		List<Point2D_F64> leftPts = new ArrayList<>();
		List<Point2D_F64> rightPts = new ArrayList<>();

		for( AssociatedPair p : matches ) {
			leftPts.add(p.p1);
			rightPts.add(p.p2);
		}

		setLocation(leftPts,rightPts);

		assocLeft = new int[ leftPts.size() ];
		assocRight = new int[ rightPts.size() ];

		for( int i = 0; i < assocLeft.length; i++ ) {
			assocLeft[i] = i;
			assocRight[i] = i;
		}

		Random rand = new Random(234);
		colors = new Color[ leftPts.size() ];
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = new Color(rand.nextInt() | 0xFF000000 );
		}
	}

	public synchronized void setAssociation( List<Point2D_F64> leftPts , List<Point2D_F64> rightPts ) {

		setLocation(leftPts,rightPts);

		assocLeft = new int[ leftPts.size() ];
		assocRight = new int[ rightPts.size() ];

		for( int i = 0; i < assocLeft.length; i++ ) {
			assocLeft[i] = i;
			assocRight[i] = i;
		}

		Random rand = new Random(234);
		colors = new Color[ leftPts.size() ];
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = new Color(rand.nextInt() | 0xFF000000 );
		}
	}

	@Override
	protected void drawFeatures(Graphics2D g2 ,
								double scaleLeft, int leftX, int leftY,
								double scaleRight, int rightX, int rightY) {
		if( selected.isEmpty() )
			drawAllFeatures(g2, scaleLeft,scaleRight,rightX);
		else {

			for( int selectedIndex : selected ) {
				// draw just an individual feature pair
				Point2D_F64 l,r;
				Color color;

				if( selectedIsLeft ) {
					l = leftPts.get(selectedIndex);
					if( assocLeft[selectedIndex] < 0 ) {
						r = null; color = null;
					} else {
						r = rightPts.get(assocLeft[selectedIndex]);
						color = colors[selectedIndex];
					}
				} else {
					r = rightPts.get(selectedIndex);
					if( assocRight[selectedIndex] < 0 ) {
						l = null; color = null;
					} else {
						l = leftPts.get(assocRight[selectedIndex]);
						color = colors[assocRight[selectedIndex]];
					}
				}

				if( color == null ) // clicking on something with no association is annoying
					drawAllFeatures(g2, scaleLeft,scaleRight,rightX);
				else
					drawAssociation(g2, scaleLeft,scaleRight,rightX, l, r, color);
			}
		}
	}

	private void drawAllFeatures(Graphics2D g2, double scaleLeft , double scaleRight , int rightX) {
		if( assocLeft == null || rightPts == null || leftPts == null )
			return;

		for( int i = 0; i < assocLeft.length; i++ ) {
			if( assocLeft[i] == -1 )
				continue;

			Point2D_F64 l = leftPts.get(i);
			Point2D_F64 r = rightPts.get(assocLeft[i]);

			Color color = colors[i];

			drawAssociation(g2, scaleLeft,scaleRight,rightX, l, r, color);
		}
	}

	private void drawAssociation(Graphics2D g2, double scaleLeft , double scaleRight , int rightX, Point2D_F64 l, Point2D_F64 r, Color color) {
		if( r == null ) {
			int x1 = (int)(scaleLeft*l.x);
			int y1 = (int)(scaleLeft*l.y);
			VisualizeFeatures.drawPoint(g2,x1,y1,Color.RED);
		} else if( l == null ) {
			int x2 = (int)(scaleRight*r.x) + rightX;
			int y2 = (int)(scaleRight*r.y);
			VisualizeFeatures.drawPoint(g2,x2,y2,Color.RED);
		} else {
			int x1 = (int)(scaleLeft*l.x);
			int y1 = (int)(scaleLeft*l.y);
			VisualizeFeatures.drawPoint(g2,x1,y1,color);

			int x2 = (int)(scaleRight*r.x) + rightX;
			int y2 = (int)(scaleRight*r.y);
			VisualizeFeatures.drawPoint(g2,x2,y2,color);

			g2.setColor(color);
			g2.drawLine(x1,y1,x2,y2);
		}
	}

	@Override
	protected boolean isValidPoint(int index) {
		if( selectedIsLeft )
			return assocLeft[index] >= 0;
		else
			return assocRight[index] >= 0;
	}
}
