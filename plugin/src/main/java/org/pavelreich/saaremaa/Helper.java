package org.pavelreich.saaremaa;

import java.util.List;
import java.util.stream.Collectors;

public class Helper {

	public static String convertListToString(List<String> ls) {
		if (ls.size() < 5) {
			return ls.stream().collect(Collectors.joining(", "));
		} else {
			String head = ls.subList(0, 3).stream().collect(Collectors.joining(", "));
			return head + ", " + (ls.size() - 4 + " more, ") + ls.get(ls.size() - 1);
		}
	}
}
