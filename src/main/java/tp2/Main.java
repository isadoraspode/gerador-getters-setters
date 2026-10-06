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
        // O caminho do arquivo de entrada vem pela linha de comando
        if (args.length == 0) {
            System.out.println("Uso: informe o caminho de um arquivo .java");
            return;
        }

        // O JavaParser faz a analise lexica e sintatica do arquivo
        // e devolve a raiz da arvore sintatica (AST): o CompilationUnit
        Path entrada = Path.of(args[0]);
        CompilationUnit arvore = StaticJavaParser.parse(entrada);

        // Opcional: com "--arvore" como segundo argumento, mostra a AST completa
        if (args.length > 1 && args[1].equals("--arvore")) {
            System.out.println(new YamlPrinter(true).output(arvore));
            return;
        }

        // EXTENSAO: com "--tostring" como segundo argumento, gera tambem o toString
        boolean comToString = args.length > 1 && args[1].equals("--tostring");

        // Busca na arvore todos os nos que sao declaracoes de classe
        for (ClassOrInterfaceDeclaration classe : arvore.findAll(ClassOrInterfaceDeclaration.class)) {
            System.out.println("Classe: " + classe.getNameAsString());

            // Guarda os nomes dos atributos do objeto, para usar no toString
            List<String> nomesAtributos = new ArrayList<>();

            // Dentro da classe, pega os nos que sao declaracoes de atributo
            for (FieldDeclaration campo : classe.getFields()) {

                // Uma declaracao pode ter mais de uma variavel (ex.: int x, y;)
                for (VariableDeclarator variavel : campo.getVariables()) {
                    String nome = variavel.getNameAsString();

                    // Atributos static pertencem a classe, nao ao objeto: ficam de fora
                    if (campo.isStatic()) {
                        System.out.println("  Ignorado (static): " + nome);
                        continue;
                    }
                    nomesAtributos.add(nome);

                    // Convencao do Java: getter de boolean comeca com "is" (ativa -> isAtiva)
                    boolean ehBoolean = variavel.getType().equals(PrimitiveType.booleanType());
                    String prefixo = ehBoolean ? "is" : "get";

                    // Nomes que os metodos devem ter: nome -> getNome / setNome
                    String nomeGetter = prefixo + capitalizar(nome);
                    String nomeSetter = "set" + capitalizar(nome);

                    // So gera o que ainda nao existe na classe
                    if (!existeMetodo(classe, nomeGetter, 0)) {
                        gerarGetter(classe, variavel, nomeGetter);
                        System.out.println("  Gerado: " + nomeGetter);
                    }
                    // EXTENSAO: atributo final nao pode ser alterado, entao nao recebe setter
                    if (campo.isFinal()) {
                        System.out.println("  Sem setter (final): " + nome);
                    } else if (!existeMetodo(classe, nomeSetter, 1)) {
                        gerarSetter(classe, variavel, nomeSetter);
                        System.out.println("  Gerado: " + nomeSetter);
                    }
                }
            }

            // EXTENSAO: gera o toString, se foi pedido e a classe ainda nao tem um
            if (comToString && !existeMetodo(classe, "toString", 0)) {
                gerarToString(classe, nomesAtributos);
                System.out.println("  Gerado: toString");
            }
        }

        // Converte a arvore modificada de volta para codigo Java
        // e grava na pasta "saida", com o mesmo nome do arquivo de entrada
        Path pastaSaida = Path.of("saida");
        Files.createDirectories(pastaSaida);
        Path arquivoSaida = pastaSaida.resolve(entrada.getFileName());
        Files.writeString(arquivoSaida, arvore.toString());
        System.out.println("Arquivo gerado: " + arquivoSaida);
    }

    // Cria na arvore o no de um metodo como:
    //   public String getNome() { return nome; }
    private static void gerarGetter(ClassOrInterfaceDeclaration classe, VariableDeclarator variavel, String nomeGetter) {
        String nome = variavel.getNameAsString();

        // Adiciona um novo MethodDeclaration como filho da classe
        MethodDeclaration metodo = classe.addMethod(nomeGetter, Modifier.Keyword.PUBLIC);

        // O tipo de retorno e uma copia do no de tipo do atributo
        metodo.setType(variavel.getType().clone());

        // Corpo do metodo
        metodo.createBody().addStatement("return " + nome + ";");
    }

    // Cria na arvore o no de um metodo como:
    //   public void setNome(String nome) { this.nome = nome; }
    private static void gerarSetter(ClassOrInterfaceDeclaration classe, VariableDeclarator variavel, String nomeSetter) {
        String nome = variavel.getNameAsString();

        // Metodos criados assim ja tem retorno void
        MethodDeclaration metodo = classe.addMethod(nomeSetter, Modifier.Keyword.PUBLIC);

        // O parametro tem o mesmo tipo e o mesmo nome do atributo
        metodo.addParameter(variavel.getType().clone(), nome);

        // Corpo do metodo
        metodo.createBody().addStatement("this." + nome + " = " + nome + ";");
    }

    // EXTENSAO: cria na arvore o no de um metodo como:
    //   @Override
    //   public String toString() { return "Livro{" + "titulo=" + titulo + "}"; }
    private static void gerarToString(ClassOrInterfaceDeclaration classe, List<String> nomesAtributos) {
        MethodDeclaration metodo = classe.addMethod("toString", Modifier.Keyword.PUBLIC);
        metodo.addMarkerAnnotation("Override");
        metodo.setType("String");

        // Monta a expressao que junta o nome da classe e cada atributo com seu valor
        StringBuilder expressao = new StringBuilder("\"" + classe.getNameAsString() + "{\"");
        for (int i = 0; i < nomesAtributos.size(); i++) {
            String nome = nomesAtributos.get(i);
            String separador = (i == 0) ? "" : ", ";
            expressao.append(" + \"" + separador + nome + "=\" + " + nome);
        }
        expressao.append(" + \"}\"");

        metodo.createBody().addStatement("return " + expressao + ";");
    }

    // Deixa a primeira letra maiuscula: "nome" -> "Nome"
    private static String capitalizar(String texto) {
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    }

    // Procura, entre os nos MethodDeclaration da classe, um metodo
    // com o nome e a quantidade de parametros informados
    private static boolean existeMetodo(ClassOrInterfaceDeclaration classe, String nome, int numParametros) {
        for (MethodDeclaration metodo : classe.getMethodsByName(nome)) {
            if (metodo.getParameters().size() == numParametros) {
                return true;
            }
        }
        return false;
    }
}