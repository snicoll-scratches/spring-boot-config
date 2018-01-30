package net.nicoll.boot.config.diff;

import java.text.BreakIterator;
import java.util.Locale;

/**
 * Extract a single sentence from a potentially multi-lines description.
 *
 * @author Stephane Nicoll
 */
public class SentenceExtractor {

	public static String getFirstSentence(String text) {
		if (text == null) {
			return null;
		}
		int dot = text.indexOf('.');
		if (dot != -1) {
			BreakIterator breakIterator = BreakIterator.getSentenceInstance(Locale.US);
			breakIterator.setText(text);
			String sentence = text
					.substring(breakIterator.first(), breakIterator.next()).trim();
			return removeSpaceBetweenLine(sentence);
		}
		else {
			String[] lines = text.split(System.lineSeparator());
			return lines[0].trim();
		}
	}

	private static String removeSpaceBetweenLine(String text) {
		String[] lines = text.split(System.lineSeparator());
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append(line.trim()).append(" ");
		}
		return sb.toString().trim();
	}

}
