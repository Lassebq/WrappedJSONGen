package lbq.jsongen;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class JSONConstants {

	public static final Instant PAULSCODE_TIME = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2009-12-21T22:00:00+00:00"));

	static Instant skin1Start = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2009-06-12T19:14:44+00:00"));
	static Instant skin2Start = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2009-07-11T13:36:02+00:00"));
	static Instant skin3Start = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2011-10-06T01:00:00+00:00"));
	
	public static String getSkin(Instant time) {
		if(time.compareTo(skin3Start) > 0) {
			return "pre-1.8";
		}
		if(time.compareTo(skin2Start) > 0) {
			return "pre-b1.9-pre4";
		}
		if(time.compareTo(skin1Start) > 0) {
			return "classic";
		}
		return null;
	}

	static Instant time1Start = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2009-06-29T00:00:00+00:00"));
	static Instant time2Start = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2009-12-23T13:00:00+00:00"));
	static Instant time3Start = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2010-10-29T21:00:00+00:00"));
	static Instant time4Start = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse("2011-11-11T01:00:00+00:00"));
	
	public static int getPort(Instant time) {
		if(time.compareTo(time4Start) > 0) {
			return 11707;
		}
		if(time.compareTo(time3Start) > 0) {
			return 11705;
		}
		if(time.compareTo(time2Start) > 0) {
			return 11702;
		}
		if(time.compareTo(time1Start) > 0) {
			return 11701;
		}
		return -1;
	}

}
