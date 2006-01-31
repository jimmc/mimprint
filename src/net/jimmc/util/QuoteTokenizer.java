/* QuoteTokenizer.java
 *
 * Jim McBeath, March 19, 2002
 */

package net.jimmc.util;

/** Tokenize a String which may contain quoted strings.
 */
public class QuoteTokenizer {
    /** Our string quoting character. */
    public static final char QUOTER = '"';

    /** Alternate string quoter. */
    public static final char QUOTER2 = '\'';

    /** Our character-quoting character within a string. */
    public static final char INTRAQUOTER = '\\';

    /** The string we are parsing. */
    protected String line;

    /** The length of the line being parsed. */
    protected int len;

    /** The position at which to start parsing on the next call to
     * {@link #nextToken}.
     */
    protected int pos;

    /** True if the string most recently returned by {@link #nextToken}
     * was a quoted string.
     * @see #wasQuoted
     */
    protected boolean quoted;

    /** The character used to quote the most recently returned
     * quoted string.
     * @see #getQuoter
     */
    protected char quoter;

    /** A buffer we use while parsing a quoted string. */
    protected StringBuffer qbuf;

    /** Create a tokenizer.
     * @see #setInput
     */
    public QuoteTokenizer() {
    }

    /** Create a tokenizer for a string. */
    public QuoteTokenizer(String line) {
	setInput(line);
    }

    /** Set the string to be parsed. */
    public void setInput(String line) {
	this.line = line;
	if (line==null)
	    len = 0;
	else
	    len = line.length();
	pos = 0;
    }

    /** Get the current position.
     * This is 0 before the first call to nextToken.
     */
    public int getPosition() {
	return pos;
    }

    /** Return the next token.
     * White space is used to separate tokens, but is not returned.
     * @return The next token, or null if no more.
     *         If the string was quoted, the quotes are removed,
     *         and a flag is set so that a call to
     *         {@link #wasQuoted} will return true.
     */
    public Object nextToken() {
	quoted = false;
	skipWhitespace();
	if (line==null || pos>=len)
	    return null;	//no input available
	char c = line.charAt(pos);

	if (isQuotedStart(c))
	    return getQuoted(c);
	if (isNumberStart(c))
	    return getNumber();
	if (isIdentifierStart(c))
	    return getIdentifier();

	//Unknown character, just return it as a token
	pos++;
	return String.valueOf(c);
    }

    /** Skip white space. */
    protected void skipWhitespace() {
	for (;pos<len; pos++) {
	    char c = line.charAt(pos);
	    if (!Character.isWhitespace(c))
		return;
	}
    }

    /** True if this character starts a quoted string. */
    protected boolean isQuotedStart(char c) {
	return (c==QUOTER || c==QUOTER2);
    }

    /** True if this character starts an identifier. */
    protected boolean isIdentifierStart(char c) {
	return (Character.isLetter(c) || c=='_');
    }

    /** True if this character can be in an identifier. */
    protected boolean isIdentiferPart(char c) {
	return (Character.isLetterOrDigit(c) || c=='_');
    }

    /** True if this character starts a number. */
    protected boolean isNumberStart(char c) {
	return Character.isDigit(c) || c=='-';
    }

    /** True if this character can be part of a number. */
    protected boolean isNumberPart(char c) {
	return (Character.isDigit(c) || c=='.');
    }

    /** Parse out a quoted string.
     * pos points to the initial quote character.
     * @param quoter The quoting character.
     */
    protected String getQuoted(char quoter) {
	this.quoter = quoter;
	if (qbuf==null)
	    qbuf = new StringBuffer();
	else
	    qbuf.setLength(0);
	for (pos++; pos<len; pos++) {
	    char c = line.charAt(pos);
	    if (c==quoter) {
		//Got the end of the quoted string
		pos++;
		break;
	    }
	    if (c==INTRAQUOTER) {	//backslash
		if (pos++>=len) {
		    //Error, no char after backslash
		    //TBD - throw exception
		    return null;
		}
		c = line.charAt(pos);
		switch (c) {
		case 'n':
		    qbuf.append("\n");
		    break;
		case 't':
		    qbuf.append("\t");
		    break;
		case '\\':
		    qbuf.append("\\");
		    break;
                case '\'':
                    qbuf.append("'");
                    break;
                case '"':
                    qbuf.append('"');
                    break;
		//TBD - handle \nnn format
		default:
		    //ignore it
		    break;
		}
	    } else {
		//Not a special character
		qbuf.append(c);
	    }
	}
	quoted = true;
	return qbuf.toString();
    }

    /** Parse out an identifier string.
     * pos points to the initial character.
     */
    protected String getIdentifier() {
	int startPos = pos;
	for (pos++; pos<len; pos++) {
	    char c = line.charAt(pos);
	    if (!isIdentiferPart(c))
		break;
	}
	return line.substring(startPos,pos);
    }

    /** Get a number string.
     * pos points to the initial digit.
     */
    protected Number getNumber() {
	int startPos = pos;
	for (pos++; pos<len; pos++) {
	    char c = line.charAt(pos);
	    if (!isNumberPart(c))
		break;
	}
	String s = line.substring(startPos,pos);
	return stringToNumber(s);
    }

    /** Convert a string representation of a number to a Number. */
    protected Number stringToNumber(String s) {
	try {
	    int n = Integer.parseInt(s);
	    return new Integer(n);
	} catch (NumberFormatException ex) {
	    //ignore the error, try another format
	}

	try {
	    double d = Double.parseDouble(s);
	    return new Double(d);
	} catch (NumberFormatException ex) {
	    //ignore the error, try another format
	}

	//We don't recognize the format
	throw new NumberFormatException(s);
    }

    /** True if the string most recently returned by {@link #nextToken}
     * was quoted.
     */
    public boolean wasQuoted() {
	return quoted;
    }

    /** The character used to quote the most recently quoted string.
     */
    public char getQuoter() {
	return quoter;
    }
}

/* end */
