package net.codesup.jaxb.plugins.delegate.test;

/**
 * @author Mirko Klemm 2016-11-02
 */
public class TestDelegate {
	private final TestDelegeeType delegee;

	public TestDelegate(final TestDelegeeType delegee) {
		this.delegee = delegee;
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof TestDelegeeType && ((TestDelegeeType)o).getUserId().equals(this.delegee.getUserId());
	}

	@Override
	public int hashCode() {
		return this.delegee != null ? this.delegee.getUserId().hashCode() : 0;
	}
}
