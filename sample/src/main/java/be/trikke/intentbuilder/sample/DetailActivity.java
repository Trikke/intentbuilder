package be.trikke.intentbuilder.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import be.trikke.intentbuilder.BuildIntent;
import be.trikke.intentbuilder.BuildIntentUrl;
import be.trikke.intentbuilder.Extra;
import be.trikke.intentbuilder.ExtraOptional;
import be.trikke.intentbuilder.UrlPath;

@BuildIntentUrl(value = {
		@UrlPath(name = "PATH DETAILS_AND SOME-ELSE",url = "test://parameters?requiredString={string}&requiredBoolean={boolean}"),
		@UrlPath(name = "PATH_MORE",url = "test://more"),
		@UrlPath(name = "STUFF",url = "test://stuff"),
})
public class DetailActivity extends AppCompatActivity {

	@Extra String requiredString;

	@Extra boolean requiredBoolean;

	@ExtraOptional String optionalString;

	@ExtraOptional int optionalInteger;

	@ExtraOptional("coolName") String optionalStringDiffName;

	@ExtraOptional boolean optionalBoolean;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DetailActivityIntent.inject(getIntent(), this);
		setContentView(R.layout.activity_detail);

		((TextView) findViewById(R.id.one)).setText("Required String: " + requiredString);
		((TextView) findViewById(R.id.two)).setText("Required Boolean: " + requiredBoolean);
		((TextView) findViewById(R.id.three)).setText("Optional String: " + optionalString);
		((TextView) findViewById(R.id.four)).setText("Optional Integer:  " + optionalInteger);
		((TextView) findViewById(R.id.five)).setText("Optional String (coolName): " + optionalStringDiffName);
		((TextView) findViewById(R.id.four)).setText("Optional Boolean: " + optionalBoolean);
	}
}
