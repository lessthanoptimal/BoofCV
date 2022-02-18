/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial.aztec;

import boofcv.alg.fiducial.aztec.AztecCode.Mode;
import boofcv.misc.BoofMiscOps;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Automatic encoding algorithm as described in [1] which seeks to encode text in a format which minimizes the amount
 * of storage required.
 *
 * <ul>
 *     <li>latch = switching into a mode until it switches out of it</li>
 *     <li>shift = switching into a mode for a single then going back into the source mode</li>
 * </ul>
 *
 * <p>[1] ISO/IEC 24778:2008(E)</p>
 *
 * @author Peter Abeles
 */
public class AztecEncoderAutomatic implements VerbosePrint {
	// Modes it will consider when automatically encoding
	public static final Mode[] modes = new Mode[]{Mode.UPPER, Mode.LOWER, Mode.MIXED, Mode.PUNCT, Mode.DIGIT, Mode.BYTE};

	// State of each mode
	final DogArray<State> states = new DogArray<>(State::new, State::reset);

	// Shorthand for infinite cost
	final static int E = 0x0fffffff;

	// Latch length table. bits to latch from one mode to another mode
	// This can involve transitioning between multiple modes or adding length bits for BYTE mode
	final static int[][] latlen = new int[][]{
			{0, 5, 5, 10, 5, 10},
			{10, 0, 5, 10, 5, 10},
			{5, 5, 0, 5, 10, 10},
			{5, 10, 10, 0, 10, 15},
			{4, 9, 9, 14, 0, 14},
			{0, 0, 0, 0, 0, 0}};

	// Shift length table. bits to shift from one mode to another mode.
	final static int[][] shiftlen = new int[][]{
			{E, E, E, 5, E},
			{5, E, E, 5, E},
			{E, E, E, 5, E},
			{E, E, E, E, E},
			{4, E, E, 4, E}};

	@Nullable PrintStream verbose = null;

	/**
	 * Processes and encodes the string
	 *
	 * @param message (Input) String that's to be encoded
	 * @param encoder (Input/Output) Encoder for parses segments
	 */
	public void process( String message, AztecEncoder encoder ) {
		byte[] characters = message.getBytes(StandardCharsets.ISO_8859_1);

		initialize();

		encodeCharacters(characters);

		encodeMessageGivenSequence(selectBestState(), message, encoder);
	}

	void initialize() {
		states.resetResize(latlen.length);
		State s = states.get(Mode.UPPER.ordinal());
		s.curLen = 0;
		s.characterCount = 0;
		s.sequence.grow().setTo(Mode.UPPER, 0);
	}

	void encodeCharacters( byte[] characters ) {
		for (int charIdx = 0; charIdx < characters.length; charIdx++) {
			int curr = characters[charIdx] & 0xFF;
			if (verbose != null)
				verbose.println("charIdx=" + charIdx + " value=" + curr + " char='" + (char)characters[charIdx] + "'");

			// Step 1: See if any of the encodings could be made shorter if they latched from another mode
			latchToReduceMessageSize(charIdx);

			// Step 2: Set all nxtLen to 0
			states.forEach(m -> m.nxtLen = 0);

			// Step 3: Find all modes which can encode this character
			addCharacterToStates(curr);

			// See if it should do a shift. Can't shift into or out of byte, hence -1
			considerShiftingInstead(charIdx, curr);

			// Step 4: Handle special 2-character sequence
			int prev = charIdx > 0 ? characters[charIdx - 1] & 0xFF : 0;
			if (isTwoCharacterSequence(prev, curr)) {
				// The second character isn't a standalone character and needs to be accounted for
				State state = states.get(Mode.PUNCT.ordinal());
				state.characterCount++;
				state.sequence.getTail().count++;
			}

			// Step 5: See if byte mode needs to have a longer length integer
			if (states.get(Mode.BYTE.ordinal()).sequence.getTail().count == 32) {
				states.get(Mode.BYTE.ordinal()).nxtLen += 11;
			}

			// Step 6: Transfer nxtLen to curLen
			states.forEach(m -> m.curLen = m.nxtLen);
		}
	}

