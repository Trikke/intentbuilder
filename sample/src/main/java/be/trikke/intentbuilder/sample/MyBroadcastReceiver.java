package be.trikke.intentbuilder.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import be.trikke.intentbuilder.BuildIntent;
import be.trikke.intentbuilder.Extra;

@BuildIntent public class MyBroadcastReceiver extends BroadcastReceiver {

	@Extra String toastMessage;

	@Override public void onReceive(Context context, Intent intent) {
		MyBroadcastReceiverIntent.inject(intent, this);
		Toast.makeText(context, toastMessage, Toast.LENGTH_LONG).show();
	}
}