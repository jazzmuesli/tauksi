package org.pavelreich.saaremaa.testdepan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectCreationContainer  {


	private final ConcurrentHashMap<ObjectCreator, Collection<ObjectCreationOccurence>> objectsCreated = new ConcurrentHashMap();
	public void put(ObjectCreator objectCreator, ObjectCreationOccurence objectCreationOccurence) {
		if (!objectsCreated.containsKey(objectCreator)) {
			objectsCreated.put(objectCreator, new ArrayList());
		}
		objectsCreated.get(objectCreator).add(objectCreationOccurence);
	}
	public Collection<ObjectCreationOccurence> get(ObjectCreator key) {
		Collection<ObjectCreationOccurence> ret = objectsCreated.get(key);
		if (ret == null) {
			return Collections.emptyList();
		}
		return ret;
	}

}
