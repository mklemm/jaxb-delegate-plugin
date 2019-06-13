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

import java.text.MessageFormat;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.kscs.util.plugins.xjc.base.PropertyDirectoryResourceBundle;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;
import com.sun.codemodel.JTypeVar;

public class TypeParser {
	private static final ResourceBundle RESOURCE_BUNDLE = PropertyDirectoryResourceBundle.getInstance(TypeParser.class);
	private final JCodeModel model;
	private final Map<TypeRef, JClass> envTypes;
	private final ParameterizedString parameterizedString;
	private final Map<TypeRefProperty, Function<JClass,String>> propertyGetters = Stream.of(
				k(TypeRefProperty.NAME, JClass::name),
				k(TypeRefProperty.BINARY_NAME, JType::binaryName),
				k(TypeRefProperty.PACKAGE, c -> c._package().name()),
			k(TypeRefProperty.FULL_NAME, JType::fullName),
			k(null, c -> c.outer() == null ? c.fullName() : c.outer().name() + "." + c.name()),
			k(TypeRefProperty.SIMPLE_NAME, c -> c.outer() == null ? c.fullName() : c.outer().name() + "." + c.name()),
			k(TypeRefProperty.TYPE_PARAMS, c -> Stream.of(c.typeParams()).map(JTypeVar::fullName).reduce((a, b) -> a+", "+b).orElse(""))
		).collect(Collectors.toMap(Kv::getKey, Kv::getGetter));

	public TypeParser(final JCodeModel model, final Map<TypeRef, JClass> envTypes) {
		this.model = model;
		this.envTypes = envTypes;
		this.parameterizedString = new ParameterizedString(this::replaceParam);
	}

	public JType parse(final String typeSpec) {
		if(typeSpec == null || typeSpec.isEmpty()) return JType.parse(this.model, "void");
		final String resolvedTypeSpec = this.parameterizedString.applyParameters(typeSpec);
		try {
			return JType.parse(this.model, resolvedTypeSpec);
		} catch (final IllegalArgumentException e) {
			final ParameterizedType p = ParameterizedType.parse(resolvedTypeSpec);
			return p.createModelClass(this.model);
		}
	}

	private String replaceParam (final String paramSpec) {
		try {
			final String[] paramParts = paramSpec.split("\\.");
			final TypeRef typeRef = TypeRef.fromValue(paramParts[0]);
			final TypeRefProperty propertyName = paramParts.length > 1 ? TypeRefProperty.fromValue(paramParts[1]) : TypeRefProperty.SIMPLE_NAME;
			final JClass envType = this.envTypes.get(typeRef);
			return envType == null ? "" : this.propertyGetters.get(propertyName).apply(envType);
		} catch(final IllegalArgumentException e) {
			throw new IllegalArgumentException(MessageFormat.format(TypeParser.RESOURCE_BUNDLE.getString("error.invalidExpansion"), e.getMessage()));
		}
	}

	private static Kv k(final TypeRefProperty key, final Function<JClass,String> getter) {
		return new Kv(key, getter);
	}

	private static class Kv {
		private final TypeRefProperty key;
		private final Function<JClass, String> getter;

		private Kv(final TypeRefProperty key, final Function<JClass, String> getter) {
			this.key = key;
			this.getter = getter;
		}

		public Function<JClass, String> getGetter() {
			return this.getter;
		}

		public TypeRefProperty getKey() {
			return this.key;
		}
	}
}
