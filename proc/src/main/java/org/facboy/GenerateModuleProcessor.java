package org.facboy;

import static com.google.auto.common.AnnotationMirrors.getAnnotationValue;
import static com.google.auto.common.MoreTypes.asTypeElement;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import dagger.Binds;
import dagger.Module;

import com.google.auto.common.MoreElements;
import com.google.common.base.Throwables;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

public class GenerateModuleProcessor extends AbstractProcessor {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(GenerateModule.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Collection<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(GenerateModule.class);

        ElementFilter.typesIn(annotatedElements).forEach(this::generateClass);

        return false;
    }

    private void generateClass(TypeElement annotatedType) {
        try {
            TypeSpec.Builder moduleTypeSpec = createModuleTypeSpecBuilder(annotatedType)
                    .addModifiers(Modifier.ABSTRACT);

            AnnotationMirror annotation = MoreElements.getAnnotationMirror(annotatedType, GenerateModule.class)
                    .toJavaUtil()
                    .get();

            AnnotationValue binds = getAnnotationValue(annotation, "binds");
            TypeMirror bindsType = asTypeMirror(binds);

            AnnotationValue value = getAnnotationValue(annotation, "value");
            TypeMirror valueType = asTypeMirror(value);

            moduleTypeSpec.addMethod(MethodSpec.methodBuilder("binds" + asTypeElement(bindsType).getSimpleName())
                    .addAnnotation(Binds.class)
                    .addModifiers(Modifier.ABSTRACT)
                    .addParameter(TypeName.get(valueType), "value")
                    .returns(TypeName.get(bindsType))
                    .build());

            writeClass(annotatedType, moduleTypeSpec.build());
        } catch (RuntimeException e) {
            String trace = Throwables.getStackTraceAsString(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Error running " + getClass().getSimpleName() + ": " + trace, annotatedType);
            throw e;
        }
    }

    private TypeSpec.Builder createModuleTypeSpecBuilder(TypeElement annotatedType) {
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder("Gen_" + annotatedType.getSimpleName())
                .addAnnotation(ClassName.get(Module.class))
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", GenerateModuleProcessor.class.getName())
                        .build())
                .addOriginatingElement(annotatedType)
                .addJavadoc("Dagger module generated from {@link $T}.\n", annotatedType);

        if (annotatedType.getModifiers().contains(Modifier.PUBLIC)) {
            typeSpecBuilder.addModifiers(Modifier.PUBLIC);
        }

        return typeSpecBuilder;
    }

    private void writeClass(TypeElement annotatedType, TypeSpec moduleTypeSpec) {
        // write into package of annotated type
        String packageName = elementUtils().getPackageOf(annotatedType).getQualifiedName().toString();

        JavaFile.Builder sourceBuilder = JavaFile.builder(packageName, moduleTypeSpec);

        String source = sourceBuilder.build().toString();

        try {
            JavaFileObject javaFileObject = processingEnv.getFiler().createSourceFile(
                    packageName + "." + moduleTypeSpec.name, annotatedType);
            try (Writer writer = javaFileObject.openWriter()) {
                writer.write(source);
            }
        } catch (IOException e) {
            // From AutoValueProcessor

            // This should really be an error, but we make it a warning in the hope of resisting Eclipse
            // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=367599. If that bug manifests, we may get
            // invoked more than once for the same file, so ignoring the ability to overwrite it is the
            // right thing to do. If we are unable to write for some other reason, we should get a compile
            // error later because user code will have a reference to the code we were supposed to
            // generate (new AutoValue_Foo() or whatever) and that reference will be undefined.
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Could not write generated class " + moduleTypeSpec.name + ": " + e);
        }
    }

    private Elements elementUtils() {
        return processingEnv.getElementUtils();
    }

    private static TypeMirror asTypeMirror(AnnotationValue annotationValue) {
        return annotationValue.accept(new SimpleAnnotationValueVisitor6<TypeMirror, Void>() {
            @Override
            protected TypeMirror defaultAction(Object o, Void aVoid) {
                throw new IllegalArgumentException("value " + o + " is not a " + TypeMirror.class.getName());
            }

            @Override
            public TypeMirror visitArray(List<? extends AnnotationValue> vals, Void aVoid) {
                throw new IllegalArgumentException("value is an array, not a single value");
            }

            @Override
            public TypeMirror visitString(String s, Void aVoid) {
                if (s.equals("<error>")) {
                    throw new RuntimeException("abort");
                }
                return defaultAction(s, aVoid);
            }

            @Override
            public TypeMirror visitType(TypeMirror t, Void aVoid) {
                return t;
            }
        }, null);
    }
}
