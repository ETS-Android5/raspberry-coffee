package restserver.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD) // Method level
public @interface OperationDefinition {
	enum Verbs {
		GET, POST, PUT, DELETE
	}
	Verbs verb() default Verbs.GET;
	String path() default "";
	boolean absolutePath() default false;
	String description() default "";
}
