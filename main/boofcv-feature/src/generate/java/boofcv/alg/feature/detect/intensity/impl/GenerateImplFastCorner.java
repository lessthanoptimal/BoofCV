/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.misc.AutoTypeImage;
import boofcv.misc.CodeGeneratorBase;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

/**
 * @author Peter Abeles
 */
public class GenerateImplFastCorner extends CodeGeneratorBase {

	private static final int TOTAL_CIRCLE = 16;

	// minimum number of edge points in a row to make a corner
	private int minContinuous;

	AutoTypeImage imageType;
	String sumType;
	String bitwise;
	String dataType;

	// What is known about each bit
	List<Sample>[] samples = new ArrayList[TOTAL_CIRCLE];

	// used to compute the score. For each possible corner given the current samples incremenet by one
	int possibleUp[] = new int[TOTAL_CIRCLE];
	int possibleDown[] = new int[TOTAL_CIRCLE];

	// if the first bit in a corner would have already been detected then it's index is set to true
	boolean detectedUp[] = new boolean[TOTAL_CIRCLE];
	boolean detectedDown[] = new boolean[TOTAL_CIRCLE];

	int tabs;

	public GenerateImplFastCorner() {
		super(false);

		for (int i = 0; i < samples.length; i++) {
			samples[i] = new ArrayList<>();
		}
	}

	@Override
	public void generate() throws FileNotFoundException {
		int n[] = {9,10,11,12};
		AutoTypeImage d[] = {AutoTypeImage.U8,AutoTypeImage.F32};

//		int n[] = {9};
//		AutoTypeImage d[] = {AutoTypeImage.U8};

		for( int minContinuous : n ) {
			for( AutoTypeImage imageType : d ) {
				createFile(imageType,minContinuous);
			}
		}
	}

	public void createFile( AutoTypeImage imageType , int minContinuous ) throws FileNotFoundException {
		className = "ImplFastCorner"+minContinuous+"_"+imageType.getAbbreviatedType();

		this.imageType = imageType;
		this.sumType = imageType.getSumType();
		this.bitwise = imageType.getBitWise();
		this.dataType = imageType.getDataType();
		this.minContinuous = minContinuous;

		initFile();
		printPreamble();

		generateSamples();

		out.println("}");
		System.out.println("Done");
	}

	private void generateSamples() {
		out.print(
				"\t/**\n" +
						"\t * @return 1 = positive corner, 0 = no corner, -1 = negative corner\n" +
						"\t */\n" +
						"\t@Override\n" +
						"\tpublic final int checkPixel( int index )\n" +
						"\t{\n" +
						"\t\tsetThreshold(index);\n"+
						"\n");

		for (int i = 0; i < samples.length; i++) {
			samples[i].clear();
		}
		Arrays.fill(detectedUp,false);
		Arrays.fill(detectedDown,false);

		tabs = 2;

		Stack<Action> actions = new Stack<>();
		actions.add(selectNextSample());
		while( !actions.empty() ) {
			if( actions.size() == 9 ) {
//				System.out.println("Foo");
			}
			Action action = actions.peek();
			System.out.println("Action bit="+action.bit+" up="+action.sampleUp+" n="+action.consider+" TOTAL="+actions.size());
			printDetected();
			printSampleState();

			if( action.consider == 0 ) {
				// First time this action is considered assume it's outcome is true
				printSample(tabs++,action);
				action.consider++;
				if( action.sampleUp ) {
					samples[action.bit].add(Sample.UP);
				} else {
					samples[action.bit].add(Sample.DOWN);
				}
			} else if( action.consider == 1 ){
				// Second time consider what to do if it's outcome is false
				printElse(tabs++);
				action.consider++;
				removeSample(action.bit);
				System.out.println("removed sample");
				printSampleState();
				updateSamples(action);
			} else {
				// Remove consideration of this action and reconsider the previous one
				removeSample(action.bit);
				printCloseIf(tabs--);
				actions.pop();
				continue;
			}

			// See if a solution has been found.
			Solution solution = checkSoluton();
			if( solution != null ) {
				// If a solution hsa been found return and mark the first bit as being found so that
				// it won't detect the same corner twice
				printReturn(tabs--,solution.up?1:-1);
				if( solution.up ) {
//					if( detectedUp[solution.firstBit])
//						throw new RuntimeException("BUG! Already detected");
					detectedUp[solution.firstBit] = true;
				} else {
//					if( detectedDown[solution.firstBit])
//						throw new RuntimeException("BUG! Already detected");
					detectedDown[solution.firstBit] = true;
				}
				// Don't add a new action. Instead consider other outcomes from previous action
			} else {
				// Wasn't able to find a solution. Sample another bit
				action = selectNextSample();
				if( action == null ) {
					// No need to sample since it has proven that there is no pixel
					printReturn(tabs--,0);
				} else {
					actions.add(action);
				}
			}
		}

		printCloseIf(1);
	}

