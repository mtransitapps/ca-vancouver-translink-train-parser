package org.mtransit.parser.ca_vancouver_translink_train;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources.aspx
// http://www.translink.ca/en/Schedules-and-Maps/Developer-Resources/GTFS-Data.aspx
// http://mapexport.translink.bc.ca/current/google_transit.zip
// http://ns.translink.ca/gtfs/notifications.zip
// http://ns.translink.ca/gtfs/google_transit.zip
// http://gtfs.translink.ca/static/latest
public class VancouverTransLinkTrainAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new VancouverTransLinkTrainAgencyTools().start(args);
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "TransLink";
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return true;
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

	private boolean isRoute(@NotNull GRoute gRoute, @NotNull List<String> rsns) {
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
		if (gRoute.getRouteShortName().isEmpty()
				|| CharUtils.isDigitsOnly(gRoute.getRouteShortName())) {
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
		return cleanRouteLongName(super.getRouteLongName(gRoute));
	}

	private static final Pattern SKY_TRAIN_ = CleanUtils.cleanWords("skytrain");

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, routeLongName, getIgnoredWords());
		routeLongName = SKY_TRAIN_.matcher(routeLongName).replaceAll(EMPTY);
		return CleanUtils.cleanLabel(routeLongName);
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

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern STARTS_WITH_QUOTE = Pattern.compile("(^\")", Pattern.CASE_INSENSITIVE);

	private static final Pattern ENDS_WITH_QUOTE = Pattern.compile("(\"[;]?$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern SKYTRAIN_LINE_TO = Pattern.compile("((skytrain )?(- platform sign )?([\\w]* line )?to )", Pattern.CASE_INSENSITIVE);

	private static final Pattern STATION = Pattern.compile("((^|\\W)(station)(\\W|$))", Pattern.CASE_INSENSITIVE);

	private static final String PRODUCTION_WAY_UNIVERSITY_SHORT = "Prod Way–U";
	private static final Pattern PRODUCTION_WAY_UNIVERSITY_ = Pattern.compile("((^|\\W)(production way-university)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String PRODUCTION_WAY_UNIVERSITY_REPLACEMENT = "$2" + PRODUCTION_WAY_UNIVERSITY_SHORT + "$4";

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign, getIgnoredWords());
		tripHeadsign = STARTS_WITH_QUOTE.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = ENDS_WITH_QUOTE.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = SKYTRAIN_LINE_TO.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = STATION.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.removeVia(tripHeadsign);
		tripHeadsign = PRODUCTION_WAY_UNIVERSITY_.matcher(tripHeadsign).replaceAll(PRODUCTION_WAY_UNIVERSITY_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_DASHES.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_DASHES_REPLACEMENT);
		tripHeadsign = CleanUtils.fixMcXCase(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@NotNull
	private String[] getIgnoredWords() {
		return new String[]{
				"VCC", "YVR",
		};
	}

	private static final Pattern STATION_LINE = Pattern.compile("( station( [\\w]* line)?)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, getIgnoredWords());
		gStopName = STATION_LINE.matcher(gStopName).replaceAll(EMPTY);
		gStopName = CleanUtils.fixMcXCase(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		return super.getStopId(gStop); // using stop ID as stop code (useful to match with GTFS real-time)
	}
}