	/**
	 * See if it makes sense to replace the sequence for one state with the sequence of another state then latch
	 * it into the state's target mode
	 *
	 * @param charIdx Index of the character being considered
	 */
	private void latchToReduceMessageSize( int charIdx ) {
		for (int modeIdxA = 0; modeIdxA < states.size; modeIdxA++) {
			State stateA = states.get(modeIdxA);
			// Make sure that the path leading to this mode has been able to encode everything up to this point
			if (stateA.characterCount != charIdx)
				continue;

			// See if it should transfer from this state into another state
			for (int modeIdxB = 0; modeIdxB < states.size; modeIdxB++) {
				if (modeIdxA == modeIdxB) {
					continue;
				}

				// If in byte mode and the previous mode is not modeB then it will need to transition into modeB
				boolean byteTransition = modes[modeIdxA] == Mode.BYTE && modes[modeIdxB] != stateA.backTo;

				// NUmber of bits if it transitioned from A to B
				int lengthIfTransition = stateA.curLen + latlen[modeIdxA][modeIdxB];
				if (byteTransition) {
					// cost of the extra transition
					lengthIfTransition += latlen[stateA.backTo.ordinal()][modeIdxB];
				}

				State stateB = states.get(modeIdxB);

				// See if the result would be a smaller encoding or if stateB is impossible to be in
				if (lengthIfTransition >= stateB.curLen && stateB.characterCount == charIdx)
					continue;

				if (verbose != null) verbose.println("latching " + modes[modeIdxA] + "->" + modes[modeIdxB]);

				// The encoding is better, so replace the history of B for the history in A
				stateB.curLen = lengthIfTransition;
				stateB.characterCount = stateA.characterCount;
				stateB.sequence.reset();
				stateB.sequence.copyAll(stateA.sequence.toList(), ( src, dst ) -> dst.setTo(src));
				stateB.backTo = modes[modeIdxA];

				// add in the extra byte mode transition
				if (byteTransition)
					stateB.sequence.grow().setTo(modes[modeIdxA], 0);

				// Add in this transition
				stateB.sequence.grow().setTo(modes[modeIdxB], 0);
			}
		}
	}

	/**
	 * If a mode can encode this character then add it to it's state
	 *
	 * @param curr character's value
	 */
	private void addCharacterToStates( int curr ) {
		for (int modeIdx = 0; modeIdx < modes.length; modeIdx++) {
			State state = states.get(modeIdx);

			if (!isMember(modeIdx, curr)) {
				// avoid zeroing curLen later on
				state.nxtLen = state.curLen;
				continue;
			}

			// See if encoding this character into this mode is better
			int length = state.curLen + modes[modeIdx].wordSize;
			if (state.nxtLen == 0 || state.nxtLen > length) {
				state.nxtLen = length;
				state.characterCount++;
				state.sequence.getTail().count++;

				if (verbose != null) verbose.printf("add %5s length=%d\n", modes[modeIdx], length);
			}
		}
	}

	/**
	 * See if it makes more sense to shift instead of latching
	 *
	 * @param curr character's value
	 */
	private void considerShiftingInstead( int charIdx, int curr ) {
		// outermost loop goes through modes that could be shifted into
		// modes.length - 1 because BYTE never has shifts
		for (int modeIdxA = 0; modeIdxA < modes.length - 1; modeIdxA++) {
			// The mode it would shift into must be compatible with the character
			if (!isMember(modeIdxA, curr))
				continue;

			// See if any shifts are possible and make sense
			for (int modeIdxB = 0; modeIdxB < modes.length - 1; modeIdxB++) {
				if (isMember(modeIdxB, curr) || shiftlen[modeIdxB][modeIdxA] == E)
					continue;

				State stateB = states.get(modeIdxB);
				int shiftLength = stateB.curLen + shiftlen[modeIdxB][modeIdxA] + modes[modeIdxA].wordSize;
				if (stateB.characterCount == (charIdx + 1) && shiftLength >= stateB.nxtLen)
					continue;

				// This can only be less expensive if it couldn't encode the current character
				stateB.nxtLen = shiftLength;
				stateB.characterCount++;
				// Add the shift character
				stateB.sequence.grow().setTo(modes[modeIdxA], 1);
				// Transition back into the mode
				stateB.sequence.grow().setTo(modes[modeIdxB], 0);
			}
		}
	}

	/**
	 * Some two character punctuations are encoded as a single character
	 */
	private boolean isTwoCharacterSequence( int a, int b ) {
		if (a == 13 && b == 10) {
			return true;
		} else if (b == 32) {
			if (a == 46) {
				return true;
			} else if (a == 44) {
				return true;
			} else if (a == 58) {
				return true;
			}
		}
		return false;
	}

