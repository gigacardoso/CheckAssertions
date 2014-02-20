package ist.meic.pa;

import javassist.ClassPool;
import javassist.Loader;
import javassist.Translator;

public class CheckAssertions {

	public static void main(String[] args) throws Throwable {
		if(args.length < 1) {
			System.err.println("Usage: java CheckAssertions <program> <arguments>");
			System.exit(1);
		}
		else {
			ClassPool pool = ClassPool.getDefault();
			Loader classLoader = new Loader();
			Translator genericTranslator = new GenericTranslator();
			classLoader.addTranslator(pool, genericTranslator);
			String[] restArgs = new String[args.length - 1];
			System.arraycopy(args, 1, restArgs, 0, restArgs.length);
			classLoader.run(args[0], restArgs);
		}
	}
}