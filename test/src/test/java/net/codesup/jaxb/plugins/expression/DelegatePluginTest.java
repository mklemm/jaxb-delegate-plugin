package net.codesup.jaxb.plugins.expression;

import org.junit.Test;

import net.codesup.jaxb.plugins.delegate.test.TestDelegeeType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by klemm0 on 2016-04-29.
 */
public class DelegatePluginTest {
	@Test
	public void testDelegatePlugin() throws Exception {
		final TestDelegeeType t1 = new TestDelegeeType();
		t1.setAutoLogon(true);
		t1.setConfirmPrivilege(0);
		t1.setUserFirstName("Mike");
		t1.setUserLastName("Glotzkowski");
		t1.setUserId("mglotz");

		final TestDelegeeType t2 = new TestDelegeeType();
		t2.setAutoLogon(false);
		t2.setConfirmPrivilege(1);
		t2.setUserFirstName("Mike");
		t2.setUserLastName("Glotzkowski");
		t2.setUserId("mglotz");

		final TestDelegeeType t3 = new TestDelegeeType();
		t3.setAutoLogon(false);
		t3.setConfirmPrivilege(1);
		t3.setUserFirstName("James");
		t3.setUserLastName("Sullivan");
		t3.setUserId("jsulli");

		assertEquals(t1,t2);
		assertNotEquals(t1,t3);
	}
}
