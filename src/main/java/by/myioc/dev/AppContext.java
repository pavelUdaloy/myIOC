package by.myioc.dev;

public interface AppContext {
    Object getComponent(String name);
    <T> T getComponent(String name, Class<T> token);
    Object[] getComponents();
}
