package org.sfm.csv.parser;


import java.io.IOException;

/**
 * Consume the charBuffer.
 */
public final class ConfigurableTrimCsvCharConsumer extends CsvCharConsumer {
	protected static final int HAS_CONTENT = 8;
	protected static final int IN_QUOTE = 4;
	protected static final int IN_CR = 2;
	protected static final int QUOTE = 1;
	protected static final int NONE = 0;
	protected static final int TURN_OFF_IN_CR_MASK = ~IN_CR;
	protected static final int ALL_QUOTES = QUOTE | IN_QUOTE;

	protected final CharBuffer csvBuffer;
	protected final char quoteChar;
	protected int _currentIndex;
	protected int currentState = NONE;
	private final char separatorChar;

	public ConfigurableTrimCsvCharConsumer(CharBuffer csvBuffer, char separatorChar, char quoteChar) {
		this.csvBuffer = csvBuffer;
		this.quoteChar = quoteChar;
		this.separatorChar = separatorChar;
	}

	@Override
	public final void consumeAllBuffer(CellConsumer cellConsumer) {
		int bufferLength = csvBuffer.bufferSize;
		char[] chars = csvBuffer.buffer;
		int currentIndex = _currentIndex;
		while(currentIndex  < bufferLength) {
			consumeOneChar(currentIndex, chars[currentIndex], cellConsumer);
			currentIndex++;
		}

		_currentIndex = currentIndex;
	}

	private void consumeOneChar(int currentIndex, char character, CellConsumer cellConsumer) {
		if (character == separatorChar) {
			newCellIfNotInQuote(currentIndex, cellConsumer);
		} else if (character ==  '\n') {
				handleEndOfLineLF(currentIndex, cellConsumer);
		} else if (character == '\r') {
			handleEndOfLineCR(currentIndex, cellConsumer);
			return;
		} else if (character == quoteChar) {
			quote(currentIndex);
		} else if (character != ' ') {
			currentState |= HAS_CONTENT;
		}
		turnOffCrFlag();
	}


	protected final void quote(int currentIndex) {
		if (isAllConsumedFromMark(currentIndex)) {
			currentState |= IN_QUOTE;
		} else if ((currentState & (HAS_CONTENT | ALL_QUOTES)) != 0) {
			currentState ^= ALL_QUOTES;
		} else {
			currentState |= IN_QUOTE;
			csvBuffer.mark(currentIndex);
		}
	}

	private boolean isAllConsumedFromMark(int bufferIndex) {
		return (bufferIndex) <  (csvBuffer.getMark() + 1)  ;
	}


	@Override
	public boolean consumeToNextRow(CellConsumer cellConsumer) {

		int bufferLength = csvBuffer.getBufferSize();
		char[] buffer = csvBuffer.getCharBuffer();
		int currentIndex = _currentIndex;
		for(; currentIndex  < bufferLength; currentIndex++) {

			char character = buffer[currentIndex];

			if (character == separatorChar) {
				newCellIfNotInQuote(currentIndex, cellConsumer);
			} else if (character ==  '\n') {
				if (handleEndOfLineLF(currentIndex, cellConsumer)) {
					_currentIndex = currentIndex + 1;
					turnOffCrFlag();
					return true;
				}
			} else if (character == '\r') {
				if (handleEndOfLineCR(currentIndex, cellConsumer)) {
					_currentIndex = currentIndex + 1;
					return true;
				}
			} else if (character == quoteChar) {
				quote(currentIndex);
			} else if (character != ' ') {
				currentState |= HAS_CONTENT;
			}
			turnOffCrFlag();
		}

		_currentIndex = currentIndex;
		return false;
	}

	protected void newCell(int currentIndex, CellConsumer cellConsumer) {
		char[] charBuffer = csvBuffer.getCharBuffer();
		int start = csvBuffer.getMark();
		int length = currentIndex - start;

		if (charBuffer[start] == quoteChar) {
			unescape(charBuffer, start, currentIndex, cellConsumer);
		} else {
			int newStart = firstNonSpaceChar(charBuffer, start, length);
			length = length - newStart + start;
			start = newStart;
			length = lastNonSpaceChar(charBuffer, start + length, start) - start;
			cellConsumer.newCell(charBuffer, start, length);
		}

		csvBuffer.mark(currentIndex + 1);
		currentState = NONE;
	}

