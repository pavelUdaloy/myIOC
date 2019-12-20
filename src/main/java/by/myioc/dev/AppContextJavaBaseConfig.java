package by.myioc.dev;

import by.myioc.dev.annotation.Prototype;

import java.util.Map;
import java.util.Objects;

public class AppContextJavaBaseConfig implements AppContext {
    private Map<String, Object> cont;



    public AppContextJavaBaseConfig(Class configClass) {
        cont = new ComponentFactory(configClass).getCont();
    }

    @Override
    public Object getComponent(String name) {
        try {
            if (cont.get(name + Objects.hash(name)).getClass().isAnnotationPresent(Prototype.class)) {
                Object o = cont.get(name + Objects.hash(name)).getClass().newInstance();
                cont.put(o.getClass().getSimpleName() + Objects.hash(o.getClass().getSimpleName()), o);
                return o;
            } else return cont.get(name + Objects.hash(name));
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <T> T getComponent(String name, Class<T> token) {
        try {
            if (cont.get(name + Objects.hash(name)).getClass().isAnnotationPresent(Prototype.class)) {
                Object o = cont.get(name + Objects.hash(name)).getClass().newInstance();//todo изменить ключи в мапах(либо доб.хэш либо 1/2)
                cont.put(o.getClass().getSimpleName() + Objects.hash(o.getClass().getSimpleName()), o);
                return token.cast(o);
            } else return token.cast(cont.get(name + Objects.hash(name)));
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object[] getComponents() {
        return cont.entrySet().toArray().clone();
    }
}
