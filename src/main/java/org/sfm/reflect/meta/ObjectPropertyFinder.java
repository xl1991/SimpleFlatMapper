package org.sfm.reflect.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.sfm.map.MapperBuildingException;
import org.sfm.reflect.asm.ConstructorDefinition;
import org.sfm.reflect.asm.ConstructorParameter;

final class ObjectPropertyFinder<T> implements PropertyFinder<T> {
	
	private final List<ConstructorDefinition<T>> eligibleConstructorDefinitions;
	private final ObjectClassMeta<T> classMeta;
	private final Map<String, PropertyFinder<?>> subPropertyFinders = new HashMap<>();

	ObjectPropertyFinder(ObjectClassMeta<T> classMeta) throws MapperBuildingException {
		this.classMeta = classMeta;
		this.eligibleConstructorDefinitions = classMeta.getConstructorDefinitions() != null ? new ArrayList<>(classMeta.getConstructorDefinitions()) : null;
	}

	/* (non-Javadoc)
	 * @see org.sfm.reflect.PropertyFinder#findProperty(org.sfm.utils.PropertyNameMatcher)
	 */
	@Override
	public PropertyMeta<T, ?> findProperty(final PropertyNameMatcher propertyNameMatcher) {
		PropertyMeta<T, ?> prop = null; 
		
		
		// check for constructor
		prop = lookForConstructor(propertyNameMatcher);

		if (prop == null) {
			prop = lookForProperty(propertyNameMatcher);
			
			if (prop == null) {
				prop = lookForSubPropertyInConstructors(propertyNameMatcher);
				
				if (prop == null) {
					prop = lookForSubProperty(propertyNameMatcher);
					
				} else {
					ConstructorPropertyMeta<T, ?> constructorProperty = (ConstructorPropertyMeta<T, ?>) ((SubPropertyMeta<T, ?>)prop).getProperty();
					removeNonMatching(constructorProperty.getConstructorParameter());
				}
			}
			
		} else {
			ConstructorPropertyMeta<T, ?> constructorProperty = (ConstructorPropertyMeta<T, ?>) prop;
			removeNonMatching(constructorProperty.getConstructorParameter());
		}

		return prop;
	}
	
	private ConstructorPropertyMeta<T, ?> lookForConstructor(final PropertyNameMatcher propertyNameMatcher) {
		if (classMeta.getConstructorProperties() != null) {
			for (ConstructorPropertyMeta<T, ?> prop : classMeta.getConstructorProperties()) {
				if (propertyNameMatcher.matches(prop.getName())
						&& hasConstructorMatching(prop.getConstructorParameter())) {
					return prop;
				}
			}
		}
		
		return null;
	}

	private PropertyMeta<T, ?> lookForProperty(final PropertyNameMatcher propertyNameMatcher) {
		for (PropertyMeta<T, ?> prop : classMeta.getProperties()) {
			if (propertyNameMatcher.matches(prop.getName())) {
				return prop;
			}
		}
		return null;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private PropertyMeta<T, ?> lookForSubPropertyInConstructors(final PropertyNameMatcher propertyNameMatcher) {
		if (classMeta.getConstructorProperties() != null) {
			for (ConstructorPropertyMeta<T, ?> prop : classMeta.getConstructorProperties()) {
				PropertyNameMatcher subPropMatcher = propertyNameMatcher.partialMatch(prop.getName());
				if (subPropMatcher != null && hasConstructorMatching(prop.getConstructorParameter())) {
					PropertyMeta<?, ?> subProp = lookForSubProperty(subPropMatcher, prop);
					if (subProp != null) {
						return new SubPropertyMeta(classMeta.getReflectionService(), prop, subProp);
					}
				}
			}
		}
		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private PropertyMeta<T, ?> lookForSubProperty(final PropertyNameMatcher propertyNameMatcher) {
		for (PropertyMeta<T, ?> prop : classMeta.getProperties()) {
			PropertyNameMatcher subPropMatcher = propertyNameMatcher.partialMatch(prop.getName());
			if (subPropMatcher != null) {
				PropertyMeta<?, ?> subProp =  lookForSubProperty(subPropMatcher, prop);
				if (subProp != null) {
					return new SubPropertyMeta(classMeta.getReflectionService(), prop, subProp);
				}
			}
		}

		return null;
	}
	
	private PropertyMeta<?, ?> lookForSubProperty(
			final PropertyNameMatcher propertyNameMatcher,
			final PropertyMeta<T, ?> prop) {
		PropertyFinder<?> subPropertyFinder = subPropertyFinders.get(prop.getName());
		if (subPropertyFinder == null) {
			subPropertyFinder = prop.getClassMeta().newPropertyFinder();
			subPropertyFinders.put(prop.getName(), subPropertyFinder);
		}

		PropertyMeta<?, ?> subProp = subPropertyFinder.findProperty(propertyNameMatcher);
		return subProp;
	}

	private void removeNonMatching(ConstructorParameter param) {
		ListIterator<ConstructorDefinition<T>> li = eligibleConstructorDefinitions.listIterator();
		while(li.hasNext()){
			ConstructorDefinition<T> cd = li.next();
			if (!cd.hasParam(param)) {
				li.remove();
			}
		}
	}
	
	private boolean hasConstructorMatching(ConstructorParameter param) {
		ListIterator<ConstructorDefinition<T>> li = eligibleConstructorDefinitions.listIterator();
		while(li.hasNext()){
			ConstructorDefinition<T> cd = li.next();
			if (cd.hasParam(param)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public PropertyMeta<T, ?> findProperty(String propertyName) {
		return findProperty(new PropertyNameMatcher(propertyName));
	}

	@Override
	public List<ConstructorDefinition<T>> getEligibleConstructorDefinitions() {
		return eligibleConstructorDefinitions;
	}
}