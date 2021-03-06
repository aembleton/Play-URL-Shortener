package views.tags.urlShortener;

import groovy.lang.Closure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import play.cache.Cache;
import play.exceptions.TagInternalException;
import play.exceptions.TemplateExecutionException;
import play.templates.FastTags;
import play.templates.GroovyTemplate.ExecutableTemplate;

/**
 * URL Shortener tag. This accepts a URL and prints out a shortened version. Currently this only uses the URL Shortener provided by http://is.gd. is.gd was chosen as it provides shorter URLs than
 * other services and has a simple API that doesn't require any registration.
 * 
 * Invoke this by calling shorten url from a view in the following way:
 * 
 * <pre>
 * #{shorten.url 'http://www.example.com'/}
 * </pre>
 * 
 * @author Arthur Embleton <arthur@blerg.net>
 * 
 */
@FastTags.Namespace("shorten")
public class UrlShortener extends FastTags {

	private static final String IS_GD = "http://is.gd/create.php?format=simple&url=";
	private static final String CACHE_PREFIX = "url-shortener_";

	/**
	 * Tag end point. This is called when view contains a call to shortener.url such as
	 * 
	 * <pre>
	 * #{shorten.url 'http://www.example.com'/}
	 * </pre>
	 */
	public static void _url(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {

		if (!args.containsKey("arg") || args.get("arg") == null) {
			throw new TemplateExecutionException(template.template, fromLine, "You must provide a URL", new TagInternalException("You must provide a URL"));
		}

		String longUrl = ((String) args.get("arg")).trim();

		// look in the cache
		String shortUrl = (String) Cache.get(CACHE_PREFIX+longUrl);

		if (shortUrl != null) {
			// found in the cache so use this
			out.println(shortUrl);
			return;
		}

		String requestURL = IS_GD + longUrl;

		try {
			shortUrl = readUrl(requestURL);

			if (!shortUrl.startsWith("http")) {
				// this is an invalid address, throw an exception
				String errorMsg = longUrl + " could not be shortened";
				throw new TemplateExecutionException(template.template, fromLine, errorMsg, new TagInternalException(errorMsg));
			}

			Cache.add(CACHE_PREFIX+longUrl, shortUrl);
			out.println(shortUrl);
			
		} catch (IOException e) {
			String errorMsg = longUrl + " could not be shortened";
			throw new TemplateExecutionException(template.template, fromLine, errorMsg, new TagInternalException(errorMsg));
		}

	}

	/**
	 * Reads a URL and returns the first line that is returned in the page.
	 * 
	 * @param urlString
	 *            The URL to read from
	 * @return The first line returned when calling the URL
	 * @throws IOException
	 *             Thrown if the URL is invalid or stream couldn't be opened to the URL
	 */
	public static String readUrl(String urlString) throws IOException {

		StringBuffer result = new StringBuffer();

		URL url = new URL(urlString);
		BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
		result.append(in.readLine());
		in.close();

		return result.toString();
	}
}

