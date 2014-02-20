package ist.meic.pa;

import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;

/* We create 2 lists (as fields) for each topmost (in class hierarchy) class:
 *	_$initialized$_: stores which non-static fields have already been initialized. There is a list for each instance of a class
 *	_$initializedStatic$_: stores which static fields have already been initialized. This list is static, hence there is only 1 list per topmost class
 * Fields are stored with the name <class name>$<field name> in order to have unique names
 */

public class FieldAssertionInjector extends Injector {

	void processClass(CtClass ctClass) throws NotFoundException, CannotCompileException {
		final CtClass tempClass = ctClass;
		createListsIfNeeded(ctClass);
		for(CtBehavior ctBehavior: ctClass.getDeclaredBehaviors()) {
			ctBehavior.instrument(new ExprEditor() {
				public void edit(FieldAccess fa)throws CannotCompileException {
					// get field assertions
					Object[] annotations = null;
					try {
						annotations = fa.getField().getAnnotations();
					}
					catch(ClassNotFoundException e) { e.printStackTrace(); }
					catch(NotFoundException e) { e.printStackTrace(); }
					List<Object> assertions = new ArrayList<Object>();
					for(Object annotation: annotations) {
						if(annotation instanceof Assertion) {
							assertions.add(annotation);
						}
					}
					if(!assertions.isEmpty()) {
						try {
							CtClass declaringClass = fa.getField().getDeclaringClass();
							// just in case declaringClass hasn't been processed by this Injector yet
							if(!declaringClass.subclassOf(tempClass) && !tempClass.subclassOf(declaringClass) && !tempClass.equals(declaringClass)) {
								createListsIfNeeded(declaringClass);
							}
						}
						catch(NotFoundException e) { e.printStackTrace(); }
						if(fa.isReader()) {
							String name = fa.getFieldName();
							String temp = null;
							try {
								if(Modifier.isStatic(fa.getField().getModifiers())) {
									// when field is static we have to use the class instead of the object containing the field (whose meta-variable '$0' is actually null)
									String className = fa.getField().getDeclaringClass().getName();
									temp = "if (!" + className + "._$initializedStatic$_.contains(\"" + className + "$" + name + "\")) {" +
											"	throw new RuntimeException(\"Error: " + name + " was not initialized\");" +
											"}" +
											"$_ = $proceed($$);";
								}
								else {
									temp = "if (!$0._$initialized$_.contains(\"" + fa.getField().getDeclaringClass().getName() + "$" + name + "\")) {" +
											"	throw new RuntimeException(\"Error: " + name + " was not initialized\");" +
											"}" +
											"$_ = $proceed($$);";
								}
							}
							catch (NotFoundException e) { e.printStackTrace(); }
							fa.replace(temp);
						}
						if(fa.isWriter()) {
							String name = fa.getFieldName();
							String temp = null;
							try {
								if(Modifier.isStatic(fa.getField().getModifiers())) {
									// when field is static we have to use the class instead of the object containing the field (whose meta-variable '$0' is actually null)
									String className = fa.getField().getDeclaringClass().getName();
									temp = className + "." + name + " = $1;" +
											"if (!" + className + "._$initializedStatic$_.contains(\"" + className + "$" + name + "\")) {" +
											"	" + className + "._$initializedStatic$_.add(\"" + className + "$" + name + "\");" +
											"}";
								}
								else {
									temp = "$0." + name + " = $1;" +
											"if (!$0._$initialized$_.contains(\"" + fa.getField().getDeclaringClass().getName() + "$" + name + "\")) {" +
											"	$0._$initialized$_.add(\"" + fa.getField().getDeclaringClass().getName() + "$" + name + "\");" +
											"}";
								}
							}
							catch (NotFoundException e) { e.printStackTrace(); }
							for(Object annotation: assertions) {
								Assertion assertion = (Assertion) annotation;
								String transformedAssertion = null;
								try {
									if(Modifier.isStatic(fa.getField().getModifiers())) {
										String className = fa.getField().getDeclaringClass().getName();
										// replaces <field name> for <declaring class name>.<field name>
										transformedAssertion = assertion.value().replace(name, (className + "." + name).subSequence(0, (className + "." + name).length()));
									}
									else {
										// creates a temporary method in order for the assertion to be evaluated inside the scope of the instance containing the field
										createAssertionMethod(fa.getField().getDeclaringClass(), assertion.value());
										transformedAssertion = "$0._$isTrue$_()";
									}
								}
								catch(NotFoundException e) { e.printStackTrace(); }
								temp += "if(!(" + transformedAssertion + ")) {" +
										"	throw new RuntimeException(\"The assertion " + assertion.value() + " is false\");" +
										"}";
							}
							fa.replace(temp);
						}
					}
				}
			});
		}
	}

	/* Creates both initialization lists (static and non-static) in the topmost (in class hierarchy) class if they are not already created */
	private void createListsIfNeeded(CtClass ctClass) throws NotFoundException, CannotCompileException {
		CtClass temp = ctClass;
		while(!temp.getSuperclass().getName().equals("java.lang.Object")) {
			temp = temp.getSuperclass();
		}
		for(CtField ctField: temp.getFields()) {
			if(ctField.getName().equals("_$initialized$_") || ctField.getName().equals("_$initializedStatic$_")) {
				return;
			}
		}
		CtField ctField = CtField.make("java.util.List _$initialized$_ = new java.util.ArrayList();", temp);
		ctField.setModifiers(Modifier.PUBLIC);
		temp.addField(ctField);
		ctField = CtField.make("java.util.List _$initializedStatic$_ = new java.util.ArrayList();", temp);
		ctField.setModifiers(Modifier.PUBLIC);
		ctField.setModifiers(Modifier.STATIC);
		temp.addField(ctField);
	}

	/* Creates a temporary method in ctClass to return the result of evaluating assertion */
	private void createAssertionMethod(CtClass ctClass, String assertion) throws NotFoundException, CannotCompileException {
		for(CtMethod ctMethod: ctClass.getMethods()) {
			if(ctMethod.getName().equals("_$isTrue$_")) {
				ctClass.removeMethod(ctMethod);
				break;
			}
		}
		String src = "boolean _$isTrue$_() { return " + assertion + "; }";
		CtMethod ctMethod = CtNewMethod.make(src, ctClass);
		ctMethod.setModifiers(Modifier.PUBLIC);
		ctClass.addMethod(ctMethod);
	}

}