	private void printDetected() {
		System.out.print("  D=");
		for (int i = 0; i < TOTAL_CIRCLE; i++) {
			int v = ((detectedUp[i]?1:0) << 1) | ((detectedDown[i]?1:0));
			System.out.print(v);
		}
		System.out.println();
	}

	private void printSampleState() {
		System.out.print("  S=");
		for (int i = 0; i < samples.length; i++) {
			System.out.print(sampleAt(i).ordinal());
		}
		System.out.println();
	}

	private void updateSamples(Action action) {
		if( action.sampleUp ) {
			switch (sampleAt(action.bit)) {
				case UNKNOWN:
					samples[action.bit].add(Sample.NOT_UP);
					break;

				case NOT_DOWN:
					samples[action.bit].add(Sample.NEITHER);
					break;

				default:
					throw new RuntimeException("BUG!");
			}
		} else {
			switch (sampleAt(action.bit)) {
				case UNKNOWN:
					samples[action.bit].add(Sample.NOT_DOWN);
					break;

				case NOT_UP:
					samples[action.bit].add(Sample.NEITHER);
					break;

				default:
					throw new RuntimeException("BUG!");
			}
		}
	}

	private Solution checkSoluton() {
		for (int i = 0; i < TOTAL_CIRCLE; ) {
			boolean success = true;
			for (int j = 0; j < minContinuous; j++) {
				int index = (i+j)%TOTAL_CIRCLE;
				if( sampleAt(index) != Sample.UP) {
					i += j+1;
					success = false;
				}
			}
			if( success ) {
				return new Solution(i,true);
			}
		}
		for (int i = 0; i < TOTAL_CIRCLE; ) {
			boolean success = true;
			for (int j = 0; j < minContinuous; j++) {
				int index = (i+j)%TOTAL_CIRCLE;
				if( sampleAt(index) != Sample.DOWN) {
					i += j+1;
					success = false;
				}
			}
			if( success )
				return new Solution(i,false);
		}
		return null;
	}

	private void printCloseIf( int numTabs ) {
		out.println(tabs(numTabs)+"}");
	}
	private void printElse( int numTabs ) {
		out.println(tabs(numTabs)+"} else {");
	}

	private void printSample( int numTabs , Action action ) {
		String comparison =  action.sampleUp ? "> upper" : "< lower";
		String strElse = action.consider==1 ? "} else " : "";
		out.println(tabs(numTabs)+strElse+"if( "+readBit(action.bit)+" "+comparison+" ) {");
	}

	private void printReturn( int numTabs , int value ) {
		out.println(tabs(numTabs) + "return " + value + ";");
	}

	private String readBit( int bit ) {
		return "(data[index+offsets["+bit+"]]"+bitwise+")";
	}


	private String tabs( int depth ) {
		String ret = "";
		for( int i = 0; i < depth; i++ ) {
			ret += "\t";
		}
		return ret;
	}

