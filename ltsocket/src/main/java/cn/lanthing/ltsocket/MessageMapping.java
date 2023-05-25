package cn.lanthing.ltsocket;

import cn.lanthing.ltproto.LtProto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MessageMapping {
    LtProto proto();
}
