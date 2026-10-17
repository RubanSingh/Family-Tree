package dna.familytree;

import android.content.Context;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a GEDCOM age at event (e.g. "35y 2m", "< 8y", "INFANT") and writes it in words (adapted from Family Gem).
 */
public final class AgeConverter {

    private static final Pattern YEARS = Pattern.compile("(\\d+)\\s*y", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTHS = Pattern.compile("(\\d+)\\s*m", Pattern.CASE_INSENSITIVE);
    private static final Pattern DAYS = Pattern.compile("(\\d+)\\s*d", Pattern.CASE_INSENSITIVE);

    private AgeConverter() {
    }

    /** The age in words, e.g. "More than 35 years 2 months"; the original text if it can't be read. */
    public static String writeAge(Context context, String gedcomAge) {
        if (gedcomAge == null) return "";
        String age = gedcomAge.trim();
        if (age.isEmpty()) return "";
        String upper = age.toUpperCase(Locale.US);
        StringBuilder builder = new StringBuilder();
        int years = 0, months = 0, days = 0;
        if (upper.contains("CHILD")) {
            builder.append(context.getString(R.string.less_than)).append(' ');
            years = 8;
        } else if (upper.contains("INFANT")) {
            builder.append(context.getString(R.string.less_than)).append(' ');
            years = 1;
        } else if (upper.contains("STILLBORN")) {
            return age;
        } else {
            if (age.startsWith("<")) builder.append(context.getString(R.string.less_than)).append(' ');
            else if (age.startsWith(">")) builder.append(context.getString(R.string.more_than)).append(' ');
            years = number(age, YEARS);
            months = number(age, MONTHS);
            days = number(age, DAYS);
        }
        if (years == 0 && months == 0 && days == 0) return age; // Not understood: shows it as written
        if (years > 0)
            builder.append(years).append(' ').append(context.getString(years == 1 ? R.string.year : R.string.years).toLowerCase()).append(' ');
        if (months > 0) builder.append(months).append(' ').append(context.getString(R.string.months)).append(' ');
        if (days > 0) builder.append(days).append(' ').append(context.getString(R.string.days));
        return builder.toString().trim();
    }

    private static int number(String age, Pattern pattern) {
        Matcher matcher = pattern.matcher(age);
        try {
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
