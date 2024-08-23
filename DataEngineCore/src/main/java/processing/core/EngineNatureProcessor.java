package processing.core;

import com.sun.source.tree.*;
import data.constants.ImplementationType;
import data.constants.Nature;
import data.constants.Type;
import data.core.Behaviour;
import data.core.EngineNature;
import data.core.Implementation;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.sun.source.util.Trees;

/**
 * The most important part of compile-time processing. This processor checks if the {@code DataEngine} is
 * actually valid, and if valid and {@code IMMUTABLE} then generates overrides to mutable methods if any, present
 * in its superclass, all the way to {@code AbstractDataEngine}. This reduces the extra boilerplate code needed
 * to be handwritten for simply throwing an {@code UnsupportedOperationException}. The entire system
 * has been developed for classes that belong to {@code Data-Engine-Framework} and it is not recommended to
 * use these annotations in any other non-data-engine-framework code.
 *
 * @author devsw
 */
@SupportedAnnotationTypes("data.core.EngineNature")
@SupportedSourceVersion(SourceVersion.RELEASE_16)
public class EngineNatureProcessor extends AbstractProcessor {

    private Messager messager;
    private Types typeUtils;
    private Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        typeUtils = processingEnv.getTypeUtils();
        trees = Trees.instance(jbUnwrap(ProcessingEnvironment.class, processingEnv));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(EngineNature.class);

        for (Element element : elements) {
            if(element.getKind().isClass()){
                if(element.getAnnotation(Implementation.class) == null)
                    injectError("@EngineNature annotation must be used on classes " +
                            "which are annotated with @Implementation", element);
                if(element.getModifiers().contains(Modifier.ABSTRACT))
                    injectError("EngineNature cannot be applied on abstract classes" ,element);

                switch (element.getAnnotation(EngineNature.class).nature()){
                    case IMMUTABLE -> {
                        if(typeUtils.asElement(((TypeElement) typeUtils.asElement(element.asType())).getSuperclass())
                                .getModifiers().contains(Modifier.ABSTRACT)){
                            injectError("An IMMUTABLE implementation cannot have an abstract superclass ", element);
                        }
                        try {
                            if (isValid(element)) {
                                //Making code checks
                                beginProcessing((TypeElement) element);
                            }
                        }catch (Exception exec){
                            injectError("Error occurred" , element);
                        }
                    }
                    case MUTABLE, THREAD_MUTABLE ->{} //Nothing to do here actually!
                }
            }else {
                injectError("EngineNature can only be applied on a class",element);;
            }
        }
        return true;
    }

    /**
     * Internal helper method
     */
    private void beginProcessing(TypeElement element) {
        //First we need to climb up the hierarchy
        LinkedHashSet<TypeMirror> hierarchy = new LinkedHashSet<>();
        TypeMirror superClassMirror = ((TypeElement) typeUtils.asElement(element.asType())).getSuperclass();

        //Climbing up the hierarchy
        while (superClassMirror.getKind() != TypeKind.NONE){
            hierarchy.add(superClassMirror);
            superClassMirror = ((TypeElement) typeUtils.asElement(superClassMirror)).getSuperclass();
        }

        //We have an active hierarchy, now get all the methods that actually need to be overridden.
        LinkedHashSet<ExecutableElement> override = new LinkedHashSet<>();
        for (TypeMirror mirror : hierarchy){
            for(Element method : typeUtils.asElement(mirror).getEnclosedElements()) {
                if (method.getKind() == ElementKind.METHOD) {
                    if(method.getAnnotation(Behaviour.class) == null) continue;
                    else if(method.getAnnotation(Behaviour.class).value() == Type.MUTABLE)
                        override.add((ExecutableElement) method);
                }
            }
        }

        Stream<? extends Element> elementStream;

        int absence = 0;
        //Now checking presence
        for(ExecutableElement method : override){
            elementStream = element.getEnclosedElements().stream();
            elementStream = elementStream.filter((meth) -> meth.getKind() == ElementKind.METHOD);

            if(elementStream.noneMatch((meth) -> meth.toString().equals(method.toString()))) {
                injectError("No concrete implementation of " + method.toString() + " exists in" +
                        " this IMMUTABLE implementation", element);
                absence++;
            }
        }

        if(absence > 0) return;

        //Now checking individual implementations, we are sure that a method will be hit
        for(ExecutableElement method : override){
            elementStream = element.getEnclosedElements().stream();
            elementStream = elementStream.filter((meth) -> meth.getKind() == ElementKind.METHOD);

            elementStream.anyMatch((meth) -> {
                if (meth.toString().equals(method.toString())) {
                    //Check params and modifiers
                    MethodTree methTree = (MethodTree) trees.getTree(meth);
                    MethodTree methodTree = trees.getTree(method);

                    BlockTree body = methTree.getBody();
                    List<? extends StatementTree> statements = body.getStatements();
                    //We must be sure that only one statement is present
                    if (statements.size() > 1) {
                        injectError("Only one statement must be present inside" +
                                " the body for the IMMUTABLE implementation: " + meth, meth);
                        return false;
                    }

                    //Obviously one statement is present
                    if (!(statements.get(0) instanceof ThrowTree)) {
                        injectError("The single statement present must be a throw statement. " +
                                "Found " + statements.get(0) + " inside " + meth, meth);
                        return false;
                    }
                }
                return true;
            });
        }
    }

    /**
     * Internal helper method, verifies if the implementation is valid for further checking
     */
    private boolean isValid(Element element) {
        if(element.getAnnotation(EngineNature.class).nature() == Nature.IMMUTABLE){
            //Now need more validation
            TypeMirror superClass = ((TypeElement) typeUtils.asElement(element.asType())).getSuperclass();
            //Should be a MUTABLE implementation
            //We know it's a valid super class
            if(typeUtils.asElement(superClass).getAnnotation(Implementation.class).value() == ImplementationType.ABSTRACTION) {
                //If not an implementation
                injectError("An IMMUTABLE implementation must extend an " +
                        " implementation declared IMPLEMENTATION " +
                        typeUtils.asElement(superClass).toString() + " is abstract", element);
            }else if(typeUtils.asElement(superClass).getAnnotation(EngineNature.class).nature() != Nature.MUTABLE &&
                        typeUtils.asElement(superClass).getAnnotation(EngineNature.class).nature() != Nature.THREAD_MUTABLE){
                injectError("An IMMUTABLE implementation must extend an " +
                        " implementation declared MUTABLE or THREAD_MUTABLE " +
                        typeUtils.asElement(superClass).toString() + " is not MUTABLE", element);
            }
            return true;
        }else return false;
    }


    /**
     * Internal helper method, injects a message directly into compilation output.
     */
    private void injectError(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
    
    private void injectWarning(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, message, element);
    }

    private <T> T jbUnwrap(Class<? extends T> iface, T wrapper) {
        T unwrapped = null;
        try {
            final Class<?> apiWrappers = wrapper.getClass().getClassLoader().loadClass("org.jetbrains.jps.javac.APIWrappers");
            final Method unwrapMethod = apiWrappers.getDeclaredMethod("unwrap", Class.class, Object.class);
            unwrapped = iface.cast(unwrapMethod.invoke(null, iface, wrapper));
        }
        catch (Throwable ignored) {}
        return unwrapped != null? unwrapped : wrapper;
    }
}
