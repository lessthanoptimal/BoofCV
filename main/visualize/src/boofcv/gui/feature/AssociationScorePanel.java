/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.associate.ScoreAssociation;
import georegression.struct.point.Point2D_F64;

import java.awt.*;
import java.awt.event.MouseListener;
import java.util.List;


/**
 * Displays relative association scores for different features.  When a feature is clicked on in
 * an image the best fit scores are show in the other image.
 *
 * @author Peter Abeles
 */
public class AssociationScorePanel<D>
		extends CompareTwoImagePanel implements MouseListener  {
	// adjusts how close to the optimal answer a point needs to be before it is plotted
	double containmentFraction;

	// how big circles are drawn in association window
	int maxCircleRadius = 15;

	// left and right window information
	List<D> leftDesc,rightDesc;
	double associationScore[];

	// computes association score
	ScoreAssociation<D> scorer;

	// statistical information on score distribution
	int indexBest;
	double worst;
	double best;

	public AssociationScorePanel( double containmentFraction ) {
		super(20,false);
		if( containmentFraction <= 0  )
			throw new IllegalArgumentException("containmentFraction must be more than zero");
		this.containmentFraction = containmentFraction;
	}


	public void setScorer(ScoreAssociation<D> scorer) {
		this.scorer = scorer;
	}

	public void setLocation(List<Point2D_F64> leftPts , List<Point2D_F64> rightPts ,
							List<D> leftDesc, List<D> rightDesc ) {
		setLocation(leftPts,rightPts);
		this.leftDesc = leftDesc;
		this.rightDesc = rightDesc;
	}

	protected void computeScore( boolean isTargetLeft , int targetIndex ) {
		int N = Math.max(leftPts.size(),rightPts.size());
		if( associationScore == null || associationScore.length < N ) {
			associationScore = new double[ N ];
		}
		if( isTargetLeft ) {
			D t = leftDesc.get(targetIndex);
			for( int i = 0; i < rightDesc.size(); i++ ) {
				D d = rightDesc.get(i);
				associationScore[i] = scorer.score(t,d);
			}
		} else {
			D t = rightDesc.get(targetIndex);
			for( int i = 0; i < leftDesc.size(); i++ ) {
				D d = leftDesc.get(i);
				associationScore[i] = scorer.score(t,d);
			}
		}
	}
	
	@Override
	protected void drawFeatures(Graphics2D g2,
							 double scaleLeft, int leftX, int leftY,
							 double scaleRight, int rightX, int rightY)
	{
		if( leftPts == null || rightPts == null ) {
			System.out.println("is null");
			return;
		}

		// draw all the found features in both images since nothing has been selected yet
		if( selected.isEmpty() ) {
			drawPoints(g2,leftPts,leftX,leftY,scaleLeft);
			drawPoints(g2,rightPts,rightX,rightY,scaleRight);
			return;
		} else if( selected.size() != 1 ) {
			System.err.println("Selected more than one feature!");
			return;
		}
		
		int selectedIndex = selected.get(0);
		
		// compute association score
		computeScore(selectedIsLeft,selectedIndex);

		// a feature has been selected.  In the image it was selected draw an X
		if( selectedIsLeft ) {
			drawCrossHair(g2,leftPts.get(selectedIndex),leftX,leftY,scaleLeft);
		} else {
			drawCrossHair(g2,rightPts.get(selectedIndex),rightX,rightY,scaleRight);
		}

		// draw circles of based on how similar a feature is to the selected one
		if( selectedIsLeft ) {
	   		drawDistribution(g2,rightPts,rightX,rightY,scaleRight);
		} else {
			drawDistribution(g2,leftPts,leftX,leftY,scaleLeft);
		}
	}

	/**
	 * Visualizes score distribution.  Larger circles mean its closer to the best
	 * fit score.
	 */
	private void drawDistribution( Graphics2D g2 , List<Point2D_F64> candidates ,
					  int offX, int offY , double scale) {
		findStatistics();

		// draw all the features, adjusting their size based on the first score
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(3));

		double normalizer;
		if( scorer.getScoreType().isZeroBest() )
			normalizer = best*containmentFraction;
		else
			normalizer = Math.abs(best)*(Math.exp(-1.0/containmentFraction));

		for( int i = 0; i < candidates.size(); i++ ) {
			Point2D_F64 p = candidates.get(i);

			double s = associationScore[i];

			// scale the circle based on how bad it is
			double ratio = 1-Math.abs(s-best)/normalizer;
			if( ratio < 0 )
				continue;

			int r = maxCircleRadius - (int)(maxCircleRadius*ratio);
			if( r > 0 ) {
				int x = (int)(p.x*scale+offX);
				int y = (int)(p.y*scale+offY);
				g2.drawOval(x-r,y-r,r*2+1,r*2+1);
			}
		}

		// draw the best feature
		g2.setColor(Color.GREEN);
		g2.setStroke(new BasicStroke(10));
		int w = maxCircleRadius*2+1;
		Point2D_F64 p = candidates.get(indexBest);
		int x = (int)(p.x*scale+offX);
		int y = (int)(p.y*scale+offY);
		g2.drawOval(x-maxCircleRadius,y-maxCircleRadius,w,w);
	}

	private void drawPoints( Graphics2D g2 , List<Point2D_F64> points ,
							 int startX , int startY , double scale ) {
		for( Point2D_F64 p : points ) {
			int x1 = (int)(scale*p.x)+startX;
			int y1 = (int)(scale*p.y)+startY;

			VisualizeFeatures.drawPoint(g2,x1,y1,Color.BLUE);
		}
	}

	private void drawCrossHair( Graphics2D g2 , Point2D_F64 target ,
								int startX , int startY , double scale) {

		int x = startX + (int)(target.x*scale);
		int y = startY + (int)(target.y*scale);

		int r = 10;
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(11));
		g2.drawLine(x-r,y,x+r,y);
		g2.drawLine(x,y-r,x,y+r);
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(5));
		g2.drawLine(x-r,y,x+r,y);
		g2.drawLine(x,y-r,x,y+r);
	}

	@Override
	protected boolean isValidPoint(int index) {
		return true;
	}

	private void findStatistics( ) {
		final int N = selectedIsLeft ? rightPts.size() : leftPts.size();

		indexBest = -1;
		worst = -Double.MAX_VALUE;
		best = Double.MAX_VALUE;
		for( int i = 0; i < N; i++ ) {
			double s = associationScore[i];
			if( s > worst )
				worst = s;
			if( s < best ) {
				best = s;
				indexBest = i;
			}
		}
	}
}