	private Action selectNextSample() {
		// compute number of possible corners that go through each pixel
		updatePossible(possibleUp,true);
		updatePossible(possibleDown,false);

		int max = 0;
		int secondary = 0;
		int which = -1;
		boolean sampleUp = false;

		for (int i = 0; i < TOTAL_CIRCLE; i++) {
			int up = possibleUp[i];
			int down = possibleDown[i];

			boolean canSampleUp = sampleAt(i) == Sample.UNKNOWN || sampleAt(i) == Sample.NOT_DOWN;
			boolean canSampleDn = sampleAt(i) == Sample.UNKNOWN || sampleAt(i) == Sample.NOT_UP;

			if( canSampleUp ) {
				if( max == up && secondary < down ) {
					secondary = down;
					which = i;
					sampleUp = true;
				} else if( max < up ) {
					max = up;
					secondary = down;
					which = i;
					sampleUp = true;
				}
			}
			if( canSampleDn ) {
				if( max == down && secondary < up ) {
					secondary = up;
					which = i;
					sampleUp = false;
				} else if( max < down ) {
					max = down;
					secondary = up;
					which = i;
					sampleUp = false;
				}
			}
		}

		if( which != -1 ) {
			return new Action(which,sampleUp);
		} else {
			return null;
		}
	}

	private void updatePossible( int possibles[], boolean up ) {
		Arrays.fill(possibles,0);
		for (int i = 0; i < TOTAL_CIRCLE; i++) {
			// see if the corner being considered has already been detected. If so skip it
			boolean detected[] = up? detectedUp : detectedDown;
			if( detected[i] )
				continue;
			boolean possible = true;
			for (int j = 0; j < minContinuous; j++) {
				int index = (i+j)%TOTAL_CIRCLE;
				Sample s = sampleAt(index);
				if( up ) {
					if( s == Sample.NOT_UP || s == Sample.DOWN || s == Sample.NEITHER ) {
						possible = false;
					}
				} else {
					if( s == Sample.NOT_DOWN || s == Sample.UP || s == Sample.NEITHER ) {
						possible = false;
					}
				}
			}
			for (int j = 0; possible && j < minContinuous; j++){
				int index = (i + j) % TOTAL_CIRCLE;
				possibles[index]++;
			}
		}
	}

	private void printPreamble() {
		out.print(
				"\n"+
						"/**\n" +
						" * <p>\n" +
						" * Contains logic for detecting fast corners. Pixels are sampled such that they can eliminate the most\n" +
						" * number of possible corners, reducing the number of samples required.\n" +
						" * </p>\n" +
						" *\n" +
						" * <p>\n" +
						" * DO NOT MODIFY. Generated by "+getClass().getSimpleName()+".\n" +
						" * </p>\n" +
						" *\n" +
						" * @author Peter Abeles\n" +
						" */\n" +
						"public class "+className+" extends ImplFastHelper_"+imageType.getAbbreviatedType()+"\n" +
						"{\n" +
						"\n" +
						"\tpublic "+className+"("+sumType+" pixelTol) {\n" +
						"\t\tsuper(pixelTol);\n" +
						"\t}\n\n");
	}

	public Sample sampleAt( int bit ) {
		List<Sample> s = samples[bit];
		if( s.isEmpty() )
			return Sample.UNKNOWN;
		else
			return s.get(s.size()-1);
	}

	public void removeSample( int bit ) {
		List<Sample> s = samples[bit];
		if( s.isEmpty() )
			throw new RuntimeException("BUG!");
		s.remove(s.size()-1);
	}

	static class Solution
	{
		int firstBit;
		boolean up;

		public Solution(int firstBit, boolean up) {
			this.firstBit = firstBit;
			this.up = up;
		}
	}

	static class Action
	{
		int bit;
		boolean sampleUp;
		int consider = 0;

		public Action(int bit, boolean sampleUp) {
			this.bit = bit;
			this.sampleUp = sampleUp;
		}
	}

	enum Sample
	{
		UNKNOWN,
		UP,
		DOWN,
		NOT_UP,
		NOT_DOWN,
		NEITHER
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateImplFastCorner gen = new GenerateImplFastCorner();
		gen.generate();
	}

}
