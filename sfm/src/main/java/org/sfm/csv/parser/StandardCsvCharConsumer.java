package org.sfm.csv.parser;


import java.io.IOException;

/**
 * Consume the charBuffer.
 */
public final class StandardCsvCharConsumer extends CsvCharConsumer {

	private static final int NOTHING = 8;
	private static final int IN_QUOTE = 4;
	private static final int IN_CR = 2;
	private static final int QUOTE = 1;
	private static final int NONE = 0;
	private static final int TURN_OFF_NOTHING = ~NOTHING;
	private static final int TURN_OFF_IN_CR_MASK = ~IN_CR;
	private static final int ALL_QUOTES = QUOTE | IN_QUOTE;

	private final CharBuffer csvBuffer;
	private int _currentIndex;
	private int _currentState = NONE;

	public StandardCsvCharConsumer(CharBuffer csvBuffer) {
		this.csvBuffer = csvBuffer;
	}

	@Override
	public final void consumeAllBuffer(CellConsumer cellConsumer) {
		int bufferLength = csvBuffer.getBufferSize();
		char[] chars = csvBuffer.getCharBuffer();
		int currentIndex = _currentIndex;
		int currentState = _currentState;
		while(currentIndex  < bufferLength) {
			char character = chars[currentIndex];
			currentState = consumeOneChar(cellConsumer, currentIndex, currentState, character);
			currentIndex++;
		}
		_currentState = currentState;
		_currentIndex = currentIndex;
	}

	private int consumeOneChar(CellConsumer cellConsumer, int currentIndex, int currentState, char character) {
		switch(character) {
            case ',':
                return newCellIfNotInQuote(currentIndex, currentState, cellConsumer);
            case '\n':
                return handleEndOfLineLF(currentIndex, currentState, cellConsumer);
            case '\r':
                return handleEndOfLineCR(currentIndex, currentState, cellConsumer);
            case '"':
                return quote(currentIndex, currentState);
        }
		return currentState;
	}

	@Override
	public boolean consumeToNextRow(CellConsumer cellConsumer) {
		int bufferLength = csvBuffer.getBufferSize();
		char[] chars = csvBuffer.getCharBuffer();
		int currentIndex = _currentIndex;
		int currentState = _currentState;
		while(currentIndex  < bufferLength) {
			char character = chars[currentIndex];
			switch(character) {
				case ',':
					currentState = newCellIfNotInQuote(currentIndex, currentState, cellConsumer);
					break;
				case '\n':
					currentState = handleEndOfLineLF(currentIndex, currentState | NOTHING, cellConsumer);
					if (currentState == NONE) {
						_currentState = currentState;
						_currentIndex = currentIndex + 1;
						return true;
					}
					currentState &= TURN_OFF_NOTHING;
					break;
				case '\r':
					currentState = handleEndOfLineCR(currentIndex, currentState | NOTHING, cellConsumer);
					if (currentState == IN_CR) {
						_currentState = currentState;
						_currentIndex = currentIndex + 1;
						return true;
					}
					currentState &= TURN_OFF_NOTHING;
					break;
				case '"':
					currentState = quote(currentIndex, currentState);
					break;
				default:
			}
			currentIndex++;
		}
		_currentState = currentState;
		_currentIndex = currentIndex;
		return false;
	}


	protected final int newCellIfNotInQuote(int currentIndex, int currentState, CellConsumer cellConsumer) {
		if ((currentState &  IN_QUOTE) != 0) return currentState & TURN_OFF_IN_CR_MASK;
		return newCell(currentIndex, cellConsumer);
	}

	protected final int handleEndOfLineLF(int currentIndex, int currentState, CellConsumer cellConsumer) {
		final int inQuoteAndCr = currentState & (IN_QUOTE | IN_CR);
		if (inQuoteAndCr == IN_CR) {
			// we had a preceding cr so shift the mark
			csvBuffer.mark(currentIndex + 1);
		} else if (inQuoteAndCr == 0) {
			return endOfRow(currentIndex, cellConsumer);
		}
		return currentState & TURN_OFF_IN_CR_MASK;
	}

	protected final int handleEndOfLineCR(int currentIndex, int currentState, CellConsumer cellConsumer) {
		if ((currentState &  IN_QUOTE) == 0) {
			endOfRow(currentIndex, cellConsumer);
			return IN_CR;
		}
		return currentState;
	}

	private final int endOfRow(int currentIndex, CellConsumer cellConsumer) {
		newCell(currentIndex, cellConsumer);
		cellConsumer.endOfRow();
		return NONE;
	}

	protected final int quote(int currentIndex, int currentState) {
		if (isNotAllConsumedFromMark(currentIndex)) {
			return currentState ^ ALL_QUOTES;
		} else {
			return currentState | IN_QUOTE;
		}
	}

	protected final int newCell(int end, final CellConsumer cellConsumer) {
		char[] charBuffer = csvBuffer.getCharBuffer();
		int start = csvBuffer.getMark();
		if (charBuffer[start] != '"') {
			cellConsumer.newCell(charBuffer, start, end - start);
		} else {
			newEscapedCell(charBuffer, start, end, cellConsumer);
		}
		csvBuffer.mark(end + 1);
		return NONE;

	}

	protected final void newEscapedCell(final char[] chars, final int offset, final int end, CellConsumer cellConsumer) {

		int start = offset + 1;

		boolean notEscaped = true;
		// copy chars apart from escape chars
		int realIndex = start;
		for(int i = start; i < end; i++) {
			notEscaped = !notEscaped || '"' != chars[i];
			chars[realIndex] = chars[i];
			if (notEscaped) {
				realIndex++;
			}
		}
		cellConsumer.newCell(chars, start, realIndex - start);
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