	private int lastNonSpaceChar(char[] charBuffer, int end, int start) {
		for(int i = end; i > start; i--) {
			if (charBuffer[i - 1] != ' ') return i;
		}
		return start;
	}

	private int firstNonSpaceChar(char[] charBuffer, int start, int length) {
		for(int i = start; i < start + length; i++) {
			if (charBuffer[i] != ' ') return i;
		}
		return start + length;
	}

	protected void unescape(final char[] chars, final int offset, final int end, CellConsumer cellConsumer) {
		int start = offset + 1;

		int lastCharacter = end;

		while(chars[lastCharacter - 1]  == ' ' && lastCharacter > offset) {
			lastCharacter --;
		}

		boolean notEscaped = true;
		char lQuoteChar = this.quoteChar;
		// copy chars apart from escape chars
		int realIndex = start;
		for(int i = start; i < lastCharacter; i++) {
			notEscaped = !notEscaped || lQuoteChar != chars[i];
			chars[realIndex] = chars[i];
			if (notEscaped) {
				realIndex++;
			}
		}
		cellConsumer.newCell(chars, start, realIndex - start);
	}

	/**
	 * use bit mask to testing if == IN_CR
	 */
	protected final void turnOffCrFlag() {
		currentState &= TURN_OFF_IN_CR_MASK;
	}

	protected final void newCellIfNotInQuote(int currentIndex, CellConsumer cellConsumer) {
		if ((currentState &  IN_QUOTE) != 0) return;
		newCell(currentIndex, cellConsumer);
	}

	protected final boolean handleEndOfLineLF(int currentIndex, CellConsumer cellConsumer) {
		final int inQuoteAndCr = this.currentState & (IN_QUOTE | IN_CR);
		if (inQuoteAndCr == IN_CR) {
			// we had a preceding cr so shift the mark
			csvBuffer.mark(currentIndex + 1);
			return false;
		} else if (inQuoteAndCr == 0) {
			endOfRow(currentIndex, cellConsumer);
			return true;
		}
		return false;
	}

	protected final boolean handleEndOfLineCR(int currentIndex, CellConsumer cellConsumer) {
		if ((currentState &  IN_QUOTE) == 0) {
			endOfRow(currentIndex, cellConsumer);
			currentState |= IN_CR;
			return true;
		}
		return false;
	}

	private final void endOfRow(int currentIndex, CellConsumer cellConsumer) {
		newCell(currentIndex, cellConsumer);
		cellConsumer.endOfRow();
	}

	private void newEscapedCell(final char[] chars, final int offset, final int length, CellConsumer cellConsumer) {
		int start = offset + 1;
		int shiftedIndex = start;
		boolean escaped = false;

		int lastCharacter = offset + length - 1;


		// copy chars apart from escape chars
		for(int i = start; i < lastCharacter; i++) {
			escaped = quoteChar == chars[i] && !escaped;
			if (!escaped) {
				chars[shiftedIndex++] = chars[i];
			}
		}

		// if last is not quote add to shifted char
		if (quoteChar != chars[lastCharacter] || escaped) {
			chars[shiftedIndex++] = chars[lastCharacter];
		}

		cellConsumer.newCell(chars, start, shiftedIndex - start);
	}

	@Override
	public final void finish(CellConsumer cellConsumer) {
		int currentIndex = _currentIndex;
		if (isNotAllConsumedFromMark(currentIndex)) {
			newCell(currentIndex, cellConsumer);
		}
		cellConsumer.end();
	}

	private void shiftCurrentIndex(int mark) {
		_currentIndex -= mark;
	}

	@Override
	public final boolean refillBuffer() throws IOException {
		shiftCurrentIndex(csvBuffer.shiftBufferToMark());
		return csvBuffer.fillBuffer();
	}

	protected final boolean isNotAllConsumedFromMark(int bufferIndex) {
		return (bufferIndex) >=  (csvBuffer.getMark() + 1)  ;
	}

}
