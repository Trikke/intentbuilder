## IntentBuilder
IntentBuilder is a type safe way of creating intents and populating them with extras. Use them with your Activities, Services, Broadcastreceivers and other components.

## Installation
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
    compile 'be.trikke:intentbuilder-api:1.2.1'
    apt 'be.trikke:intentbuilder-compiler:1.2.1'
}
```

####  Version History

* v1.0
Initial release
* v1.1
Added support for Broadcastreceivers. Changed the usage of `@Nullable` and created an extra annotation `@ExtraOptional` to use with optional values.
* v1.2
Added ability to have urls as a usage of navigation.

## Usage
Annotate your activities and services with an `@BuildIntent` annotation so that they are picked up by the library. For every class with an `@BuildIntent` annotation a class named `<ComponentName>Intent` will be generated. If your Component takes in parameters via extras in the intent you can now mark field with the `@Extra` annotation and they can be injected with the static `inject` method on the generated intent builder class. Extras can be marked as optional with the `@ExtraOptional` annotation.

Sample activity using IntentBuilder:
```java
@BuildIntent public class DetailActivity extends AppCompatActivity {

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

        // use extras
    }
}
```

You can also use the `@BuildIntentUrl` annotation to add url schemes to a certain component.

```java
@BuildIntentUrl(value = {
		@UrlPath(name = "DETAILS",url = "test://parameters?requiredString={string}&requiredBoolean={boolean}"),
		@UrlPath(name = "MORE",url = "test://more"),
		@UrlPath(name = "STUFF",url = "test://stuff"),
}) public class DetailActivity extends AppCompatActivity {
	@Extra String requiredString;
    @Extra boolean requiredBoolean;
}
```
Make sure the parameters in the url scheme have the same name as the `@Extra` fields. Otherwise they will not be injected correctly.

## Launching

Use the builder to create the Intent.
```java
Intent i = new DetailActivityIntent("Required String", true)
                      .optionalString("Optional String")
                      .optionalInteger(102)
                      .build(MainActivity.this);
startActivity(i);
```

Or quickly build an intent with the provided `Navigate` shortcut class.
```java
Navigate.gotoDetailActivity("i'm a required String", true)
                      .optionalString("i'm just optional")
                      .coolName("Trikke")
                      .optionalBoolean(true)
                      .flag(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                      .launch(MainActivity.this);
```

Use the provided `Navigate` shortcut class to quickly launch an Intent with it's required extra's (if any).
```java
Navigate.launchDetailActivity(MainActivity.this, "required", true)
```

The `Navigate` provides quick methods for getting all your generated Intents, and has launch methods for compatible Components.

```java
Navigate.getMyBroadcastReceiver("A required String").action(MY_INTENT_FILTER).send(MainActivity.this);
```

```java
Navigate.startMyService(MainActivity.this, "Required String");
```

If you have components with url schemes, the provided `Navigate` shortcut class contains methods to launch and resolve these urls. Make sure to only launch known urls.
```java
Navigate.launch(MainActivity.this, "test://stuff")
```


## Contributing
Please do! Let me know what you think, and PR's are always welcome!

## Thanks
Thanks to [emilsjolander](https://github.com/emilsjolander/IntentBuilder) and [robertoestivill](https://github.com/robertoestivill/intentbuilder) for their source and inspiring me to get off my butt and make this.