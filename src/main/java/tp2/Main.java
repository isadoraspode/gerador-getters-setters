package tp2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.printer.YamlPrinter;

public class Main {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Uso: informe o caminho de um arquivo .java");
            return;
        }

        Path entrada = Path.of(args[0]);
        CompilationUnit arvore = StaticJavaParser.parse(entrada);

        if (args.length > 1 && args[1].equals("--arvore")) {
            System.out.println(new YamlPrinter(true).output(arvore));
            return;
        }

        boolean comToString = args.length > 1 && args[1].equals("--tostring");

        for (ClassOrInterfaceDeclaration classe : arvore.findAll(ClassOrInterfaceDeclaration.class)) {
            System.out.println("Classe: " + classe.getNameAsString());

            List<String> nomesAtributos = new ArrayList<>();

            for (FieldDeclaration campo : classe.getFields()) {
                for (VariableDeclarator variavel : campo.getVariables()) {
                    String nome = variavel.getNameAsString();
                    if (campo.isStatic()) {
                        System.out.println("  Ignorado (static): " + nome);
                        continue;
                    }
                    nomesAtributos.add(nome);

                    boolean ehBoolean = variavel.getType().equals(PrimitiveType.booleanType());
                    String prefixo = ehBoolean ? "is" : "get";
                    String nomeGetter = prefixo + capitalizar(nome);
                    String nomeSetter = "set" + capitalizar(nome);

                    if (!existeMetodo(classe, nomeGetter, 0)) {
                        gerarGetter(classe, variavel, nomeGetter);
                        System.out.println("  Gerado: " + nomeGetter);
                    }

                    if (campo.isFinal()) {
                        System.out.println("  Sem setter (final): " + nome);
                    } else if (!existeMetodo(classe, nomeSetter, 1)) {
                        gerarSetter(classe, variavel, nomeSetter);
                        System.out.println("  Gerado: " + nomeSetter);
                    }
                }
            }

            if (comToString && !existeMetodo(classe, "toString", 0)) {
                gerarToString(classe, nomesAtributos);
                System.out.println("  Gerado: toString");
            }
        }

        Path pastaSaida = Path.of("saida");
        Files.createDirectories(pastaSaida);
        Path arquivoSaida = pastaSaida.resolve(entrada.getFileName());
        Files.writeString(arquivoSaida, arvore.toString());
        System.out.println("Arquivo gerado: " + arquivoSaida);
    }

    private static void gerarGetter(ClassOrInterfaceDeclaration classe, VariableDeclarator variavel, String nomeGetter) {
        String nome = variavel.getNameAsString();

        MethodDeclaration metodo = classe.addMethod(nomeGetter, Modifier.Keyword.PUBLIC);

        metodo.setType(variavel.getType().clone());

        metodo.createBody().addStatement("return " + nome + ";");
    }

    private static void gerarSetter(ClassOrInterfaceDeclaration classe, VariableDeclarator variavel, String nomeSetter) {
        String nome = variavel.getNameAsString();

        MethodDeclaration metodo = classe.addMethod(nomeSetter, Modifier.Keyword.PUBLIC);

        metodo.addParameter(variavel.getType().clone(), nome);
        metodo.createBody().addStatement("this." + nome + " = " + nome + ";");
    }


    private static void gerarToString(ClassOrInterfaceDeclaration classe, List<String> nomesAtributos) {
        MethodDeclaration metodo = classe.addMethod("toString", Modifier.Keyword.PUBLIC);
        metodo.addMarkerAnnotation("Override");
        metodo.setType("String");

        StringBuilder expressao = new StringBuilder("\"" + classe.getNameAsString() + "{\"");
        for (int i = 0; i < nomesAtributos.size(); i++) {
            String nome = nomesAtributos.get(i);
            String separador = (i == 0) ? "" : ", ";
            expressao.append(" + \"" + separador + nome + "=\" + " + nome);
        }
        expressao.append(" + \"}\"");

        metodo.createBody().addStatement("return " + expressao + ";");
    }

    private static String capitalizar(String texto) {
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    private static boolean existeMetodo(ClassOrInterfaceDeclaration classe, String nome, int numParametros) {
        for (MethodDeclaration metodo : classe.getMethodsByName(nome)) {
            if (metodo.getParameters().size() == numParametros) {
                return true;
            }
        }
        return false;
    }
}