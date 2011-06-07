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

package gecv.gui.geo;

import gecv.alg.geo.AssociatedPair;
import jgrl.struct.point.Point2D_F32;

import java.awt.*;


/**
 * Various functions for drawing information about {@link AssociatedPair} features.
 *
 * @author Peter Abeles
 */
public class DrawAssociatedPairs {

	int r;
	int w;
	int ro;
	int wo;

	Color color = Color.red;

	public DrawAssociatedPairs( int radius ) {
		r = radius;
		w = r*2+1;
		ro = r+2;
		wo = ro*2+1;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public void drawNumber(Graphics2D g2, java.util.List<AssociatedPair> list ) {

		for (int i = 0; i < list.size(); i++) {
			AssociatedPair p = list.get(i);
			Point2D_F32 pt = p.currLoc;

			g2.drawString(""+p.featureId,pt.x-10,pt.y-10);
		}
	}

	public void drawCurrent(Graphics2D g2, java.util.List<AssociatedPair> list ) {

		for (int i = 0; i < list.size(); i++) {
			Point2D_F32 pt = list.get(i).currLoc;

			int x = (int)pt.x;
			int y = (int)pt.y;

			g2.setColor(Color.BLACK);
			g2.fillOval(x - ro, y - ro, wo, wo);
			g2.setColor(color);
			g2.fillOval(x - r, y - r, w, w);
		}
	}

	public void drawKey(Graphics2D g2, java.util.List<AssociatedPair> list ) {

		for (int i = 0; i < list.size(); i++) {
			Point2D_F32 pt = list.get(i).keyLoc;

			int x = (int)pt.x;
			int y = (int)pt.y;

			g2.setColor(Color.BLACK);
			g2.fillOval(x - ro, y - ro, wo, wo);
			g2.setColor(color);
			g2.fillOval(x - r, y - r, w, w);
		}
	}
}