	private void encodeMessageGivenSequence( State state, String message, AztecEncoder encoder ) {
		int char0 = 0;
		for (int i = 0; i < state.sequence.size; i++) {
			Group g = state.sequence.get(i);
			int char1 = char0 + g.count;
			switch (g.mode) {
				case UPPER -> encoder.addUpper(message.substring(char0, char1));
				case LOWER -> encoder.addLower(message.substring(char0, char1));
				case MIXED -> encoder.addMixed(message.substring(char0, char1));
				case PUNCT -> encoder.addPunctuation(message.substring(char0, char1));
				case DIGIT -> encoder.addDigit(message.substring(char0, char1));
				case BYTE -> {
					byte[] data = message.substring(char0, char1).getBytes(StandardCharsets.ISO_8859_1);
					encoder.addBytes(data, 0, data.length);
				}
				default -> throw new RuntimeException("Invalid");
			}
			char0 = char1;
		}
	}

	/**
	 * Selects the state with the smallest bit count
	 */
	State selectBestState() {
		State state = states.get(0);
		for (int i = 1; i < states.size; i++) {
			State candidate = states.get(i);
			if (candidate.characterCount > state.characterCount) {
				state = states.get(i);
			} else if (candidate.characterCount == state.characterCount && states.get(i).curLen < state.curLen) {
				state = states.get(i);
			}
		}
		return state;
	}

	/**
	 * Checks to see if the character is a memeber of the specified mode
	 */
	boolean isMember( int mode, int curr ) {
		return switch (modes[mode]) {
			case UPPER -> isUpper(curr);
			case LOWER -> isLower(curr);
			case MIXED -> isMixed(curr);
			case PUNCT -> isPunctuation(curr);
			case DIGIT -> isDigit(curr);
			case BYTE -> true;
			default -> throw new RuntimeException("Invalid");
		};
	}

	boolean isUpper( int c ) {
		if (c == 32) {
			return true;
		} else if (c >= 65 && c <= 90) {
			return true;
		}
		return false;
	}

	boolean isLower( int c ) {
		if (c == 32) {
			return true;
		} else if (c >= 97 && c <= 122) {
			return true;
		}
		return false;
	}

	boolean isMixed( int c ) {
		if (c >= 1 && c <= 13) {
			return true;
		} else if (c >= 27 && c <= 32) {
			return true;
		} else if (c == 64) {
			return true;
		} else if (c == 92) {
			return true;
		} else if (c == 94) {
			return true;
		} else if (c == 95) {
			return true;
		} else if (c == 96) {
			return true;
		} else if (c == 124) {
			return true;
		} else if (c == 126) {
			return true;
		} else if (c == 127) {
			return true;
		}
		return false;
	}

	public boolean isPunctuation( int c ) {
		if (c == 13) {
			return true;
		} else if (c >= 33 && c <= 47) {
			return true;
		} else if (c >= 58 && c <= 63) {
			return true;
		} else if (c == 91) {
			return true;
		} else if (c == 93) {
			return true;
		} else if (c == 123) {
			return true;
		} else if (c == 125) {
			return true;
		}
		return false;
	}

	boolean isDigit( int c ) {
		if (c == 32) {
			return true;
		} else if (c >= 48 && c <= 57) {
			return true;
		} else if (c == 44) {
			return true;
		} else if (c == 46) {
			return true;
		}
		return false;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}

	/**
	 * Encodes the state as described in Annex H.
	 */
	static class State {
		// Number of bits to encode it into this state
		int curLen = Integer.MAX_VALUE;
		// Number of bits to encode and the latest character
		int nxtLen = -1;

		// Number of characters it has encoded
		int characterCount = -1;

		// Sequence of character sets to get to this state
		DogArray<Group> sequence = new DogArray<>(Group::new, Group::reset);

		// Mode that the binary shift came from
		Mode backTo = Mode.UPPER;

		public void reset() {
			curLen = Integer.MAX_VALUE;
			nxtLen = -1;
			characterCount = -1;
			sequence.reset();
			backTo = Mode.UPPER;
		}
	}

	/** Character set and the number of characters. Group = group of characters */
	static class Group {
		Mode mode = Mode.UPPER;
		int count;

		public void reset() {
			mode = Mode.UPPER;
			count = 0;
		}

		public void setTo( Mode mode, int count ) {
			this.mode = mode;
			this.count = count;
		}

		public void setTo( Group g ) {
			mode = g.mode;
			count = g.count;
		}
	}
}
