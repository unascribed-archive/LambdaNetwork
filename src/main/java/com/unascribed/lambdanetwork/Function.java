package com.unascribed.lambdanetwork;

public interface Function<T, R> {
	R apply(T t);
}
