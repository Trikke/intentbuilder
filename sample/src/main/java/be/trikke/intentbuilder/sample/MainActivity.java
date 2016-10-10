package be.trikke.intentbuilder.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import be.trikke.intentbuilder.Flow;

public class MainActivity extends AppCompatActivity {

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button goToDetail = (Button) findViewById(R.id.go_to_detail);
		goToDetail.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				Flow.gotoDetailActivity("i'm a required String", true)
						.optionalString("i'm just optional")
						.coolName("Trikke")
						.optionalBoolean(true)
						.flag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
						.launch(MainActivity.this);
			}
		});

		Button sendBroadcast = (Button) findViewById(R.id.send_broadcast);
		sendBroadcast.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View view) {
				EditText editText = (EditText) findViewById(R.id.toast_message);
				String message = editText.getText().length() > 0 ? editText.getText().toString() : "Just a toast message";
				Flow.getMyBroadcastReceiver(message).action("muhFilter").send(MainActivity.this);
			}
		});
	}
}
