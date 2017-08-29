package net.codesup.jaxb.plugins.delegate;

import org.junit.Assert;
import org.junit.Test;

public class ParameterizedTypeTest {
	@Test
	public void testParseSimple() throws Exception {
		final ParameterizedType p = ParameterizedType.parse("Integer");
		Assert.assertEquals("Integer",p.getTypeName());
	}

	@Test
	public void testParseUnary() throws Exception {
		final ParameterizedType p = ParameterizedType.parse("java.util.Iterable<Java.Util.Optional<Integer>>");
		Assert.assertEquals("java.util.Iterable<Java.Util.Optional<Integer>>", p.toString());
	}

	@Test
	public void testParseComplex() throws Exception {
		final ParameterizedType p = ParameterizedType.parse("java.util.Map<java.util.Optional<java.util.String>,Java.Util.Optional< Pair<Integer, String> >>");
		Assert.assertEquals("java.util.Map<java.util.Optional<java.util.String>,Java.Util.Optional<Pair<Integer,String>>>", p.toString());
		System.out.println(p.toJson());
	}
}
