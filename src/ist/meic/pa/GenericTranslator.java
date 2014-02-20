package ist.meic.pa;

import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.Translator;

public class GenericTranslator implements Translator {

	private List<Injector> _injectors = new ArrayList<Injector> ();

	public void start(ClassPool pool)throws NotFoundException, CannotCompileException {
		_injectors.add(new ConstructorInjector());
		_injectors.add(new FieldAssertionInjector());
		_injectors.add(new MethodAssertionInjector());
		_injectors.add(new PreConditionInjector());
		_injectors.add(new PostConditionInjector());
		for(Injector injector: _injectors) {
			injector.setup(pool);
		}
	}

	public void onLoad(ClassPool pool, String className) throws NotFoundException, CannotCompileException {
		CtClass ctClass = pool.get(className);
		for(Injector injector: _injectors) {
			injector.processClass(ctClass);
		}
	}

}