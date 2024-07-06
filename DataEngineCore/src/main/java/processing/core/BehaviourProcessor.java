package processing.core;

import data.core.Behaviour;
import data.core.Implementation;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * Very simple processor for checking valid use of {@code Behaviour}on methods.
 */
@SupportedAnnotationTypes("data.core.Behaviour")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class BehaviourProcessor extends AbstractProcessor {

    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Behaviour.class);

        for (Element element : elements) {
            if(element.getKind().equals(ElementKind.METHOD)){
                if(element.getModifiers().contains(Modifier.ABSTRACT)){
                    injectMessage("A method marked with Behaviour cannot be declared abstract",element);
                }

                if(element.getModifiers().contains(Modifier.FINAL) | element.getModifiers().contains(Modifier.NATIVE)){
                    injectMessage("A method marked with Behaviour cannot be final or have a native implementation", element);
                }

            }else injectMessage("Behaviour can only be used on a method", element);
        }
        return true;
    }

    /**
     * Internal helper method, injects a message directly into compilation output.
     */
    private void injectMessage(String message, Element element) {
        messager.printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}




