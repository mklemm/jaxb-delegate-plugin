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
	public void testParseComplex() throws Exception {
		final ParameterizedType p = ParameterizedType.parse("java.util.Iterable<Java.Util.Optional<Pair<Integer, Int>>>");
		Assert.assertEquals("java.util.Iterable<Java.Util.Optional<Pair<Integer, Int>>>", p.toString());
	}
}
