package be.trikke.intentbuilder;

import com.squareup.javapoet.TypeName;

public class PrimitiveDefaults {
	// These gets initialized to their default values
	private static boolean DEFAULT_BOOLEAN;
	private static byte DEFAULT_BYTE;
	private static short DEFAULT_SHORT;
	private static int DEFAULT_INT;
	private static long DEFAULT_LONG;
	private static float DEFAULT_FLOAT;
	private static double DEFAULT_DOUBLE;

	public static Object getDefaultValue(TypeName clazz) {
		if (clazz.equals(TypeName.BOOLEAN)) {
			return DEFAULT_BOOLEAN;
		} else if (clazz.equals(TypeName.BYTE)) {
			return DEFAULT_BYTE;
		} else if (clazz.equals(TypeName.SHORT)) {
			return DEFAULT_SHORT;
		} else if (clazz.equals(TypeName.INT)) {
			return DEFAULT_INT;
		} else if (clazz.equals(TypeName.LONG)) {
			return DEFAULT_LONG;
		} else if (clazz.equals(TypeName.FLOAT)) {
			return DEFAULT_FLOAT;
		} else if (clazz.equals(TypeName.DOUBLE)) {
			return DEFAULT_DOUBLE;
		} else {
			return null;
		}
	}
}