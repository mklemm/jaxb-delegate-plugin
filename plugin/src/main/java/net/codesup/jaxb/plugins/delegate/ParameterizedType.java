package net.codesup.jaxb.plugins.delegate;

import java.util.List;
import java.util.stream.Collectors;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;

/**
 * Represents a parameterized type
 */
public class ParameterizedType {
	private static final char OPEN = '<';
	private static final char CLOSE = '>';
	private static final char SEP = ',';

	private final String typeName;
	private final List<ParameterizedType> typeArgs;

	private ParameterizedType(final Token token) {
		this.typeName = token.getContent().trim();
		this.typeArgs = token.getChildren().stream().map(ParameterizedType::new).collect(Collectors.toList());
	}

	String getTypeName() {
		return this.typeName;
	}

	public List<ParameterizedType> getTypeArgs() {
		return this.typeArgs;
	}

	static ParameterizedType parse(final String spec) {
		final Token token = Token.parse(spec, ParameterizedType.OPEN, ParameterizedType.SEP, ParameterizedType.CLOSE);
		return new ParameterizedType(token);
	}

	JClass createModelClass(final JCodeModel model) {
		if(this.typeArgs.isEmpty()) {
			return model.ref(this.typeName);
		} else {
			return model.ref(this.typeName).narrow(this.typeArgs.stream().map(a -> a.createModelClass(model)).collect(Collectors.toList()));
		}
	}

	String toJson() {
		return toJson(0);
	}

	private String toJson(final int level) {
		final StringBuilder sb = new StringBuilder();
		indent(sb, level);
		sb.append("{\n");
		indent(sb, level + 1);
		sb.append("name: ");
		sb.append(this.typeName);
		sb.append(",\n");
		if(!this.typeArgs.isEmpty()) {
			indent(sb, level + 1);
			sb.append("typeArgs: {\n");
			boolean first = true;
			for (final ParameterizedType typeArg : this.typeArgs) {
				if (first) {
					first = false;
				} else {
					indent(sb, level + 1);
					sb.append(",\n");
				}
				sb.append(typeArg.toJson(level + 2));
			}
			indent(sb, level + 1);
			sb.append("}\n");
		}
		indent(sb, level);
		sb.append("}\n");
		return sb.toString();
	}

	private static void indent(final StringBuilder sb, final int level) {
		for(int i = 0; i < level; i++) {
			sb.append("\t");
		}
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
