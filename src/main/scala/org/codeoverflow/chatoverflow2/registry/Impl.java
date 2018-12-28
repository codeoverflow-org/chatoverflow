package org.codeoverflow.chatoverflow2.registry;

import org.codeoverflow.chatoverflow2.connector.Connector;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to clearly specify a input / output / requirement implementation.
 * The complete structure is generated at init time.
 * Note: You have to use this annotation explicitly to make sure,
 * that you want your input in the tree to be instantiated at runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Impl {
    /**
     * The input or output that is implemented here. For TwitchChatInputImpl that would be TwitchChatInput.
     */
    Class<?> impl();

    /**
     * The connector type of the requirement. This is optional and only needed for input & outputs.
     */
    Class<? extends Connector> connector() default Connector.class;
}
