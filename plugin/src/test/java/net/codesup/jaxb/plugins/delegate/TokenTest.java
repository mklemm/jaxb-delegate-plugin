package net.codesup.jaxb.plugins.delegate;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Mirko Klemm 2017-08-29
 */
public class TokenTest {

	@Test
	public void testParse() throws Exception {
		final Token p = Token.parse("java.util.Map<java.util.Optional<java.util.String>,Java.Util.Optional<Pair<Integer,String>>>", '<', ',','>');
		System.out.println(p.toString());
		Assert.assertEquals("java.util.Map<java.util.Optional<java.util.String>,Java.Util.Optional<Pair<Integer,String>>>", p.toString());
	}

}
