package be.trikke.intentbuilder.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import be.trikke.intentbuilder.BuildIntent;
import be.trikke.intentbuilder.Extra;

@BuildIntent public class MySubClass extends MySuperClass {

	@Extra @Nullable String three;

	@Extra @Nullable String four;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		MySubClassIntent.inject(getIntent(), this);
	}
}
