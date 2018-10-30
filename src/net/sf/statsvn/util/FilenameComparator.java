package net.sf.statsvn.util;

import java.io.Serializable;
import java.util.Comparator;

public class FilenameComparator implements Comparator, Serializable {
	private static final long serialVersionUID = 3456265631104179922L;

	public int compare(final Object arg0, final Object arg1) {
		if (arg0 == null || arg1 == null) {
			return 0;
		}

		final String s0 = arg0.toString().replace('/', '\t');
		final String s1 = arg1.toString().replace('/', '\t');

		return s0.compareTo(s1);
	}
}
