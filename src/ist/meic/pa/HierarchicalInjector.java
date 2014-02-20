package ist.meic.pa;

import java.util.ArrayList;
import java.util.List;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

public abstract class HierarchicalInjector extends Injector {

	/* Returns all annotations of type annotationType of the specified method and all methods overridden by that method */
	protected List<Object> getAllAnnotations(CtClass ctClass, String ctMethodName, String ctMethodSignature, Class<?> annotationType) throws ClassNotFoundException, NotFoundException {
		CtClass ctSuperClass = ctClass.getSuperclass();
		if(ctSuperClass.getName().equals("java.lang.Object")) {
			List<Object> matchedAnnotations = new ArrayList<Object>();
			CtMethod ctMethod;
			try {
				ctMethod = ctClass.getMethod(ctMethodName, ctMethodSignature);
			}
			catch(NotFoundException e) {
				return matchedAnnotations;
			}
			Object[] annotations = ctMethod.getAnnotations();
			for(Object annotation: annotations) {
				if(annotationType.isInstance(annotation)) {
					matchedAnnotations.add(annotation);
				}
			}
			return matchedAnnotations;
		}
		List<Object> matchedAnnotations = new ArrayList<Object>();
		CtMethod ctMethod;
		try {
			ctMethod = ctClass.getMethod(ctMethodName, ctMethodSignature);
		}
		catch(NotFoundException e) {
			return matchedAnnotations;
		}
		Object[] annotations = ctMethod.getAnnotations();
		for(Object annotation: annotations) {
			if(annotationType.isInstance(annotation)) {
				matchedAnnotations.add(annotation);
			}
		}
		List<Object> superAssertions = getAllAnnotations(ctSuperClass, ctMethodName, ctMethodSignature, annotationType);
		superAssertions.addAll(matchedAnnotations);
		return superAssertions;
	}

}