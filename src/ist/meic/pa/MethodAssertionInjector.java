package ist.meic.pa;

import java.util.List;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class MethodAssertionInjector extends HierarchicalInjector {

	public void processClass(CtClass ctClass) throws NotFoundException, CannotCompileException {
		try {
			for(CtMethod ctMethod: ctClass.getDeclaredMethods()) {
				List<Object> assertions = getAllAnnotations(ctClass, ctMethod.getName(), ctMethod.getSignature(), Assertion.class);
				if(!assertions.isEmpty()) {
					for(Object annotation: assertions) {
						Assertion assertion = (Assertion) annotation;
						CtClass[] types = ctMethod.getParameterTypes();
						/* creates a local variable for each parameter and stores the parameters' original value
						 * each local variable is called _p$Noriginal_ where N comes from $N (the corresponding meta-variable name)
						 */
						for(int i = 0; i < types.length; ++i) {
							ctMethod.addLocalVariable("_p" + ((int) (i + 1)) + "$original_", types[i]);
							ctMethod.insertBefore("_p" + ((int) (i + 1)) + "$original_ = $" + ((int) (i + 1)) + ";");
						}
						String transformedAssertion = transformAssertion(assertion.value(), types.length);
						ctMethod.insertAfter("if(!(" + transformedAssertion + ")) throw new RuntimeException(\"Assertion '" + assertion.value() + "' failed!\");");
					}
				}
			}
		}
		catch(ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}