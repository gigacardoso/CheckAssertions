package ist.meic.pa;

import java.util.List;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class PostConditionInjector extends HierarchicalInjector {

	public void processClass(CtClass ctClass) throws NotFoundException, CannotCompileException {
		try {
			for(CtMethod ctMethod: ctClass.getDeclaredMethods()) {
				List<Object> postConditions = getAllAnnotations(ctClass, ctMethod.getName(), ctMethod.getSignature(), PostCondition.class);
				if(!postConditions.isEmpty()) {
					for(Object annotation: postConditions) {
						PostCondition postCondition = (PostCondition) annotation;
						CtClass[] types = ctMethod.getParameterTypes();
						/* Creates a local variable for each parameter and stores the parameters' original value
						 * Each local variable is called _p$Noriginal_ where N comes from $N (the corresponding meta-variable name)
						 */
						for(int i = 0; i < types.length; ++i) {
							ctMethod.addLocalVariable("_p" + ((int) (i + 1)) + "$original_", types[i]);
							ctMethod.insertBefore("_p" + ((int) (i + 1)) + "$original_ = $" + ((int) (i + 1)) + ";");
						}
						String transformedPostCondition = transformAssertion(postCondition.value(), types.length);
						ctMethod.insertAfter("if(!(" + transformedPostCondition + ")) throw new RuntimeException(\"Post-Condition '" + postCondition.value() + "' failed!\");");
					}
				}
			}
		}
		catch(ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}