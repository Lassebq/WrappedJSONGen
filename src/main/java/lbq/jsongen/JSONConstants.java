package lbq.jsongen;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class JSONConstants {

	public static final Instant PAULSCODE_TIME = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2009-12-21T22:00:00+00:00"));
	public static final Instant ASSETINDEX_TIME = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2013-04-21T12:49:20+00:00"));
	public static final Instant LWJGL2_TIME = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2017-09-18T08:39:46+00:00"));

	static Instant skin1Start = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2009-06-14T07:53:24+00:00"));
	static Instant skin2Start = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2009-06-19T22:53:16+00:00"));
	static Instant skin3Start = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2009-07-11T13:36:02+00:00"));
	static Instant skin4Start = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2011-10-06T01:00:00+00:00"));
	static Instant skin5Start = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2014-01-16T14:45:12+00:00"));

	public static String getSkin(Instant time, String ver) {
		if (time.compareTo(skin5Start) >= 0 && !ver.startsWith("1.7.")) {
			return "default";
		}
		// TODO pre-1.9
		if (time.compareTo(skin4Start) >= 0) {
			return "pre-1.8";
		}
		if (time.compareTo(skin3Start) >= 0) {
			return "pre-b1.9-pre4";
		}
		if (time.compareTo(skin2Start) >= 0) {
			return "classic";
		}
		if (time.compareTo(skin1Start) >= 0) {
			return "pre-c0.0.19a";
		}
		return "default";
	}

}
