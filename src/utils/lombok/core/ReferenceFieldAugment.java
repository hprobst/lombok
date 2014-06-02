/*
 * Copyright (C) 2014 The Project Lombok Authors.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package lombok.core;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public abstract class ReferenceFieldAugment<T, F> {
	
	/**
	 * Augments a instance of a type with a reference field.
	 * <p>
	 * If the type already declares an instance field with the given name and field type, that field might be used. Otherwise the field will be augmented.
	 * <p>
	 * This code assumes that for any combination of {@code type} and {@code name} this method is only called once.
	 * Otherwise, whether state is shared is undefined.
	 * 
	 * @param type to augment
	 * @param fieldType type of the field
	 * @param name of the field
	 * @throws NullPointerException if {@code type}, {@code fieldType} or {@code name} is {@code null}
	 */
	public static <T, F> ReferenceFieldAugment<T, F> augment(Class<T> type, Class<? super F> fieldType, String name) {
		return new MapFieldAugment<T, F>();
	}
	
	/**
	 * Augments a instance of a type with a weak reference field.
	 * <p>
	 * If the type already declares an instance field with the given name and field type, that field might be used. Otherwise the field will be augmented.
	 * <p>
	 * This code assumes that for any combination of {@code type} and {@code name} this method is only called once.
	 * Otherwise, whether state is shared is undefined.
	 * 
	 * @param type to augment
	 * @param fieldType type of the field
	 * @param name of the field
	 * @throws NullPointerException if {@code type}, {@code fieldType} or {@code name} is {@code null}
	 */
	public static <T, F> ReferenceFieldAugment<T, F> augmentWeakField(Class<T> type, Class<? super F> fieldType, String name) {
		return new MapWeakFieldAugment<T, F>();
	}
	
	private ReferenceFieldAugment() {
		// prevent external instantiation
	}
	
	/**
	 * @throws NullPointerException if {@code object} is {@code null}
	 */
	public abstract F get(T object);
	
	/**
	 * @throws NullPointerException if {@code object} or {@code expected} is {@code null}
	 */
	public final void set(T object, F value) {
		getAndSet(object, value);
	}
	
	/**
	 * @return the value of the field <strong>before</strong> the operation.
	 * @throws NullPointerException if {@code object} or {@code expected} is {@code null}
	 */
	public abstract F getAndSet(T object, F value);
	
	/**
	 * @return the value of the field <strong>before</strong> the operation.
	 * @throws NullPointerException if {@code object} is {@code null}
	 */
	public abstract F clear(T object);
	
	/**
	 * @return the value of the field <strong>after</strong> the operation. If the value was equal to {@code expected} or already cleared {@code null}, otherwise the current value.
	 * @throws NullPointerException if {@code object} or {@code expected} is {@code null}
	 */
	public abstract F compareAndClear(T object, F expected);
	
	/**
	 * @return the value of the field <strong>after</strong> the operation.
	 * @throws NullPointerException if {@code object} or {@code value} is {@code null}
	 */
	public abstract F setIfAbsent(T object, F value);
	
	/**
	 * @return the value of the field <strong>after</strong> the operation.
	 * @throws NullPointerException if {@code object}, {@code expected} or {@code value} is {@code null}
	 */
	public abstract F compareAndSet(T object, F expected, F value);
	
	private static class MapFieldAugment<T, F> extends ReferenceFieldAugment<T, F> {
		final Map<T, Object> values = new WeakHashMap<T, Object>();
		
		@Override
		public F get(T object) {
			checkNotNull(object, "object");
			synchronized (values) {
				return read(object);
			}
		}
		
		@Override
		public F getAndSet(T object, F value) {
			checkNotNull(object, "object");
			checkNotNull(value, "value");
			synchronized (values) {
				F result = read(object);
				write(object, value);
				return result;
			}
		}
		
		@Override
		public F clear(T object) {
			checkNotNull(object, "object");
			synchronized (values) {
				F result = read(object);
				values.remove(object);
				return result;
			}
		}
		
		@Override
		public F compareAndClear(T object, F expected) {
			checkNotNull(object, "object");
			checkNotNull(expected, "expected");
			synchronized (values) {
				F result = read(object);
				if (result == null) {
					return null;
				}
				if (!expected.equals(result)) {
					return result;
				}
				values.remove(object);
				return null;
			}
		}
		
		@Override
		public F setIfAbsent(T object, F value) {
			checkNotNull(object, "object");
			checkNotNull(value, "value");
			synchronized (values) {
				F result = read(object);
				if (result != null) {
					return result;
				}
				write(object, value);
				return value;
			}
		}
		
		@Override
		public F compareAndSet(T object, F expected, F value) {
			checkNotNull(object, "object");
			checkNotNull(expected, "expected");
			checkNotNull(value, "value");
			synchronized (values) {
				F result = read(object);
				if (!expected.equals(result)) {
					return result;
				}
				write(object, value);
				return value;
			}
		}
		
		@SuppressWarnings("unchecked")
		F read(T object) {
			return (F)values.get(object);
		}
		
		void write(T object, F value) {
			values.put(object, value);
		}
	}
	
	static class MapWeakFieldAugment<T, F> extends MapFieldAugment<T, F> {
		
		@SuppressWarnings("unchecked")
		F read(T object) {
			WeakReference<F> read = (WeakReference<F>)values.get(object);
			if (read == null) return null;
			F result = read.get();
			if (result == null) values.remove(object);
			return result;
		}
		
		void write(T object, F value) {
			values.put(object, new WeakReference<F>(value));
		}
	}
	
	private static <T> T checkNotNull(T object, String name) {
		if (object == null) throw new NullPointerException(name);
		return object;
	}
}
