package org.facboy;

import javax.inject.Singleton;

import dagger.Component;

@Component(modules = MyModule.class)
@Singleton
public interface MyComponent {
    SomeBean someBean();
}
