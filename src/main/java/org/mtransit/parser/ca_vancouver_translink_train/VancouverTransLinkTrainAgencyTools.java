package org.mtransit.parser.ca_vancouver_translink_train;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources.aspx
// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources/GTFS-Data.aspx
// http://mapexport.translink.bc.ca/current/google_transit.zip
// http://ns.translink.ca/gtfs/notifications.zip
// http://ns.translink.ca/gtfs/google_transit.zip
// http://gtfs.translink.ca/static/latest
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
		MTLog.log("Generating TransLink train data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating TransLink train data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
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

	private static final List<String> RSN_CANADA_LINE = Arrays.asList(//
			"980", "Canada Line", "CANADA LINE SKYTRAIN" //
	);
	private static final List<String> RSN_MILLENNIUM_LINE = Arrays.asList(//
			"991", "Millennium Line", "MILLENNIUM SKYTRAIN" //
	);
	private static final List<String> RSN_EXPO_LINE = Arrays.asList(//
			"992", "Expo Line", "EXPO SKYTRAIN" //
	);

	private boolean isRoute(GRoute gRoute, List<String> rsns) {
		return rsns.contains(gRoute.getRouteShortName()) //
				|| rsns.contains(gRoute.getRouteLongName());
	}

	private static final List<String> INCLUDE_RSN;

	static {
		List<String> list = new ArrayList<>();
		list.addAll(RSN_CANADA_LINE);
		list.addAll(RSN_MILLENNIUM_LINE);
		list.addAll(RSN_EXPO_LINE);
		INCLUDE_RSN = list;
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!isRoute(gRoute, INCLUDE_RSN)) {
			return true; // exclude
		}
		return false; // keep
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
		// TODO export original route ID
		return super.getRouteId(gRoute); // useful to match with GTFS real-time
	}

	@SuppressWarnings("unused")
	private static final long RID_CANADA_LINE = 980L;
	private static final String CANADA_LINE_SHORT_NAME = "CAN";
	private static final String CANADA_LINE_LONG_NAME = "Canada Line";
	private static final String CANADA_LINE_COLOR = "0098C9"; // (from PDF)
	//
	@SuppressWarnings("unused")
	private static final long RID_MILLENNIUM_LINE = 991L;
	private static final String MILLENNIUM_LINE_SHORT_NAME = "MIL";
	private static final String MILLENNIUM_LINE_LONG_NAME = "Millenium Line";
	private static final String MILLENNIUM_LINE_COLOR = "FDD005"; // (from PDF)
	//
	@SuppressWarnings("unused")
	private static final long RID_EXPO_LINE = 992L;
	private static final String EXPO_LINE_SHORT_NAME = "EXP";
	private static final String EXPO_LINE_LONG_NAME = "Expo Line";
	private static final String EXPO_LINE_COLOR = "1D59AF"; // (from PDF)

	@Override
	public String getRouteShortName(GRoute gRoute) {
		// REAL-TIME API IS FOR BUS ONLY
		if (isRoute(gRoute, RSN_CANADA_LINE)) {
			return CANADA_LINE_SHORT_NAME;
		} else if (isRoute(gRoute, RSN_MILLENNIUM_LINE)) {
			return MILLENNIUM_LINE_SHORT_NAME;
		} else if (isRoute(gRoute, RSN_EXPO_LINE)) {
			return EXPO_LINE_SHORT_NAME;
		}
		MTLog.logFatal("Unexpected route short name %s!", gRoute);
		return null;
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		if (isRoute(gRoute, RSN_CANADA_LINE)) {
			return CANADA_LINE_LONG_NAME;
		} else if (isRoute(gRoute, RSN_MILLENNIUM_LINE)) {
			return MILLENNIUM_LINE_LONG_NAME;
		} else if (isRoute(gRoute, RSN_EXPO_LINE)) {
			return EXPO_LINE_LONG_NAME;
		}
		MTLog.logFatal("Unexpected route long name " + gRoute);
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
		if (isRoute(gRoute, RSN_CANADA_LINE)) {
			return CANADA_LINE_COLOR;
		} else if (isRoute(gRoute, RSN_MILLENNIUM_LINE)) {
			return MILLENNIUM_LINE_COLOR;
		} else if (isRoute(gRoute, RSN_EXPO_LINE)) {
			return EXPO_LINE_COLOR;
		}
		MTLog.logFatal("Unexpected route color " + gRoute);
		return null;
	}

	private static final String YVR_RICHMOND_BRIGHOUSE = "YVR / Richmond-Brighouse";
	private static final String VCC_CLARK = "VCC-Clark";
	private static final String KING_GEORGE = "King George";
	private static final String PRODUCTION_WAY_UNIVERSITY_SHORT = "Prod Way–U";
	private static final String KING_GEORGE_PRODUCTION_WAY_UNIVERSITY = KING_GEORGE + " / " + PRODUCTION_WAY_UNIVERSITY_SHORT;
	private static final String WATERFRONT = "Waterfront";
	private static final String LAFARGE_LAKE_DOUGLAS = "Lafarge Lake-Douglas";

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	private static List<String> CANADA_LINE_WATERFRONT = Arrays.asList("Waterfront", "Bridgeport");
	private static List<String> CANADA_LINE_YVR_RICHMOND_BRIGHOUSE = Arrays.asList("YVR", "YVR-Airport", "Richmond-Brighouse");

	private static List<String> MILLENNIUM_LINE_VCC_CLARK = Arrays.asList("VCC–Clark", "VCC-Clark");

	private static List<String> MILLENNIUM_LINE_LAFARGE_LAKE_DOUGLAS = Arrays.asList("Lougheed", "Lougheed Town Centre", LAFARGE_LAKE_DOUGLAS);

	private static List<String> EXPO_LINE_KING_GEORGE_PRODUCTION_WAY_UNIVERSITY = Arrays.asList("King George", "Production Way-University",
			PRODUCTION_WAY_UNIVERSITY_SHORT, "Edmonds", "Lougheed Town Centre");
	private static List<String> EXPO_LINE_WATERFRONT = Collections.singletonList("Waterfront");

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		if (mTrip.getRouteId() == 13686L) { // RID_CANADA_LINE
			if (mTrip.getHeadsignId() == 0) {
				if (CANADA_LINE_WATERFRONT.contains(mTrip.getHeadsignValue()) || CANADA_LINE_WATERFRONT.contains(mTripToMerge.getHeadsignValue())) {
					mTrip.setHeadsignString(WATERFRONT, mTrip.getHeadsignId());
					return true;
				}
			} else if (mTrip.getHeadsignId() == 1) {
				if (CANADA_LINE_YVR_RICHMOND_BRIGHOUSE.contains(mTrip.getHeadsignValue())
						|| CANADA_LINE_YVR_RICHMOND_BRIGHOUSE.contains(mTripToMerge.getHeadsignValue())) {
					mTrip.setHeadsignString(YVR_RICHMOND_BRIGHOUSE, mTrip.getHeadsignId());
					return true;
				}
			}
			MTLog.log("%s: Unexpected trips to merge: %s>%s & %s>%s!", mTrip.getRouteId(), //
					mTrip.getHeadsignId(), mTrip.getHeadsignValue(), //
					mTripToMerge.getHeadsignId(), mTripToMerge.getHeadsignValue());
			MTLog.logFatal("%s: Unexpected trips to merge: %s and %s!", mTrip.getRouteId(), mTrip, mTripToMerge);
			return false;
		} else if (mTrip.getRouteId() == 30052L) { // RID_MILLENNIUM_LINE
			if (mTrip.getHeadsignId() == 0) {
				if (MILLENNIUM_LINE_LAFARGE_LAKE_DOUGLAS.contains(mTrip.getHeadsignValue())
						|| MILLENNIUM_LINE_LAFARGE_LAKE_DOUGLAS.contains(mTripToMerge.getHeadsignValue())) {
					mTrip.setHeadsignString(LAFARGE_LAKE_DOUGLAS, mTrip.getHeadsignId());
					return true;
				}
			} else if (mTrip.getHeadsignId() == 1) {
				if (MILLENNIUM_LINE_VCC_CLARK.contains(mTrip.getHeadsignValue()) || MILLENNIUM_LINE_VCC_CLARK.contains(mTripToMerge.getHeadsignValue())) {
					mTrip.setHeadsignString(VCC_CLARK, mTrip.getHeadsignId());
					return true;
				}
			}
			MTLog.log("%s: Unexpected trips to merge: %s>%s & %s>%s!", mTrip.getRouteId(), //
					mTrip.getHeadsignId(), mTrip.getHeadsignValue(), //
					mTripToMerge.getHeadsignId(), mTripToMerge.getHeadsignValue());
			MTLog.logFatal("%s: Unexpected trips to merge: %s and %s!", mTrip.getRouteId(), mTrip, mTripToMerge);
			return false;
		} else if (mTrip.getRouteId() == 30053L) { // RID_EXPO_LINE
			if (mTrip.getHeadsignId() == 0) {
				if (EXPO_LINE_KING_GEORGE_PRODUCTION_WAY_UNIVERSITY.contains(mTrip.getHeadsignValue())
						|| EXPO_LINE_KING_GEORGE_PRODUCTION_WAY_UNIVERSITY.contains(mTripToMerge.getHeadsignValue())) {
					mTrip.setHeadsignString(KING_GEORGE_PRODUCTION_WAY_UNIVERSITY, mTrip.getHeadsignId());
					return true;
				}
			} else if (mTrip.getHeadsignId() == 1) {
				if (EXPO_LINE_WATERFRONT.contains(mTrip.getHeadsignValue()) || EXPO_LINE_WATERFRONT.contains(mTripToMerge.getHeadsignValue())) {
					mTrip.setHeadsignString(WATERFRONT, mTrip.getHeadsignId());
					return true;
				}
			}
			MTLog.log("%s: Unexpected trips to merge: %s>%s & %s>%s!", mTrip.getRouteId(), //
					mTrip.getHeadsignId(), mTrip.getHeadsignValue(), //
					mTripToMerge.getHeadsignId(), mTripToMerge.getHeadsignValue());
			MTLog.logFatal("%s: Unexpected trips to merge: %s and %s!", mTrip.getRouteId(), mTrip, mTripToMerge);
			return false;
		}
		MTLog.logFatal("%s: Unexpected trips to merge: %s and %s!", mTrip.getRouteId(), mTrip, mTripToMerge);
		return false;
	}

	private static final Pattern STARTS_WITH_QUOTE = Pattern.compile("(^\")", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_QUOTE = Pattern.compile("(\"[;]?$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern SKYTRAIN_LINE_TO = Pattern.compile("((skytrain )?(- platform sign )?([\\w]* line )?to )", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_VIA = Pattern.compile("(via.*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STATION = Pattern.compile("((^|\\W)(station)(\\W|$))", Pattern.CASE_INSENSITIVE);

	private static final Pattern PRODUCTION_WAY_UNIVERSITY_ = Pattern.compile("((^|\\W)(production way-university)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String PRODUCTION_WAY_UNIVERSITY_REPLACEMENT = "$2" + PRODUCTION_WAY_UNIVERSITY_SHORT + "$4";

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = tripHeadsign.toLowerCase(Locale.ENGLISH);
		tripHeadsign = STARTS_WITH_QUOTE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = ENDS_WITH_QUOTE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = SKYTRAIN_LINE_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STATION.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = ENDS_WITH_VIA.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = PRODUCTION_WAY_UNIVERSITY_.matcher(tripHeadsign).replaceAll(PRODUCTION_WAY_UNIVERSITY_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_DASHES.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_DASHES_REPLACEMENT);
		tripHeadsign = fixCase(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
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
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(GStop gStop) {
		return super.getStopId(gStop); // using stop ID as stop code (useful to match with GTFS real-time)
	}
}
