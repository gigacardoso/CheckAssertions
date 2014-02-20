package ist.meic.pa;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

public abstract class Injector {

	public void setup(ClassPool pool) throws NotFoundException, CannotCompileException {};

	abstract void processClass(CtClass ctClass) throws NotFoundException, CannotCompileException;

	/* Swaps parameter meta-variable names for the corresponding backup variable names */
	protected String transformAssertion(String s, int nParams) {
		for(int i = 1; i < nParams + 1; ++i) {
			s = s.replace(("$" + i).subSequence(0, ("$" + i).length()), ("_p" + i + "$original_").subSequence(0, ("_p" + i + "$original_").length()));
		}
		return s;
	}

}