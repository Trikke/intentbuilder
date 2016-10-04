package be.trikke.intentbuilder.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import be.trikke.intentbuilder.BuildIntent;
import be.trikke.intentbuilder.Extra;

@BuildIntent public class DetailActivity extends AppCompatActivity {

	@Extra String one;

	@Extra String two;

	@Extra @Nullable String three;

	@Extra @Nullable int four;

	@Extra("five") @Nullable String mFive;

	@Extra @Nullable boolean six;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DetailActivityIntent.inject(getIntent(), this);
		setContentView(be.trikke.intentbuilder.sample.R.layout.activity_detail);

		((TextView) findViewById(be.trikke.intentbuilder.sample.R.id.one)).setText(one);
		((TextView) findViewById(be.trikke.intentbuilder.sample.R.id.two)).setText(two);
		((TextView) findViewById(be.trikke.intentbuilder.sample.R.id.three)).setText(three);
		((TextView) findViewById(be.trikke.intentbuilder.sample.R.id.four)).setText("integer > " + four);
		((TextView) findViewById(be.trikke.intentbuilder.sample.R.id.five)).setText(mFive);
		((TextView) findViewById(be.trikke.intentbuilder.sample.R.id.four)).setText("boolean > " + six);
	}
}
