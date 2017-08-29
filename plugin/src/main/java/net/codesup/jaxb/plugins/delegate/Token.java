package net.codesup.jaxb.plugins.delegate;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple token parser
 */
public class Token {
	private final char open;
	private final char sep;
	private final char close;
	private final Token parent;
	private final StringBuilder content = new StringBuilder();
	private final List<Token> children = new ArrayList<>();

	public Token(final Token parent, final char open, final char sep, final char close) {
		this.open = open;
		this.sep = sep;
		this.close = close;
		this.parent = parent;
		if(parent != null) {
			parent.children.add(this);
		}
	}

	private Token parseChar(final char c) {
		if(c == this.open) {
			return new Token(this, this.open, this.sep, this.close);
		} else if(c == this.close) {
			return this.parent;
		} else if(c== this.sep) {
			return new Token(this.parent, this.open, this.sep, this.close);
		} else {
			this.content.append(c);
			return this;
		}
	}

	public static Token parse(final String s, final char open, final char sep, final char close) {
		final Token root = new Token(null, open, sep, close);
		Token t = root;
		for(int i = 0; i < s.length(); i++) {
			t = t.parseChar(s.charAt(i));
		}
		return root;
	}

	@Override
	public String toString() {
		return this.content.toString() + this.children.stream().map(Token::toString).reduce((s1,s2) -> s1 + this.sep + s2).map(s -> this.open + s + this.close ).orElse("");
	}

	public String toInfixString() {
		return this.open + this.content.toString() + this.children.stream().map(Token::toInfixString).reduce((s1,s2) -> s1 + this.sep + s2).orElse("") + this.close;
	}

	public String toPostfixString() {
		return this.open  + this.children.stream().map(Token::toPostfixString).reduce((s1,s2) -> s1 + this.sep + s2).orElse("") + this.content.toString() + this.close;
	}

	public String getContent() {
		return this.content.toString();
	}

	public List<Token> getChildren() {
		return this.children;
	}
}
