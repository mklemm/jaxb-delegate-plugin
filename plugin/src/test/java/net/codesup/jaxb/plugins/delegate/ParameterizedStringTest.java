package net.codesup.jaxb.plugins.delegate;

import org.junit.Test;

/**
 * @author Mirko Klemm 2019-06-12
 */
public class ParameterizedStringTest {
	@Test
	public void applyParameters() {
		final String s = "We have {var1.d} of {var2} items.";
		ParameterizedString ps = new ParameterizedString(ParameterizedStringTest::getParameterValue);
		System.out.println(ps.applyParameters(s));
	}

	public static String getParameterValue(final String paramName) {
		if(paramName.equals("var1.d")) {
			return "2";
		} else if(paramName.equals("var2")) {
			return "4";
		} else {
			return "";
		}
	}
}
