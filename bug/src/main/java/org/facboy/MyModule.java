package org.facboy;

import dagger.Module;


@Module(includes = Gen_Module2.class)
abstract class MyModule {
    @GenerateModule(binds = SomeBean.class, value = MyBean.class)
    static abstract class Module2 {
    }
}
