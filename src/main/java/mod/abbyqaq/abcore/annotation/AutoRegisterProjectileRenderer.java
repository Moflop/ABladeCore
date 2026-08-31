package mod.abbyqaq.abcore.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于自动注册抛射物渲染器的注解
 *
 * @author Arcomit
 * @since 2026-08-31
 */
@Target(ElementType.TYPE) // 只能打在类上
@Retention(RetentionPolicy.RUNTIME) // 运行时保留
public @interface AutoRegisterProjectileRenderer {
	String type();
}
