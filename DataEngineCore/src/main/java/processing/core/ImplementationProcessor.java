package processing.core;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import data.core.AbstractDataEngine;
import data.core.EngineNature;
import data.core.Hidden;
import data.core.Implementation;

/**
 * Verifies if an implementation is set up perfectly depending on the para
 */
@SupportedAnnotationTypes("data.core.Implementation")
@SupportedSourceVersion(value = SourceVersion.RELEASE_16)
public class ImplementationProcessor extends AbstractProcessor {

    private Messager messager;
    private Types typeUtils;
    // Initialize core components to be used during processing
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        typeUtils = processingEnv.getTypeUtils();

    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        //Get all classes that are considered an implementation
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Implementation.class);

        //Loop over all the elements
        for(Element element:elements) {
            //Now check the type if the annotation is used correctly
            if(element.getKind().isClass()){

                //Checks if the implementation is "hidden"
                if(element.getAnnotation(Hidden.class) != null) return true;

                //We know it is a class, now check type
                switch(element.getAnnotation(Implementation.class).value()){
                    case IMPLEMENTATION -> {
                        //All implementations must be marked with EngineNature
                        if(element.getAnnotation(EngineNature.class) == null) 
                            //Making sure its marked.
                            injectError("An implementation must be marked with EngineNature " +
                                    ((TypeElement) element).getQualifiedName() + " has not been marked", element);

                        //We know it is labeled as an implementation
                        //So we check if it has an iterator and extends AbstractDataEngine
                        //Also it can't be abstract
                        if(element.getModifiers().contains(Modifier.ABSTRACT))
                            injectError("An implementation cannot be declared abstract. " +
                                    ((TypeElement)element).getQualifiedName()+ " is declared abstract", element);
                        else if(!hasEnclosedIterator((TypeElement) element))
                            injectError("An implementation must contain an iterator implementation. " +
                                    ((TypeElement)element).getQualifiedName() +" has no concrete iterator.", element);
                        else if(isNotSubClassOf((TypeElement) element))
                            injectError("An implementation must be a direct or indirect subclass of " +
                                    "AbstractDataEngine. " + ((TypeElement)element).getQualifiedName() + " does not have " +
                                    "AbstractDataEngine as a superclass ",element );

                    }
                    case ABSTRACTION -> {
                        //Here we only need to check abstraction and superclass
                        if(!element.getModifiers().contains(Modifier.ABSTRACT))
                            injectError("An abstraction must be declared abstract "
                                    + ((TypeElement)element).getQualifiedName() + " is not declared abstract", element);
                        else if(isNotSubClassOf((TypeElement) element))
                            injectError("An abstraction must be a direct or indirect subclass of " +
                                    "AbstractDataEngine "+ ((TypeElement)element).getQualifiedName() + " does not have " +
                                    "AbstractDataEngine as a superclass ",element );
                    }
                }
            }else injectError("Implementation must only be used on a class. Used on "+ element.getKind() ,element);
        }return true;
    }

    /**
     * Internal helper method, checks if it is a subclass of {@code AbstractDataEngine}
     */
    private boolean isNotSubClassOf(TypeElement element) {
        TypeMirror superClassType = ((TypeElement) typeUtils.asElement(element.asType())).getSuperclass();
        while (superClassType.getKind() != TypeKind.NONE){
            if(typeUtils.asElement(superClassType).toString().equals(AbstractDataEngine.class.getCanonicalName())){
                return false;
            }
            superClassType = ((TypeElement) typeUtils.asElement(superClassType)).getSuperclass();
        }

        return true;
    }

    /**
     * Internal helper method, checks if the class has an inner class with an implemented iterator
     */
    private boolean hasEnclosedIterator(TypeElement element) {
        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind().isClass() & !element.getModifiers().contains(Modifier.ABSTRACT)) {
                for (TypeMirror inf : ((TypeElement)enclosedElement).getInterfaces()) {
                    if (typeUtils.asElement(inf).toString().equals(Iterator.class.getCanonicalName()) ||
                    subInterface((TypeElement) typeUtils.asElement(inf)))
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Internal helper method, injects a message directly into compilation output.
     */
    private void injectError(String message, Element element) {
        messager.printMessage(Kind.ERROR, message, element);
    }

    private boolean subInterface(TypeElement element) {
        TypeMirror superInterface = element.getSuperclass();
        while(superInterface.getKind() != TypeKind.NONE){
            if(typeUtils.asElement(superInterface).toString().equals(Iterator.class.getCanonicalName())){
                return true;
            }
        }
        return false;
    }
}