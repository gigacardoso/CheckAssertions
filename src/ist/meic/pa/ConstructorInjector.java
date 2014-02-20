package ist.meic.pa;

import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;

public class ConstructorInjector extends Injector {

	void processClass(CtClass ctClass) throws NotFoundException, CannotCompileException {
		try {
			for(CtConstructor ctConstructor: ctClass.getConstructors()) {
				Object[] annotations = ctConstructor.getAnnotations();
				List<Object> assertions = new ArrayList<Object>();
				for(Object annotation: annotations) {
					if(annotation instanceof Assertion) {
						assertions.add(annotation);
					}
				}
				if(!assertions.isEmpty()) {
					for(Object annotation: assertions) {
						Assertion assertion = (Assertion) annotation;
						ctConstructor.insertBefore("if(!(" + assertion.value() + ")) throw new RuntimeException(\"Assertion '" + assertion.value() + "' failed!\");");
					}
				}
			}
		}
		catch(ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

}