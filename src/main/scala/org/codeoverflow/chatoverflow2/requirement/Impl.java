package org.codeoverflow.chatoverflow2.requirement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to clearly specify a input or output implementation.
 * The complete tree is generated at init time.
 * Note: You have to use this annotation explicitly to make sure,
 * that you want your input in the tree to be instantiated at runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Impl {
    /**
     * The input or output that is implemented here. For TwitchChatInputImpl that would be TwitchChatInput.
     */
    Class<?> value();
}
