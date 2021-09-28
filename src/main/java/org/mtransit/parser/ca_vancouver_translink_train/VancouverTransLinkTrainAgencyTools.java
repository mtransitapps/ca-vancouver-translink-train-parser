package org.mtransit.parser.ca_vancouver_translink_train;

import static org.mtransit.commons.StringUtils.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.mt.data.MAgency;

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

	public static void main(@NotNull String[] args) {
		new VancouverTransLinkTrainAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_EN;
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

	@NotNull
	@Override
	public Integer getOriginalAgencyRouteType() {
		return MAgency.ROUTE_TYPE_SUBWAY; // classified as subway instead of train
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_TRAIN;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return false; // useful to match with GTFS real-time
	}

	@NotNull
	@Override
	public String provideMissingRouteShortName(@NotNull GRoute gRoute) {
		final String routeLongName = gRoute.getRouteLongNameOrDefault().toLowerCase(getFirstLanguageNN());
		switch (routeLongName) {
		case "canada line":
			return "CAN";
		case "millennium line":
			return "MIL";
		case "expo line":
			return "EXP";
		}
		return super.provideMissingRouteShortName(gRoute);
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	private static final Pattern SKY_TRAIN_ = CleanUtils.cleanWords("skytrain");

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, routeLongName, getIgnoredWords());
		routeLongName = SKY_TRAIN_.matcher(routeLongName).replaceAll(EMPTY);
		return CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
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
	public String provideMissingRouteColor(@NotNull GRoute gRoute) {
		final String routeLongName = gRoute.getRouteLongNameOrDefault().toLowerCase(getFirstLanguageNN());
		switch (routeLongName) {
		case "canada line":
			return "0098C9"; // (from PDF)
		case "millennium line":
			return "FDD005"; // (from PDF)
		case "expo line":
			return "1D59AF"; // (from PDF)
		}
		return super.provideMissingRouteColor(gRoute);
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
