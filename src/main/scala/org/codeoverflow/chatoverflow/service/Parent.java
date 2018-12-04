package org.codeoverflow.chatoverflow.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to clearly specify a parent input or output.
 * The complete tree is generated at init time.
 * Note: You have to use this annotation explicitly to make sure,
 * that you want your input in the tree to be instantiated at runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Parent {
    /**
     * The parent class or interface. E.g. for a TwitchChatInput, that would probably be ChatInput.
     */
    Class<?> value();
}
