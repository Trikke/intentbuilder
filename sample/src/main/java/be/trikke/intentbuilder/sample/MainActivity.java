package be.trikke.intentbuilder.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import be.trikke.intentbuilder.Flow;

public class MainActivity extends AppCompatActivity {

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(be.trikke.intentbuilder.sample.R.layout.activity_main);

		Button goToDetail = (Button) findViewById(be.trikke.intentbuilder.sample.R.id.go_to_detail);
		goToDetail.setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				Flow.gotoDetailActivity("een", "twee").three("drie").five("5").six(true).flag(Intent.FLAG_ACTIVITY_CLEAR_TOP).launch(MainActivity.this);
			}
		});
	}
}
