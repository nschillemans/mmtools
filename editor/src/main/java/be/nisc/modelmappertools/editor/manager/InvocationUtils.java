package be.nisc.modelmappertools.editor.manager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class InvocationUtils {

    private Map<Class, Map<String, List<Method>>> methods = new HashMap<>();
    private ClassLoader classLoader;

    public InvocationUtils(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public <T> T instantiate(String qualifiedName) {
        try {
            return (T) classLoader.loadClass(qualifiedName).getConstructors()[0].newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Class getInvocationTargetType(Object target) {
        if (target.getClass().getInterfaces().length > 0) {
            return target.getClass().getInterfaces()[0];
        } else {
            return target.getClass();
        }
    }

    public <T> T invoke(Object target, Class targetType, String name, Object... params) {
        if (!methods.containsKey(targetType)) {
            Map<String, List<Method>> classMethods = new HashMap<>();
            methods.put(targetType, classMethods);

            for (Method method : targetType.getMethods()) {
                if (!classMethods.containsKey(method.getName())) {
                    classMethods.put(method.getName(), new ArrayList<>());
                }

                classMethods.get(method.getName()).add(method);
            }
        }

        for (Method possibleMethod : methods.get(targetType).get(name)) {
            try {
                return (T) possibleMethod.invoke(target, params);
            } catch (IllegalArgumentException e) {
                // Probably wrong overloaded method; ignore
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException("Could not find proper method to invoke");
    }

    public <T> T invoke(Object target, String name, Object... params) {
        Class targetType = getInvocationTargetType(target);
        return invoke(target, targetType, name, params);
    }
}
