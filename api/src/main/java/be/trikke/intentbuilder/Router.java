package be.trikke.intentbuilder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class Router {

	private final Map<String, Route> predefinedRoutes = new HashMap<>();

	public void call(Context context, String url) {
		this.call(context, url, null, 0);
	}

	public void call(Context context, String url, int flags) {
		this.call(context, url, null, flags);
	}

	public void call(Context context, String url, Bundle extras, int flags) {
		Route route = determineRoute(url);

		if (route.getTargetClass() != null) {
			if (context == null) throw new NullPointerException("In order to start activities, you need to provide the router with context.");
			Intent intent = route.getIntent(context);
			if (extras != null) intent.putExtras(extras);
			if (flags > 0) intent.addFlags(flags);
			context.startActivity(intent);
		}
	}

	/**
	 * Routes a URL to a callback function code block and to a Android {@link Activity} to be opened when the URL is called.
	 *
	 * @param routeUrl The URL to route to the callback function code block and Android {@link Activity}.
	 * @param targetClass The Android {@link Activity} to open when the URL is called.
	 */
	public void route(String routeUrl, Class<? extends Activity> targetClass) {
		String cleanedRouteUrl = cleanUrl(routeUrl);
		Route route = new Route(cleanedRouteUrl);
		if (targetClass != null) route.setTargetClass(targetClass);
		predefinedRoutes.put(cleanedRouteUrl, route);
	}

	/**
	 * Takes a URL, determines what route it falls under and creates a new {@link Route} instance using that information.
	 *
	 * @param url The URL with payload data we want to check the predefined routes for.
	 * @return New {@link Route} object with parameters bundle.
	 */
	private Route determineRoute(String url) {
		URI uri = URI.create(url);
		String cleanUrl = cleanUrl(url);
		for (Map.Entry<String, Route> routeEntry : predefinedRoutes.entrySet()) {
			if (!routeEntry.getKey().equalsIgnoreCase(cleanUrl)) continue;
			Bundle routeParams = createParamBundle(uri.getQuery());
			if (routeParams == null) continue;
			routeParams.putString("route", url);

			Route route = new Route(routeEntry.getKey());
			if (routeEntry.getValue().getTargetClass() != null) route.setTargetClass(routeEntry.getValue().getTargetClass());
			route.setParameters(routeParams);
			return route;
		}
		throw new UnsupportedOperationException("Route not found for the url " + url);
	}

	public Intent getIntent(Context context, String url) {
		return getIntent(context, determineRoute(url));
	}

	private Intent getIntent(Context context, Route route) {
		if (route.getTargetClass() != null) {
			Intent intent = new Intent();
			intent.putExtras(route.getParameters());
			intent.setClass(context, route.getTargetClass());
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			return intent;
		}

		return null;
	}

	/**
	 * Checks each of the segments in the URL, compares them and puts the payload in a {@link Bundle} with the :variable.
	 *
	 * @return {@link Bundle} with the payloads assigned to their route variables.
	 */
	private Bundle createParamBundle(String query) {
		Bundle routeParams = new Bundle();
		if (query != null) {
			String[] pairs = query.split("&");
			for (String pair : pairs) {
				int idx = pair.indexOf("=");
				String name;
				String value;
				try {
					value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
					name = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					value = pair.substring(idx + 1);
					name = pair.substring(0, idx);
				}
				try {
					int integer = Integer.parseInt(value);
					routeParams.putInt(name, integer);
				} catch (NumberFormatException nfe) {
					routeParams.putString(name, value);
				}
			}
		}
		return routeParams;
	}

	/**
	 * remove the query string
	 */
	private String cleanUrl(String routeUrl) {
		if (routeUrl.indexOf("?") > 0) routeUrl = routeUrl.substring(0, routeUrl.indexOf("?"));
		return routeUrl;
	}
}
