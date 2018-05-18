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
 * Generates a FAST corner detector. A heuristic is used to automatically select which pixels on the circle to sample
 * by trying to eliminate the number of possible solutions as fast as possible. This is done by selecting the bit
 * in which the greatest number of corners could contain it.
 *
 * This produces code which should be functionally identical to what's described in the paper by very different
 * from what the original author posted online. Appear to be much smaller.
 *
 * The code has been optimized for the JVM by splitting it into functions. if a function is too large/complex
 * the JIT appears to have problems optimizing the code.
 *
 * @author Peter Abeles
 */
public class GenerateImplFastCorner extends CodeGeneratorBase {

	private static final int TOTAL_CIRCLE = 16;

	// generate inner functions up to this depth
	private static final int MAX_FUNCTION_DEPTH = 2;

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

		for (int i = 0; i < samples.length; i++) {
			samples[i].clear();
		}

		List<String> codes  = new ArrayList<>();
		List<String> names = new ArrayList<>();

		// Need to split the code into smaller function to help the JVM optize the code
		codes.add(generateSamples());
		names.add("DUMMY");
		splitIntoFunctions(codes,names,0,0);

		out.print(
				"\t/**\n" +
						"\t * @return 1 = positive corner, 0 = no corner, -1 = negative corner\n" +
						"\t */\n" +
						"\t@Override\n" +
						"\tpublic final int checkPixel( int index )\n" +
						"\t{\n" +
						"\t\tsetThreshold(index);\n"+
						"\n");
		out.println(codes.get(0));
		out.println("\t}\n");

		for (int i = 1; i < codes.size(); i++) {
			String inside = codes.get(i);
			inside = "\tpublic final int "+names.get(i)+"( int index ) {\n" + inside + "\n\t}\n";
			out.println(inside);
		}

		out.println("}");
		System.out.println("Done");
	}

	private void splitIntoFunctions( List<String> codes , List<String> names, int depth , int which ) {

		if( depth >= MAX_FUNCTION_DEPTH ) {
			return;
		}

		int N = codes.size();
		String code = codes.get(which);

		String functionNameA = "function"+(N+1);
		String functionNameB = "function"+(N+2);

		int index0 = code.indexOf(") {\n")+4;
		int index1 = code.indexOf("\n\t\t} else {");
		int index2 = index1 + 12;
		int index3 = code.length()-4;

		String mainFunction = code.substring(0,index0);
		mainFunction += "\t\t\treturn "+functionNameA+"( index );";
		mainFunction += code.substring(index1,index2);
		mainFunction += "\t\t\treturn "+functionNameB+"( index );\n";
		mainFunction += "\t\t}\n";

		codes.set(which,mainFunction);

		String inside0 = code.substring(index0,index1);
		String inside1 = code.substring(index2,index3);

		inside0 = inside0.replaceAll("^\\t\\t\\t","\t\t");
		inside0 = inside0.replaceAll("\\n\\t\\t\\t","\n\t\t");
		inside1 = inside1.replaceAll("^\\t\\t\\t","\t\t");
		inside1 = inside1.replaceAll("\\n\\t\\t\\t","\n\t\t");

		int indexA = names.size();
		int indexB = indexA+1;
		names.add( functionNameA); codes.add(inside0);
		names.add( functionNameB); codes.add(inside1);
		splitIntoFunctions(codes,names,depth+1,indexA);
		splitIntoFunctions(codes,names,depth+1,indexB);
	}

	// TODO in each branch exhaust all

	// TODO keep a list of bits which are finished and there's a direct path from the root which does not reply on other bits
	// TODO only be considered finished when that list is exhausted
	private String generateSamples() {
		String output = "";

		tabs = 2;

		Stack<Action> actions = new Stack<>();
		actions.add(selectNextSample());
		while( !actions.empty() ) {
			Action action = actions.peek();
			System.out.println("Action bit="+action.bit+" up="+action.sampleUp+" n="+action.consider+" TOTAL="+actions.size());
			debugSampleState();

			if( action.consider == 0 ) {
				// First time this action is considered assume it's outcome is true
				output += strSample(tabs++,action);
				action.consider++;
				if( action.sampleUp ) {
					samples[action.bit].add(Sample.UP);
				} else {
					samples[action.bit].add(Sample.DOWN);
				}
			} else if( action.consider == 1 ){
				// Second time consider what to do if it's outcome is false
				output += strElse(tabs++);
				action.consider++;
				removeSample(action.bit);
				System.out.println("removed sample");
				debugSampleState();
				updateSamples(action);
			} else {
				// Remove consideration of this action and reconsider the previous one
				removeSample(action.bit);
				output += strCloseIf(tabs--);
				actions.pop();
				continue;
			}

			// See if a solution has been found.
			Solution solution = checkSoluton();
			if( solution != null ) {
				// If a solution hsa been found return and mark the first bit as being found so that
				// it won't detect the same corner twice
				output += strReturn(tabs--,solution.up?1:-1);
				// Don't add a new action. Instead consider other outcomes from previous action
			} else {
				// Wasn't able to find a solution. Sample another bit
				action = selectNextSample();
				if( action == null ) {
					// No need to sample since it has proven that there is no pixel
					output += strReturn(tabs--,0);
				} else {
					actions.add(action);
				}
			}
		}

		return output;
	}

	private void debugSampleState() {
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

	private String strCloseIf(int numTabs ) {
		return tabs(numTabs)+"}\n";
	}
	private String strElse(int numTabs ) {
		return tabs(numTabs)+"} else {\n";
	}

	private String strSample(int numTabs , Action action ) {
		String comparison =  action.sampleUp ? "> upper" : "< lower";
		String strElse = action.consider==1 ? "} else " : "";
		return tabs(numTabs)+strElse+"if( "+readBit(action.bit)+" "+comparison+" ) {\n";
	}

	private String strReturn(int numTabs , int value ) {
		return tabs(numTabs) + "return " + value + ";\n";
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
