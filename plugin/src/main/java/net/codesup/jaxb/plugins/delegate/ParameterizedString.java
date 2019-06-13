/*
 * GNU General Public License
 *
 * Copyright (c) 2018 Klemm Software Consulting, Mirko Klemm
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.codesup.jaxb.plugins.delegate;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParameterizedString {
	public static final Pattern DEFAULT_PARAMETER_NAME_PATTERN = Pattern.compile("\\{(.*?)}");
	private final Pattern parameterNamePattern;
	private final Function<String,String> parameterHandler;

	public ParameterizedString(final Function<String, String> parameterHandler) {
		this.parameterNamePattern = DEFAULT_PARAMETER_NAME_PATTERN;
		this.parameterHandler = parameterHandler;
	}

	public ParameterizedString(final Pattern parameterNamePattern, final Function<String, String> parameterHandler) {
		this.parameterNamePattern = parameterNamePattern;
		this.parameterHandler = parameterHandler;
	}

	public ParameterizedString(final String parameterNamePatternString, final Function<String, String> parameterHandler) {
		this.parameterNamePattern = Pattern.compile(parameterNamePatternString);
		this.parameterHandler = parameterHandler;
	}

	public String applyParameters(final String input) {
		final Matcher matcher = parameterNamePattern.matcher(input);
		final StringBuffer b = new StringBuffer();
		while(matcher.find()) {
			final String replaced = this.parameterHandler.apply(matcher.group(1));
			matcher.appendReplacement(b, replaced);
		}
		matcher.appendTail(b);
		return b.toString();
	}

}
