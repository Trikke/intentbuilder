package be.trikke.intentbuilder.sample;

import android.app.IntentService;
import android.content.Intent;
import be.trikke.intentbuilder.BuildIntent;
import be.trikke.intentbuilder.Extra;

@BuildIntent public class MyService extends IntentService {

	@Extra String downloadUrl;

	public MyService(String name) {
		super(name);
	}

	@Override protected void onHandleIntent(Intent intent) {
		MyServiceIntent.inject(intent, this);
	}
}
