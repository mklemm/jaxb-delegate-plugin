package net.codesup.jaxb.plugins.delegate;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;

public class ParameterizedType {
	public static final Pattern TYPEARG_REGEX = Pattern.compile("(\\w(?:\\w|\\.)*)(?:\\<(\\w(?:\\w|\\.|\\,|\\<|\\>)*)+\\>)?");

	public String getTypeName() {
		return this.typeName;
	}

	public List<ParameterizedType> getTypeArgs() {
		return this.typeArgs;
	}

	private final String typeName;
	private final List<ParameterizedType> typeArgs;

	public ParameterizedType(final String typeName, final List<ParameterizedType> typeArgs) {
		this.typeName = typeName;
		this.typeArgs = typeArgs;
	}

	public static ParameterizedType parse(final String spec) {
		int argStart = spec.indexOf('<');
		int argEnd = spec.lastIndexOf('>');
		final String typeName = spec.substring(0, argStart);
		final String innerSpecs = spec.substring(argStart + 1, argEnd);

	}

	public JClass createModelClass(final JCodeModel model) {
		return model.ref(this.typeName).narrow(this.typeArgs.stream().map(a -> a.createModelClass(model)).collect(Collectors.toList()));
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(this.typeName);
		if(!this.typeArgs.isEmpty()) {
			sb.append("<");
			boolean first = true;
			for (final ParameterizedType typeArg : this.typeArgs) {
				if (first) {
					first = false;
				} else {
					sb.append(",");
				}
				sb.append(typeArg.toString());
			}
			sb.append(">");
		}
		return sb.toString();
	}
}
