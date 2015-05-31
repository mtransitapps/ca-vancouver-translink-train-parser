package org.mtransit.parser.ca_vancouver_translink_train;

import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources.aspx
// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources/GTFS-Data.aspx
// http://mapexport.translink.bc.ca/current/google_transit.zip
public class VancouverTransLinkTrainAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-vancouver-translink-train-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new VancouverTransLinkTrainAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("Generating TransLink train data...\n");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this);
		super.start(args);
		System.out.printf("Generating TransLink train data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	private static final String INCLUDE_AGENCY_ID = "SKYT"; // SkyTrain only

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!INCLUDE_AGENCY_ID.equals(gRoute.agency_id)) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_TRAIN;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.route_short_name); // use route short name as route ID
	}

	private static final String RSN_CANADA_LINE = "980";
	private static final long RID_CANADA_LINE = 980l;
	private static final String CANADA_LINE_SHORT_NAME = "CAN";
	private static final String CANADA_LINE_LONG_NAME = "Canada Line";
	private static final String CANADA_LINE_COLOR = "0098C9"; // (from PDF)
	//
	private static final String RSN_MILLENNIUM_LINE = "996";
	private static final long RID_MILLENNIUM_LINE = 996l;
	private static final String MILLENNIUM_LINE_SHORT_NAME = "MIL";
	private static final String MILLENNIUM_LINE_LONG_NAME = "Millenium Line";
	private static final String MILLENNIUM_LINE_COLOR = "FDD005"; // (from PDF)
	//
	private static final String RSN_EXPO_LINE = "999";
	private static final long RID_EXPO_LINE = 999l;
	private static final String EXPO_LINE_SHORT_NAME = "EXP";
	private static final String EXPO_LINE_LONG_NAME = "Expo Line";
	private static final String EXPO_LINE_COLOR = "1D59AF"; // (from PDF)

	@Override
	public String getRouteShortName(GRoute gRoute) {
		if (RSN_CANADA_LINE.equals(gRoute.route_short_name)) {
			return CANADA_LINE_SHORT_NAME;
		} else if (RSN_MILLENNIUM_LINE.equals(gRoute.route_short_name)) {
			return MILLENNIUM_LINE_SHORT_NAME;
		} else if (RSN_EXPO_LINE.equals(gRoute.route_short_name)) {
			return EXPO_LINE_SHORT_NAME;
		}
		System.out.println("Unexpected route short name " + gRoute);
		System.exit(-1);
		return null;
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (RSN_CANADA_LINE.equals(gRoute.route_short_name)) {
			return CANADA_LINE_LONG_NAME;
		} else if (RSN_MILLENNIUM_LINE.equals(gRoute.route_short_name)) {
			return MILLENNIUM_LINE_LONG_NAME;
		} else if (RSN_EXPO_LINE.equals(gRoute.route_short_name)) {
			return EXPO_LINE_LONG_NAME;
		}
		System.out.println("Unexpected route long name " + gRoute);
		System.exit(-1);
		return null;
	}

	private static final String AGENCY_COLOR_BLUE = "0761A5"; // BLUE (merge)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (RSN_CANADA_LINE.equals(gRoute.route_short_name)) {
			return CANADA_LINE_COLOR;
		} else if (RSN_MILLENNIUM_LINE.equals(gRoute.route_short_name)) {
			return MILLENNIUM_LINE_COLOR;
		} else if (RSN_EXPO_LINE.equals(gRoute.route_short_name)) {
			return EXPO_LINE_COLOR;
		}
		System.out.println("Unexpected route color " + gRoute);
		System.exit(-1);
		return null;
	}

	private static final String YVR_RICHMOND_BRIGHOUSE = "YVR / Richmond-Brighouse";
	private static final String VCC_CLARK = "VCC–Clark";
	private static final String KING_GEORGE = "King George";
	private static final String WATERFRONT = "Waterfront";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (mRoute.id == RID_CANADA_LINE) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(WATERFRONT, gTrip.direction_id);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignString(YVR_RICHMOND_BRIGHOUSE, gTrip.direction_id);
				return;
			}
		} else if (mRoute.id == RID_MILLENNIUM_LINE) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(VCC_CLARK, gTrip.direction_id);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignString(WATERFRONT, gTrip.direction_id);
				return;
			}
		} else if (mRoute.id == RID_EXPO_LINE) {
			if (gTrip.direction_id == 0) {
				mTrip.setHeadsignString(KING_GEORGE, gTrip.direction_id);
				return;
			} else if (gTrip.direction_id == 1) {
				mTrip.setHeadsignString(WATERFRONT, gTrip.direction_id);
				return;
			}
		}
		System.out.printf("Unexpected trip (unexpected route ID: %s): %s\n", mRoute.id, gTrip);
		System.exit(-1);
	}

	private static final Pattern STARTS_WITH_QUOTE = Pattern.compile("(^\")", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_QUOTE = Pattern.compile("(\"[;]?$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern SKYTRAIN_LINE_TO = Pattern.compile("((skytrain )?(\\- platform sign )?([\\w]* line )?to )", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_VIA = Pattern.compile("(via.*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STATION = Pattern.compile("((^|\\W){1}(station)(\\W|$){1})", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		tripHeadsign = STARTS_WITH_QUOTE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = ENDS_WITH_QUOTE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = SKYTRAIN_LINE_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STATION.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = ENDS_WITH_VIA.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = fixCase(tripHeadsign);
		return MSpec.cleanLabel(tripHeadsign);
	}

	private static final Pattern VCC = Pattern.compile("(vcc)", Pattern.CASE_INSENSITIVE);
	private static final String VCC_REPLACEMENT = "VCC";

	private static final Pattern YVR = Pattern.compile("(Yvr)", Pattern.CASE_INSENSITIVE);
	private static final String YVR_REPLACEMENT = "YVR";

	private String fixCase(String string) {
		string = VCC.matcher(string).replaceAll(VCC_REPLACEMENT);
		string = YVR.matcher(string).replaceAll(YVR_REPLACEMENT);
		return string;
	}

	private static final Pattern STATION_LINE = Pattern.compile("( station( [\\w]* line)?)", Pattern.CASE_INSENSITIVE);

	private static final Pattern PLATFORM = Pattern.compile("( platform )", Pattern.CASE_INSENSITIVE);
	private static final String PLATFORM_REPLACEMENT = " P";

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = gStopName.toLowerCase(Locale.ENGLISH);
		gStopName = STATION_LINE.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = PLATFORM.matcher(gStopName).replaceAll(PLATFORM_REPLACEMENT);
		gStopName = fixCase(gStopName);
		gStopName = MSpec.cleanStreetTypes(gStopName);
		return MSpec.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		if (!StringUtils.isEmpty(gStop.stop_code) && Utils.isDigitsOnly(gStop.stop_code)) {
			return Integer.parseInt(gStop.stop_code); // using stop code as stop ID
		}
		return 1000000 + Integer.parseInt(gStop.stop_id);
	}
}
