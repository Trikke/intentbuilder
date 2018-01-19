package be.trikke.intentbuilder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

public class Processor extends AbstractProcessor {

	private static final String ACTIVITY_FULL_NAME = "android.app.Activity"; // All activities extend from this
	private static final String SERVICE_FULL_NAME = "android.app.Service"; // All services extend from this
	private static final String BROADCASTRECEIVER_FULL_NAME = "android.content.BroadcastReceiver"; // All broadcastreceivers extend from this

	private static Processor instance;

	private Types typeUtils;
	private Elements elementUtils;
	private Filer filer;
	private Messager messager;
	private ArrayList<Element> generatedForClasses;
	private ArrayList<FoundPath> foundUrlPaths;

	@Override public Set<String> getSupportedAnnotationTypes() {
		return new HashSet<String>() {{
			add(BuildIntent.class.getCanonicalName());
			add(BuildIntentUrl.class.getCanonicalName());
		}};
	}

	@Override public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		instance = this;
		typeUtils = processingEnv.getTypeUtils();
		elementUtils = processingEnv.getElementUtils();
		filer = processingEnv.getFiler();
		messager = processingEnv.getMessager();
		generatedForClasses = new ArrayList<>();
		foundUrlPaths = new ArrayList<>();
	}

	@Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Set<? extends Element> set = roundEnv.getElementsAnnotatedWith(BuildIntent.class);
		Set<? extends Element> set2 = roundEnv.getElementsAnnotatedWith(BuildIntentUrl.class);

		try {
			// generate one class for easy navigation
			if (!set.isEmpty()) {
				JavaFile navigatorFile = JavaFile.builder("be.trikke.intentbuilder", getNavigatorSpec(set, set2)).build();
				navigatorFile.writeTo(filer);
			}
		} catch (Exception e) {
			messager.printMessage(Diagnostic.Kind.ERROR, "Can't create navigation class :" + e.toString());
		}

		processSet(set, BuildIntent.class);
		processSet(set2, BuildIntentUrl.class);

		return true;
	}

	private boolean processSet(Set<? extends Element> set, Class<? extends Annotation> annotation) {
		for (Element annotatedElement : set) {
			// Make sure element is a field or a method declaration
			if (!annotatedElement.getKind().isClass()) {
				error(annotatedElement, "Only classes can be annotated with @%s", annotation.getSimpleName());
				return true;
			}
			// generate separate builders
			try {
				TypeSpec builderSpec = getBuilderSpec(annotatedElement);
				JavaFile builderFile = JavaFile.builder(getPackageName(annotatedElement), builderSpec).build();
				builderFile.writeTo(filer);
				generatedForClasses.add(annotatedElement);
			} catch (Exception e) {
				error(annotatedElement, "Could not create intent builder for %s: %s", annotatedElement.getSimpleName(), e.getMessage());
			}
		}
		return false;
	}

	private void error(Element e, String msg, Object... args) {
		messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
	}

	private String getPackageName(Element e) {
		while (!(e instanceof PackageElement)) {
			e = e.getEnclosingElement();
		}
		return ((PackageElement) e).getQualifiedName().toString();
	}

	private boolean isElementInstanceOfActivity(Element element) {
		return isElementInstanceOfClass(element, ACTIVITY_FULL_NAME);
	}

	private boolean isElementInstanceOfService(Element element) {
		return isElementInstanceOfClass(element, SERVICE_FULL_NAME);
	}

	private boolean isElementInstanceOfBroadcastReceiver(Element element) {
		return isElementInstanceOfClass(element, BROADCASTRECEIVER_FULL_NAME);
	}

	private boolean isElementInstanceOfClass(Element element, String fullClassName) {
		boolean ret = false;
		if (element.getKind() == ElementKind.CLASS) {

			TypeElement currentClass = (TypeElement) element;
			while (true) {
				TypeMirror superClassType = currentClass.getSuperclass();

				/** Super class is an instance of {@link Object}, we have reach the end of inheritance */
				if (superClassType.getKind() == TypeKind.NONE) {
					break;
				}

				// Found the required class
				if (superClassType.toString().equals(fullClassName)) {
					ret = true;
					break;
				}

				// Moving up in the inheritance tree
				currentClass = (TypeElement) typeUtils.asElement(superClassType);
			}
		}

		return ret;
	}

	private TypeSpec getNavigatorSpec(Set<? extends Element>... generated) {
		TypeSpec.Builder builder = TypeSpec.classBuilder("Navigate").addModifiers(Modifier.PUBLIC, Modifier.FINAL);
		for (Set<? extends Element> set : generated) {
			for (Element element : set) {
				boolean isActivity = isElementInstanceOfActivity(element);
				boolean isService = isElementInstanceOfService(element);
				boolean isBroadcastreceiver = isElementInstanceOfBroadcastReceiver(element);
				String gotoKeyword = isActivity ? "goto" : "get";
				String launchKeyword;
				if (isActivity) {
					launchKeyword = "launch";
				} else if (isService) {
					launchKeyword = "start";
				} else {
					launchKeyword = "send";
				}

				List<Element> required = new ArrayList<>();
				List<Element> optional = new ArrayList<>();

				getAnnotatedFields(element, required, optional);

				String name = String.format("%sIntent", element.getSimpleName());
				ClassName className = ClassName.get(getPackageName(element), name);

				Map<String, String> paths = new HashMap<>();
				paths.put(element.getSimpleName().toString(), null);

				if (element.getAnnotation(BuildIntentUrl.class) != null) {
					UrlPath[] value = element.getAnnotation(BuildIntentUrl.class).value();
					paths = new HashMap<>(value.length);
					for (UrlPath path : value) {
						String pathName = cleanUpName(path.name());
						foundUrlPaths.add(new FoundPath(pathName, path.url(), element));
						paths.put(pathName, path.url());
					}
				}
				for (Map.Entry<String, String> entry : paths.entrySet()) {
					String methodName = entry.getKey();

					MethodSpec.Builder gotoMethod = MethodSpec.methodBuilder(gotoKeyword + methodName);
					MethodSpec.Builder launchMethod = MethodSpec.methodBuilder(launchKeyword + methodName)
					                                            .addParameter(Context.class, "context")
					                                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

					MethodSpec.Builder launchWithActivityForResultMethod = MethodSpec.methodBuilder("launch" + methodName + "ForResult")
					                                                                 .addParameter(Activity.class, "activity")
					                                                                 .addParameter(int.class, "requestCode");
					MethodSpec.Builder launchWithFragmentForResultMethod = MethodSpec.methodBuilder("launch" + methodName + "ForResult")
					                                                                 .addParameter(ClassName.get("android.support.v4.app", "Fragment"),
							                                                                 "fragment").addParameter(int.class, "requestCode");
					StringBuilder launchParams = new StringBuilder();
					for (Element e : required) {
						String paramName = getParamName(e);
						gotoMethod.addParameter(TypeName.get(e.asType()), paramName);
						launchMethod.addParameter(TypeName.get(e.asType()), paramName);
						launchWithActivityForResultMethod.addParameter(TypeName.get(e.asType()), paramName);
						launchWithFragmentForResultMethod.addParameter(TypeName.get(e.asType()), paramName);
						if (launchParams.length() > 0) launchParams.append(", ");
						launchParams.append(paramName);
					}
					if (entry.getValue() != null) {
						if (launchParams.length() > 0) {
							launchParams.append(", ");
						}
						launchParams.append("PATH_" + entry.getKey().toUpperCase());
					}

					gotoMethod.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					          .addStatement("return new $L($L)", name, launchParams.toString())
					          .returns(className);
					launchMethod.addStatement("new $L($L)." + launchKeyword + "(context)", name, launchParams.toString());
					launchWithActivityForResultMethod.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					                                 .addStatement("new $L($L).launchForResult(activity,requestCode)", name, launchParams.toString());
					launchWithFragmentForResultMethod.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
					                                 .addStatement("new $L($L).launchForResult(fragment,requestCode)", name, launchParams.toString());

					builder.addMethod(gotoMethod.build());
					if (isActivity || isService || isBroadcastreceiver) {
						builder.addMethod(launchMethod.build());
					}
					if (isActivity) {
						builder.addMethod(launchWithActivityForResultMethod.build());
						builder.addMethod(launchWithFragmentForResultMethod.build());
					}
				}
			}

			for (FoundPath path : foundUrlPaths) {
				FieldSpec pathMethod = FieldSpec.builder(String.class, "PATH_" + path.getName().toUpperCase())
				                                .initializer("\"" + path.getPath() + "\"")
				                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
				                                .build();
				builder.addField(pathMethod);
			}
		}

		if (!foundUrlPaths.isEmpty()) {
			builder.addField(Router.class, "router", Modifier.PRIVATE, Modifier.STATIC);
			CodeBlock.Builder staticInitializer = CodeBlock.builder();
			staticInitializer.addStatement("router = new Router()");
			for (FoundPath path : foundUrlPaths) {
				staticInitializer.addStatement("router.route($N, $N)", "PATH_" + path.getName().toUpperCase(),
						String.format("%sIntent", path.getElement().getSimpleName()) + ".getTarget()");
			}

			builder.addStaticBlock(staticInitializer.build());

			MethodSpec.Builder gotoUrlMethod = MethodSpec.methodBuilder("gotoUrl");
			gotoUrlMethod.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
			             .addParameter(Context.class, "context")
			             .addParameter(String.class, "url")
			             .addStatement("return router.getIntent($L,$L)", "context", "url")
			             .returns(Intent.class);

			builder.addMethod(gotoUrlMethod.build());

			MethodSpec.Builder openUrlMethod = MethodSpec.methodBuilder("launchUrl");
			openUrlMethod.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
			             .addParameter(Context.class, "context")
			             .addParameter(String.class, "url")
			             .addStatement("router.call($L,$L)", "context", "url");

			builder.addMethod(openUrlMethod.build());

			MethodSpec.Builder openUrlWithFlagsMethod = MethodSpec.methodBuilder("launchUrl");
			openUrlWithFlagsMethod.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
			                      .addParameter(Context.class, "context")
			                      .addParameter(String.class, "url")
			                      .addParameter(Integer.class, "flags")
			                      .addStatement("router.call($L,$L,$L)", "context", "url", "flags");

			builder.addMethod(openUrlWithFlagsMethod.build());

			MethodSpec.Builder consumeMethod = MethodSpec.methodBuilder("consumeUrl");
			consumeMethod.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
			             .addParameter(Activity.class, "activity")
			             .addParameter(String.class, "url")
			             .beginControlFlow("if(activity.getIntent().hasExtra(\"route\"))")
			             .beginControlFlow("if(activity.getIntent().getStringExtra(\"route\").equalsIgnoreCase(url))")
			             .addStatement("activity.getIntent().removeExtra(\"route\")")
			             .addStatement("return true")
			             .endControlFlow()
			             .endControlFlow()
			             .addStatement("return false")
			             .returns(TypeName.BOOLEAN);

			builder.addMethod(consumeMethod.build());
		}
		return builder.build();
	}

	private TypeSpec getBuilderSpec(Element annotatedElement) {
		boolean isActivity = isElementInstanceOfActivity(annotatedElement);
		boolean isService = isElementInstanceOfService(annotatedElement);
		boolean isBroadcastreceiver = isElementInstanceOfBroadcastReceiver(annotatedElement);

		List<Element> required = new ArrayList<>();
		List<Element> optional = new ArrayList<>();
		List<Element> all = new ArrayList<>();

		getAnnotatedFields(annotatedElement, required, optional);
		all.addAll(required);
		all.addAll(optional);

		final String name = String.format("%sIntent", annotatedElement.getSimpleName());
		TypeSpec.Builder builder = TypeSpec.classBuilder(name).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
		builder.addField(Intent.class, "intent", Modifier.PRIVATE);
		MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
		constructor.addStatement("intent = new Intent()");
		if (annotatedElement.getAnnotation(BuildIntentUrl.class) != null) {
			// constructor with required params and url path
			for (Element e : required) {
				String paramName = getParamName(e);
				constructor.addParameter(TypeName.get(e.asType()), paramName);
				constructor.addStatement("intent.putExtra($S, $N)", paramName, paramName);
			}
			constructor.addParameter(String.class, "route");
			constructor.addStatement("intent.putExtra($S, $N)", "route", "route");
		} else {
			// constructor with required params
			for (Element e : required) {
				String paramName = getParamName(e);
				constructor.addParameter(TypeName.get(e.asType()), paramName);
				constructor.addStatement("intent.putExtra($S, $N)", paramName, paramName);
			}
		}
		builder.addMethod(constructor.build());

		MethodSpec.Builder actionMethod = MethodSpec.methodBuilder("action")
		                                            .addModifiers(Modifier.PUBLIC)
		                                            .addParameter(String.class, "action")
		                                            .addStatement("intent.setAction(action)")
		                                            .addStatement("return this")
		                                            .returns(ClassName.get(getPackageName(annotatedElement), name));
		builder.addMethod(actionMethod.build());

		MethodSpec.Builder dataMethod = MethodSpec.methodBuilder("data")
		                                          .addModifiers(Modifier.PUBLIC)
		                                          .addParameter(Uri.class, "data")
		                                          .addStatement("intent.setData(data)")
		                                          .addStatement("return this")
		                                          .returns(ClassName.get(getPackageName(annotatedElement), name));
		builder.addMethod(dataMethod.build());

		MethodSpec.Builder typeMethod = MethodSpec.methodBuilder("type")
		                                          .addModifiers(Modifier.PUBLIC)
		                                          .addParameter(String.class, "type")
		                                          .addStatement("intent.setType(type)")
		                                          .addStatement("return this")
		                                          .returns(ClassName.get(getPackageName(annotatedElement), name));
		builder.addMethod(typeMethod.build());

		MethodSpec.Builder flagMethod = MethodSpec.methodBuilder("flag")
		                                          .addModifiers(Modifier.PUBLIC)
		                                          .addParameter(int.class, "flag")
		                                          .addStatement("return flags(flag)")
		                                          .returns(ClassName.get(getPackageName(annotatedElement), name));
		builder.addMethod(flagMethod.build());

		MethodSpec.Builder flagsMethod = MethodSpec.methodBuilder("flags")
		                                           .addModifiers(Modifier.PUBLIC)
		                                           .varargs(true)
		                                           .addParameter(int[].class, "flags")
		                                           .beginControlFlow("for (int flag : flags)")
		                                           .addStatement("intent.addFlags(flag)")
		                                           .endControlFlow()
		                                           .addStatement("return this")
		                                           .returns(ClassName.get(getPackageName(annotatedElement), name));
		builder.addMethod(flagsMethod.build());

		for (Element e : optional) {
			String paramName = getParamName(e);
			builder.addMethod(MethodSpec.methodBuilder(paramName)
			                            .addModifiers(Modifier.PUBLIC)
			                            .addParameter(TypeName.get(e.asType()), paramName)
			                            .addStatement("intent.putExtra($S, $N)", paramName, paramName)
			                            .addStatement("return this")
			                            .returns(ClassName.get(getPackageName(annotatedElement), name))
			                            .build());
		}
		//public Intent putExtra(String name, boolean value)
		MethodSpec.Builder putExtraMethod;
		for (TypeName type : PrimitiveDefaults.getAll()) {
			putExtraMethod = MethodSpec.methodBuilder("putExtra")
			                           .addModifiers(Modifier.PUBLIC)
			                           .addParameter(String.class, "name")
			                           .addParameter(type, "value")
			                           .addStatement("intent.putExtra(name, value)")
			                           .addStatement("return this")
			                           .returns(ClassName.get(getPackageName(annotatedElement), name));
			builder.addMethod(putExtraMethod.build());
		}
		// add also one for String,String
		putExtraMethod = MethodSpec.methodBuilder("putExtra")
		                           .addModifiers(Modifier.PUBLIC)
		                           .addParameter(String.class, "name")
		                           .addParameter(String.class, "value")
		                           .addStatement("intent.putExtra(name, value)")
		                           .addStatement("return this")
		                           .returns(ClassName.get(getPackageName(annotatedElement), name));
		builder.addMethod(putExtraMethod.build());

		MethodSpec.Builder getExtrasMethod =
				MethodSpec.methodBuilder("getExtras").addModifiers(Modifier.PUBLIC).returns(Bundle.class).addStatement("return intent.getExtras()");
		builder.addMethod(getExtrasMethod.build());

		MethodSpec.Builder buildMethod = MethodSpec.methodBuilder("build")
		                                           .addModifiers(Modifier.PUBLIC)
		                                           .addParameter(Context.class, "context")
		                                           .addStatement("intent.setClass(context, $T.class)", TypeName.get(annotatedElement.asType()))
		                                           .returns(Intent.class)
		                                           .addStatement("return intent");
		builder.addMethod(buildMethod.build());

		if (isActivity || isService || isBroadcastreceiver) {
			MethodSpec.Builder launchMethod;
			if (isActivity) {
				launchMethod = MethodSpec.methodBuilder("launch").addStatement("intent.setClass(context, $T.class)", TypeName.get(annotatedElement.asType()));
				launchMethod.addStatement("context.startActivity(intent)");
			} else if (isService) {
				launchMethod = MethodSpec.methodBuilder("start").addStatement("intent.setClass(context, $T.class)", TypeName.get(annotatedElement.asType()));
				launchMethod.addStatement("context.startService(intent)");
			} else {
				launchMethod = MethodSpec.methodBuilder("send").addStatement("intent.setClass(context, $T.class)", TypeName.get(annotatedElement.asType()));
				launchMethod.addStatement("context.sendBroadcast(intent)");
			}
			launchMethod.addModifiers(Modifier.PUBLIC).addParameter(Context.class, "context");
			builder.addMethod(launchMethod.build());
		}

		if (isActivity) {
			MethodSpec.Builder launchForResultMethod = MethodSpec.methodBuilder("launchForResult")
			                                                     .addModifiers(Modifier.PUBLIC)
			                                                     .addParameter(Activity.class, "activity")
			                                                     .addParameter(int.class, "requestCode")
			                                                     .addStatement("intent.setClass(activity, $T.class)", TypeName.get(annotatedElement.asType()))
			                                                     .addStatement("intent.putExtra(\"requestcode\", requestCode)")
			                                                     .addStatement("activity.startActivityForResult(intent, requestCode)");
			builder.addMethod(launchForResultMethod.build());

			MethodSpec.Builder launchForResultWithFragmentMethod = MethodSpec.methodBuilder("launchForResult")
			                                                                 .addModifiers(Modifier.PUBLIC)
			                                                                 .addParameter(ClassName.get("android.support.v4.app", "Fragment"), "fragment")
			                                                                 .addParameter(int.class, "requestCode")
			                                                                 .addStatement("intent.setClass(fragment.getActivity(), $T.class)",
					                                                                 TypeName.get(annotatedElement.asType()))
			                                                                 .addStatement("intent.putExtra(\"requestcode\", requestCode)")
			                                                                 .addStatement("fragment.startActivityForResult(intent, requestCode)");
			builder.addMethod(launchForResultWithFragmentMethod.build());

			MethodSpec.Builder justinjectMethod = MethodSpec.methodBuilder("inject")
			                                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
			                                                .addParameter(TypeName.get(annotatedElement.asType()), "activity")
			                                                .addStatement("inject(activity.getIntent(), activity, false)");
			builder.addMethod(justinjectMethod.build());
		}

		MethodSpec.Builder injectWithOptionalsMethod = MethodSpec.methodBuilder("inject")
		                                                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
		                                                         .addParameter(Intent.class, "intent")
		                                                         .addParameter(TypeName.get(annotatedElement.asType()), "component")
		                                                         .addStatement("inject(intent, component, false)");
		builder.addMethod(injectWithOptionalsMethod.build());

		MethodSpec.Builder injectMethod = MethodSpec.methodBuilder("inject")
		                                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
		                                            .addParameter(Intent.class, "intent")
		                                            .addParameter(TypeName.get(annotatedElement.asType()), "component")
		                                            .addParameter(boolean.class, "writeDefaultValues")
		                                            .addStatement("$T extras = intent.getExtras()", Bundle.class)
		                                            .beginControlFlow("if(extras == null)")
		                                            .addStatement("// no need to actually inject anything")
		                                            .addStatement("return")
		                                            .endControlFlow();
		for (Element e : all) {
			String paramName = getParamName(e);
			injectMethod.beginControlFlow("if (extras.containsKey($S))", paramName)
			            .addStatement("component.$N = ($T) extras.get($S)", e.getSimpleName().toString(), e.asType(), paramName)
			            .nextControlFlow("else if (writeDefaultValues)");
			if (TypeName.get(e.asType()).isPrimitive()) {
				injectMethod.addStatement("component.$N = $L", e.getSimpleName().toString(), PrimitiveDefaults.getDefaultValue(TypeName.get(e.asType())))
				            .endControlFlow();
			} else {
				injectMethod.addStatement("component.$N = null", e.getSimpleName().toString()).endControlFlow();
			}
		}
		builder.addMethod(injectMethod.build());

		ClassName className = ClassName.get(getPackageName(annotatedElement), annotatedElement.getSimpleName().toString());
		TypeName wildcard = WildcardTypeName.subtypeOf(className);

		TypeName classOfAny = ParameterizedTypeName.get(ClassName.get(Class.class), wildcard);
		MethodSpec.Builder targetGetterMethod = MethodSpec.methodBuilder("getTarget")
		                                                  .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
		                                                  .returns(classOfAny)
		                                                  .addStatement("return " + className + ".class");
		builder.addMethod(targetGetterMethod.build());

		return builder.build();
	}

	private String getParamName(Element e) {
		String extraValue = null;
		if (e.getAnnotation(Extra.class) != null) {
			extraValue = e.getAnnotation(Extra.class).value();
		}
		if (e.getAnnotation(ExtraOptional.class) != null) {
			extraValue = e.getAnnotation(ExtraOptional.class).value();
		}
		return extraValue != null && extraValue.trim().length() > 0 ? extraValue : e.getSimpleName().toString();
	}

	private void getAnnotatedFields(Element annotatedElement, List<Element> required, List<Element> optional) {
		for (Element e : annotatedElement.getEnclosedElements()) {
			if (e.getAnnotation(Extra.class) != null) {
				required.add(e);
			}
			if (e.getAnnotation(ExtraOptional.class) != null) {
				optional.add(e);
			}
		}

		List<? extends TypeMirror> superTypes = Processor.instance.typeUtils.directSupertypes(annotatedElement.asType());
		TypeMirror superClassType = superTypes.size() > 0 ? superTypes.get(0) : null;
		Element superClass = superClassType == null ? null : Processor.instance.typeUtils.asElement(superClassType);
		if (superClass != null && superClass.getKind() == ElementKind.CLASS) {
			getAnnotatedFields(superClass, required, optional);
		}
	}

	private boolean hasAnnotation(Element e, String name) {
		for (AnnotationMirror annotation : e.getAnnotationMirrors()) {
			if (annotation.getAnnotationType().asElement().getSimpleName().toString().equals(name)) {
				return true;
			}
		}
		return false;
	}

	private String cleanUpName(String phrase) {
		phrase = phrase.replaceAll("-", "_");
		phrase = phrase.replaceAll(" ", "_");
		phrase = phrase.toLowerCase();
		phrase = phrase.substring(0, 1).toUpperCase() + phrase.substring(1);
		while (phrase.contains("_")) {
			phrase = phrase.replaceFirst("_[a-zA-Z0-9]", String.valueOf(Character.toUpperCase(phrase.charAt(phrase.indexOf("_") + 1))));
		}
		return phrase;
	}
}