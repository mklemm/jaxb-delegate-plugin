package net.codesup.jaxb.plugins.delegate.test;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Mirko Klemm 2016-11-02
 */
public class TestDelegate<T extends Comparable<T>> {
	private final TestDelegeeType delegee;

	public TestDelegate(final TestDelegeeType delegee) {
		this.delegee = delegee;
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof TestDelegeeType && ((TestDelegeeType)o).getUserId().equals(this.delegee.getUserId());
	}


	public <A extends Serializable>void voidMethod2(A obj) {

	}

	public void voidMethod1(Object obj) {

	}

	public Map<Optional<String>, List<String>> genericTypeMethod(final Optional<ResultSet> obj) {
		return null;
	}

	@Override
	public int hashCode() {
		return this.delegee != null ? this.delegee.getUserId().hashCode() : 0;
	}
}
