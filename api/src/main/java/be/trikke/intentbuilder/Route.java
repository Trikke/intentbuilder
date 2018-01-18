package be.trikke.intentbuilder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class Route {

	private String routeName;
	private Bundle parameters;
	private Class<? extends Activity> targetClass;

	/**
	 * Instantiates a new {@link Route} object and sets the route name, example; "/user/:userId/projects/:projectId".
	 *
	 * @param routeName The name of the {@link Route} object being instantiated.
	 */
	public Route(String routeName) {
		this.routeName = routeName;
	}

	/**
	 * Gets the name of the {@link Route} object instance.
	 *
	 * @return The name of the {@link Route} object instance.
	 */
	public String getRouteName() {
		return routeName;
	}

	/**
	 * Gets the parameter bundle for this {@link Route} object.
	 *
	 * @return The parameter bundle for this {@link Route} object.
	 */
	public Bundle getParameters() {
		return parameters;
	}

	/**
	 * Sets the parameters for this {@link Route} object, by breaking down the passed URL parameters and assigning them
	 * to the predefined route parameters, example; "/user/ryanw-se/projects/example" &amp; "/user/:userId/projects/:projectId
	 * would assign the parameter userId the value ryanw-se and the projectId to the value example.
	 *
	 * @param parameters The parameters to be set for this {@link Route} object.
	 */
	public void setParameters(Bundle parameters) {
		this.parameters = parameters;
	}

	/**
	 * Gets the Android activity that has been assigned to this {@link Route}.
	 *
	 * @return The Android activity that has been assigned to this {@link Route}.
	 */
	public Class<? extends Activity> getTargetClass() {
		return targetClass;
	}

	/**
	 * Optionally, you can specify an Android activity to open when a predefined route is called. For instance,
	 * if you had a URL like "/users/:userId/projects/:projectId", you'd probably want to open the project activity.
	 * Within this activity you can access the {@link Bundle} within the {@link Route} object and determine behaviour accordingly.
	 *
	 * @param targetClass The Android {@link Activity} to open when the predefined route is called.
	 */
	public void setTargetClass(Class<? extends Activity> targetClass) {
		this.targetClass = targetClass;
	}

	Intent getIntent(Context context) {
		if (getTargetClass() != null) {
			Intent intent = new Intent();
			intent.putExtras(getParameters());
			intent.setClass(context, getTargetClass());
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			return intent;
		}

		return null;
	}
}
