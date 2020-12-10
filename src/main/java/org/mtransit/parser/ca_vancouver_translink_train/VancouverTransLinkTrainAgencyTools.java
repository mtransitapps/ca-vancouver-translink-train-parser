package org.mtransit.parser.ca_vancouver_translink_train;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-vancouver-translink-train-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new VancouverTransLinkTrainAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating TransLink train data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating TransLink train data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
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
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		//noinspection RedundantIfStatement
		if (!isRoute(gRoute, INCLUDE_RSN)) {
			return true; // exclude
		}
		return false; // keep
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_TRAIN;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		// TODO export original route ID
		return super.getRouteId(gRoute); // useful to match with GTFS real-time
	}

	private static final String CANADA_LINE_SHORT_NAME = "CAN";
	private static final String CANADA_LINE_LONG_NAME = "Canada Line";
	private static final String CANADA_LINE_COLOR = "0098C9"; // (from PDF)
	//
	private static final String MILLENNIUM_LINE_SHORT_NAME = "MIL";
	private static final String MILLENNIUM_LINE_LONG_NAME = "Millenium Line";
	private static final String MILLENNIUM_LINE_COLOR = "FDD005"; // (from PDF)
	//
	private static final String EXPO_LINE_SHORT_NAME = "EXP";
	private static final String EXPO_LINE_LONG_NAME = "Expo Line";
	private static final String EXPO_LINE_COLOR = "1D59AF"; // (from PDF)

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		if (gRoute.getRouteShortName().isEmpty()) {
			// REAL-TIME API IS FOR BUS ONLY
			if (isRoute(gRoute, RSN_CANADA_LINE)) {
				return CANADA_LINE_SHORT_NAME;
			} else if (isRoute(gRoute, RSN_MILLENNIUM_LINE)) {
				return MILLENNIUM_LINE_SHORT_NAME;
			} else if (isRoute(gRoute, RSN_EXPO_LINE)) {
				return EXPO_LINE_SHORT_NAME;
			}
			throw new MTLog.Fatal("Unexpected route short name %s!", gRoute);
		}
		return super.getRouteShortName(gRoute);
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteLongName())) {
			if (isRoute(gRoute, RSN_CANADA_LINE)) {
				return CANADA_LINE_LONG_NAME;
			} else if (isRoute(gRoute, RSN_MILLENNIUM_LINE)) {
				return MILLENNIUM_LINE_LONG_NAME;
			} else if (isRoute(gRoute, RSN_EXPO_LINE)) {
				return EXPO_LINE_LONG_NAME;
			}
			throw new MTLog.Fatal("Unexpected route long name " + gRoute);
		}
		return super.getRouteLongName(gRoute);
	}

	private static final String AGENCY_COLOR_BLUE = "0761A5"; // BLUE (merge)

	private static final String AGENCY_COLOR = AGENCY_COLOR_BLUE;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			if (isRoute(gRoute, RSN_CANADA_LINE)) {
				return CANADA_LINE_COLOR;
			} else if (isRoute(gRoute, RSN_MILLENNIUM_LINE)) {
				return MILLENNIUM_LINE_COLOR;
			} else if (isRoute(gRoute, RSN_EXPO_LINE)) {
				return EXPO_LINE_COLOR;
			}
			throw new MTLog.Fatal("Unexpected route color " + gRoute);
		}
		return super.getRouteColor(gRoute);
	}

	private static final String PRODUCTION_WAY_UNIVERSITY_SHORT = "Prod Way–U";

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		throw new MTLog.Fatal("%s: Using direction finder to merge %s and %s!", mTrip.getRouteId(), mTrip, mTripToMerge);
	}

	private static final Pattern STARTS_WITH_QUOTE = Pattern.compile("(^\")", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_QUOTE = Pattern.compile("(\"[;]?$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern SKYTRAIN_LINE_TO = Pattern.compile("((skytrain )?(- platform sign )?([\\w]* line )?to )", Pattern.CASE_INSENSITIVE);

	private static final Pattern STATION = Pattern.compile("((^|\\W)(station)(\\W|$))", Pattern.CASE_INSENSITIVE);

	private static final Pattern PRODUCTION_WAY_UNIVERSITY_ = Pattern.compile("((^|\\W)(production way-university)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String PRODUCTION_WAY_UNIVERSITY_REPLACEMENT = "$2" + PRODUCTION_WAY_UNIVERSITY_SHORT + "$4";

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = STARTS_WITH_QUOTE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = ENDS_WITH_QUOTE.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = SKYTRAIN_LINE_TO.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = STATION.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY);
		tripHeadsign = CleanUtils.removeVia(tripHeadsign);
		tripHeadsign = PRODUCTION_WAY_UNIVERSITY_.matcher(tripHeadsign).replaceAll(PRODUCTION_WAY_UNIVERSITY_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_DASHES.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_DASHES_REPLACEMENT);
		tripHeadsign = CleanUtils.fixMcXCase(tripHeadsign);
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign, "VCC", "YVR");
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	private static final Pattern STATION_LINE = Pattern.compile("( station( [\\w]* line)?)", Pattern.CASE_INSENSITIVE);

	private static final Pattern PLATFORM = Pattern.compile("( platform )", Pattern.CASE_INSENSITIVE);
	private static final String PLATFORM_REPLACEMENT = " P";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = STATION_LINE.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = PLATFORM.matcher(gStopName).replaceAll(PLATFORM_REPLACEMENT);
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, "VCC", "YVR");
		gStopName = CleanUtils.fixMcXCase(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		return super.getStopId(gStop); // using stop ID as stop code (useful to match with GTFS real-time)
	}
}
