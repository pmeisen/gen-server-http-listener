package net.meisen.general.server.http.listener.servlets;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/***
 * A thread safe-storage to store data accross the different running scripts.
 * 
 * @author pmeisen
 * 
 */
public class ScriptedServletStorage {

	private Map<String, Object> storage = new ConcurrentHashMap<String, Object>();

	/**
	 * Puts an {@code object} into the storage and associates it tot he
	 * specified {@code key}.
	 * 
	 * @param key
	 *            the key of the {@code object}, if another {@code object}
	 *            exists already with the same key the object is overwritten
	 * @param object
	 *            the {@code object} to be stored, cannot be {@code null}
	 * 
	 * @return the old object associated to the key, i.e. it's {@code null} if
	 *         no object was associated
	 * 
	 * @throws NullPointerException
	 *             if the passed {@code object} is {@code null}.
	 */
	public Object put(final String key, final Object object)
			throws NullPointerException {
		if (object == null) {
			throw new NullPointerException("The object cannot be null.");
		}

		return storage.put(key, object);
	}

	/**
	 * The amount of objects stored in the storage.
	 * 
	 * @return amount of objects stored
	 */
	public int size() {
		return storage.size();
	}

	/**
	 * Checks if the storage is currently empty.
	 * 
	 * @return {@code true} if the storage is empty, otherwise {@code false}
	 */
	public boolean isEmpty() {
		return storage.isEmpty();
	}

	/**
	 * Retrieves the object from the storage, which is associated to the
	 * specified {@code key}.
	 * 
	 * @param key
	 *            the associated key to the object to be retrieved
	 * 
	 * @return the retrieved object, or {@code null} if the object cannot be
	 *         found
	 */
	public Object get(final String key) {
		return storage.get(key);
	}

	/**
	 * Removes an object from the storage.
	 * 
	 * @param key
	 *            the object to be removed
	 * 
	 * @return the removed object
	 */
	public Object remove(final String key) {
		return storage.remove(key);
	}

	/**
	 * Removes all the objects from the storage.
	 */
	public void removeAll() {
		storage.clear();
	}

	/**
	 * Synonym method for {@link #removeAll()}.
	 */
	public void clear() {
		removeAll();
	}

	/**
	 * Gets a {@code Set} of all the keys of the storage.
	 * 
	 * @return a {@code Set} of all the keys
	 */
	public Set<String> keys() {
		return storage.keySet();
	}

	/**
	 * Gets a {@code Collection} of all the objects within the storage
	 * 
	 * @return all the objects within the storage.
	 */
	public Collection<Object> objects() {
		return storage.values();
	}

	/**
	 * Synonym method for {@link #objects()}.
	 * 
	 * @return all the objects within the storage.
	 */
	public Collection<Object> values() {
		return objects();
	}
}
