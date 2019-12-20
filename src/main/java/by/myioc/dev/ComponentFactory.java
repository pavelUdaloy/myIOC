package by.myioc.dev;

import by.myioc.dev.annotation.*;
import com.sun.jmx.snmp.SnmpUnknownAccContrModelException;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class ComponentFactory {

    private Class configClass;
    private Map<String, Object> cont = new HashMap<>();

    public ComponentFactory(Class configClass) {
        this.configClass = configClass;
        readDef();
    }

    public Map<String, Object> getCont() {
        return cont;
    }

    private void readDef() {
        String componentsLocation = "";
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            componentsLocation = ((ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class)).basePackage();
        }
        Reflections reflections = new Reflections(componentsLocation);
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(Component.class);
        for (Class<?> aClass : typesAnnotatedWith) {
            if (aClass.getConstructors().length != 1) {
                throw new RuntimeException();
            }
            for (Constructor<?> constructor : aClass.getConstructors()) {
                int count = 0;
                Parameter[] parameters = constructor.getParameters();
                Object[] objcs = new Object[parameters.length];
                for (Parameter parameter : parameters) {
                    if (parameter.getAnnotation(Qualifier.class) != null) {
                        Object obj = cont.get(parameter.getAnnotation(Qualifier.class).name() + Objects.hash(parameter.getAnnotation(Qualifier.class).name()));
                        objcs[count++] = obj;
                    } else if (parameter.getAnnotation(Value.class) != null) {
                        String value = parameter.getAnnotation(Value.class).value();
                        objcs[count++] = value;
                    } else {
                        for (Object obj : cont.entrySet().toArray()) {
                            if (parameter.getType().getSimpleName().equals(obj.getClass().getSimpleName())) {
                                objcs[count++] = obj;
                                break;
                            }
                        }
                    }
                }
                addToContainer(constructor, objcs, aClass);
            }
        }
        Object configClassObj = null;
        try {
            configClassObj = configClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        Queue<Method> queueMethods = addMethodsWithCREATEtoQueue(configClassObj);
        for (Method method : queueMethods) {
            int count = 0;
            Parameter[] parameters = method.getParameters();
            Object[] objcs = new Object[parameters.length];
            for (Parameter parameter : parameters) {
                if (parameter.getAnnotation(Qualifier.class) != null) {
                    Object o = cont.get(parameter.getAnnotation(Qualifier.class).name()+ Objects.hash(parameter.getAnnotation(Qualifier.class).name()));
                    objcs[count++] = o;
                } else if (parameter.getAnnotation(Value.class) != null) {
                    String value = parameter.getAnnotation(Value.class).value();
                    objcs[count++] = value;
                } else {
                    for (Object o : cont.entrySet()) {
                        Map.Entry<String, Object> o1 = (Map.Entry<String, Object>) o;
                        if (parameter.getType().getSimpleName().equals(cont.get(o1.getKey()).getClass().getSimpleName())) {
                            objcs[count++] = o1.getValue();
                            break;
                        }
                    }
                }
            }
            addToContainer(configClassObj, method, objcs);
        }
    }

    private void addToContainer(Constructor<?> constructor, Object[] objcs, Class clazz) {
        try {
            Object o = constructor.newInstance(objcs);
            String name = clazz.getSimpleName().toLowerCase();
            System.out.println("Create " + name + " " + o);
            cont.put(name + Objects.hash(name), o);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void addToContainer(Object configClassObj, Method method, Object[] objcs) {
        try {
            Object invoke = method.invoke(configClassObj, objcs);
            String name = method.getName();
            System.out.println("Create " + name + " " + invoke);
            cont.put(name + Objects.hash(name), invoke);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private Queue<Method> addMethodsWithCREATEtoQueue(Object configClassObj) {
        Method[] methods = configClass.getMethods();
        Queue<Method> queue = new LinkedList<>();
        for (Method method : methods) {
            if (method.isAnnotationPresent(Create.class)) {
                try {
                    if (method.getParameterCount() == 0) {
                        String objName = method.getName();
                        Object newObj = method.invoke(configClassObj);
                        cont.put(objName + Objects.hash(objName), newObj);
                    } else {
                        queue.add(method);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return queue;
    }
}
