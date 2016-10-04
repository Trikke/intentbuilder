#IntentBuilder
IntentBuilder is a type safe way of creating intents and populating them with extras. Use them with your Activities and Services.

##Installation
```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.neenbedankt.gradle.plugins:android-apt:1.4'
    }
}

apply plugin: 'com.neenbedankt.android-apt'

dependencies {
    compile 'be.trikke:intentbuilder-api:1.0.0'
    apt 'be.trikke:intentbuilder-compiler:1.0.0'
}
```

##Usage
Annotate your activities and services with an `@BuildIntent` annotation so that they are picked up by the library. For every class with an `@BuildIntent` annotation a class named `<ComponentName>Intent` will be generated. If your Component takes in parameters via extras in the intent you can now mark field with the `@Extra` annotation and they can be injected with the static `inject` method on the generated intent builder class. Extras can be marked as optional with the `@Nullable` annotation.

Sample activity using IntentBuilder:
```java
@BuildIntent
public class DetailActivity extends AppCompatActivity {

	@Extra String one;

	@Extra String two;

	@Extra @Nullable String three;

	@Extra @Nullable int four;

	@Extra("five") @Nullable String mFive;

	@Extra @Nullable boolean six;

	@Override public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DetailActivityIntent.inject(getIntent(), this);
		// use extras
	}
}
```

##Launching

Use the builder to create the Intent.
```java
Intent i = new DetailActivityIntent("een", "twee")
                        .three("drie")
                        .five("5")
                        .build(MainActivity.this);
startActivity(i);
```

Or quickly build an intent with the provided `Flow` shortcut class.
```java
Flow.gotoDetailActivity("een", "twee").three("drie").five("5").six(true).flag(Intent.FLAG_ACTIVITY_CLEAR_TOP).launch(MainActivity.this);
```

Use the provided `Flow` shortcut class to quickly launch an Intent with it's required extra's (if any).
```java
Flow.launchDetailActivity(MainActivity.this, "een","twee")
```

The `Flow` provides quick methods for getting all your generated Intents, and has launch methods for compatible Components.

##Contributing
Please do! Let me know what you think, and PR's are always welcome!

##Thanks
Thanks to [emilsjolander](https://github.com/emilsjolander/IntentBuilder) and [robertoestivill](https://github.com/robertoestivill/intentbuilder) for their source and inspiring me to get off my butt and make this.