package ist.meic.pa;

import java.util.List;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public class PreConditionInjector extends HierarchicalInjector {

	public void processClass(CtClass ctClass) throws NotFoundException, CannotCompileException {
		try {
			for(CtMethod ctMethod: ctClass.getDeclaredMethods()) {
				List<Object> preConditions = getAllAnnotations(ctClass, ctMethod.getName(), ctMethod.getSignature(), PreCondition.class);
				if(!preConditions.isEmpty()) {
					for(Object annotation: preConditions) {
						PreCondition preCondition = (PreCondition) annotation;
						ctMethod.insertBefore("if(!(" + preCondition.value() + ")) throw new RuntimeException(\"Pre-Condition '" + preCondition.value() + "' failed!\");");
					}
				}
			}
		}
		catch(ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}