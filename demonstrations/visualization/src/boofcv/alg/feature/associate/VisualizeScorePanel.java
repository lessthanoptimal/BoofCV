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

package boofcv.alg.feature.associate;

import boofcv.abst.feature.associate.*;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.struct.feature.*;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Let's the user select which score type to use.
 *
 * @author Peter Abeles
 */
public class VisualizeScorePanel extends StandardAlgConfigPanel implements ActionListener {

	// selects which image to view
	JComboBox scoreTypes = new JComboBox();

	Class type;
	ScoreAssociation selected;

	Listener listener;

	public VisualizeScorePanel( Listener listener ) {
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));

		scoreTypes.addActionListener(this);
		scoreTypes.setMaximumSize(scoreTypes.getPreferredSize());

		this.listener = listener;

		addLabeled(scoreTypes, "Score: ", this);
	}

	public void setFeatureType( Class type ) {
		if( this.type == type )
			return;

		this.type = type;

		scoreTypes.removeActionListener(this);
		scoreTypes.removeAllItems();
		if( type == TupleDesc_B.class ) {
			scoreTypes.addItem(new ScoreItem(new ScoreAssociateHamming_B(),"Hamming"));
		} else if( type == NccFeature.class ) {
			scoreTypes.addItem(new ScoreItem(new ScoreAssociateNccFeature(),"NCC"));
			scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclidean_F64(),"Euclidean"));
			scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclideanSq_F64(),"Euclidean2"));
			scoreTypes.addItem(new ScoreItem(new ScoreAssociateSad_F64(),"SAD"));
		} else if( TupleDesc_F64.class.isAssignableFrom(type) ) {
			scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclidean_F64(),"Euclidean"));
			scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclideanSq_F64(),"Euclidean2"));
			scoreTypes.addItem(new ScoreItem(new ScoreAssociateSad_F64(),"SAD"));
		} else if( type == TupleDesc_F32.class ) {
			scoreTypes.addItem(new ScoreItem(new ScoreAssociateEuclideanSq_F32(),"Euclidean2"));
			scoreTypes.addItem(new ScoreItem(new ScoreAssociateSad_F32(),"SAD"));
		} else if( type == TupleDesc_U8.class ) {
			scoreTypes.addItem(new ScoreItem(new ScoreAssociateSad_U8(),"SAD"));
		} else {
			throw new RuntimeException("Unknown description type "+type.getSimpleName());
		}
		selected = ((ScoreItem)scoreTypes.getSelectedItem()).assoc;
		scoreTypes.revalidate();
		scoreTypes.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ScoreItem item = (ScoreItem)scoreTypes.getSelectedItem();
		selected = item.assoc;

		if( listener != null )
			listener.changedSetting();
	}

	public ScoreAssociation getSelected() {
		return selected;
	}

	public static interface Listener
	{
		public void changedSetting();

	}

	private static class ScoreItem
	{
		String name;
		ScoreAssociation assoc;

		private ScoreItem(ScoreAssociation assoc, String name) {
			this.assoc = assoc;
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
