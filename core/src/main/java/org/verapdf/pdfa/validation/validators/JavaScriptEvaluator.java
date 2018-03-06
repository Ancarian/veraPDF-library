package org.verapdf.pdfa.validation.validators;

import org.mozilla.javascript.*;
import org.verapdf.model.baselayer.Object;
import org.verapdf.pdfa.validation.profiles.Rule;
import org.verapdf.pdfa.validation.profiles.RuleId;
import org.verapdf.pdfa.validation.profiles.Variable;

import java.util.HashMap;
import java.util.Map;

public class JavaScriptEvaluator {
	private static final int OPTIMIZATION_LEVEL = 9;

	private static Context context;

	private static Map<RuleId, Script> ruleScripts = new HashMap<>();
	private static Map<String, Script> variableScripts = new HashMap<>();

	public static synchronized ScriptableObject initialise() {
		context = Context.enter();
		context.setOptimizationLevel(OPTIMIZATION_LEVEL);
		return context.initStandardObjects();
	}

	public static synchronized java.lang.Object evaluateString(String source, ScriptableObject scope) {
		return context.evaluateString(scope, source, null, 0, null);
	}

	public static synchronized java.lang.Object evalVariableResult(Variable variable, Object object, ScriptableObject scope) {
		Script script;
		if (!variableScripts.containsKey(variable.getName())) {
			String source = getStringScript(object, variable.getValue());
			script = JavaScriptEvaluator.compileString(source);

			variableScripts.put(variable.getName(), script);
		} else {
			script = variableScripts.get(variable.getName());
		}

		scope.put("obj", scope, object);

		java.lang.Object res = script.exec(context, scope);

		if (res instanceof NativeJavaObject) {
			res = ((NativeJavaObject) res).unwrap();
		}
		return res;
	}

	private static Script compileString(String source) {
		return context.compileString(source, null, 0, null);
	}

	private static String getStringScript(Object obj, String arg) {
		return getScriptPrefix(obj, arg) + arg + getScriptSuffix();
	}

	private static String getScriptPrefix(Object obj, String test) {
		StringBuilder builder = new StringBuilder();
		String[] vars = test.split("\\W");

		for (String prop : obj.getProperties()) {
			if (contains(vars, prop)) {
				builder.append("var ");
				builder.append(prop);
				builder.append(" = obj.get");
				builder.append(prop);
				builder.append("();\n");
			}
		}

		for (String linkName : obj.getLinks()) {
			if (contains(vars, linkName + "_size")) {
				builder.append("var ");
				builder.append(linkName);
				builder.append("_size = obj.getLinkedObjects(\"");
				builder.append(linkName);
				builder.append("\").size();\n");
			}
		}
		builder.append("function test(){return ");
		return builder.toString();
	}

	private static boolean contains(String[] values, String prop) {
		for (String value : values) {
			if (value.equals(prop)) {
				return true;
			}
		}
		return false;
	}

	private static String getScriptSuffix() {
		return ";}\ntest();";
	}

	private static String getScript(Object obj, Rule rule) {
		return getStringScript(obj, "(" + rule.getTest() + ")==true");
	}

	public static synchronized boolean getTestEvalResult(Object obj, Rule rule, ScriptableObject scope) {
		scope.put("obj", scope, obj);

		Script scr;
		if (!ruleScripts.containsKey(rule.getRuleId())) {
			scr = compileString(getScript(obj, rule));
			ruleScripts.put(rule.getRuleId(), scr);
		} else {
			scr = ruleScripts.get(rule.getRuleId());
		}

		boolean testEvalResult = ((Boolean) scr.exec(context, scope)).booleanValue();

		return testEvalResult;
	}

	public static void exitContext() {
		Context.exit();
	}
}
