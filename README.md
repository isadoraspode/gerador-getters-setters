# Gerador de getters e setters para classes Java

Trabalho Prático 2 da disciplina de Linguagens de Programação (PUCRS, Escola Politécnica, 2026/II).

**Integrante:** Isadora Spode Cardoso

**Vídeo de apresentação:** https://youtu.be/qRG2XqhpdDU

## O problema

Escrever getters e setters à mão é repetitivo. Esta ferramenta lê uma classe Java, descobre quais atributos ela tem e quais métodos de acesso já existem, e gera os que estão faltando.

A entrada não é tratada como texto. O arquivo é convertido em uma árvore sintática abstrata (AST), e toda a análise e a geração de código são feitas sobre os nós dessa árvore.

## Requisitos do enunciado

| Requisito | Resposta |
|---|---|
| 1. Artefato de entrada | Um arquivo de código-fonte `.java` contendo uma ou mais classes |
| 2. Linguagem reconhecida | Java |
| 3. Parser e representação sintática | Biblioteca JavaParser (versão 3.26.4), que faz a análise léxica e sintática e entrega uma AST |
| 4. Elementos sintáticos relevantes | `CompilationUnit`, `ClassOrInterfaceDeclaration`, `FieldDeclaration`, `VariableDeclarator`, `MethodDeclaration`, `PrimitiveType` e os modificadores `static` e `final` |
| 5. Análise e transformação | Análise: identificar atributos e verificar quais getters e setters já existem. Transformação: inserir na AST os métodos que faltam |
| 6. Saída | Um novo arquivo `.java` na pasta `saida`, com a classe original mais os métodos gerados, e um resumo no terminal |

## Como a ferramenta funciona

1. `StaticJavaParser.parse(...)` lê o arquivo e devolve o `CompilationUnit`, a raiz da AST.
2. `findAll(ClassOrInterfaceDeclaration.class)` localiza os nós que são declarações de classe.
3. Para cada classe, `getFields()` devolve os nós `FieldDeclaration` (declarações de atributo), e `getVariables()` devolve os `VariableDeclarator` de cada declaração, de onde saem o nome e o tipo.
4. Para cada atributo, a ferramenta calcula os nomes esperados (`getNome`, `setNome`) e procura, entre os nós `MethodDeclaration` da classe, um método com esse nome e a quantidade certa de parâmetros (zero para getter, um para setter).
5. Se o método não existe, `addMethod(...)` cria um novo nó `MethodDeclaration` como filho da classe, com tipo, parâmetro e corpo.
6. `toString()` da árvore converte a AST modificada de volta em código Java, que é gravado na pasta `saida`.

Regras aplicadas:

- atributos `static` são ignorados, pois pertencem à classe e não ao objeto;
- o getter de um atributo `boolean` usa o prefixo `is` em vez de `get`;
- métodos já existentes nunca são duplicados.

## Como executar

Pré-requisitos: JDK 21 e Apache Maven instalados e configurados (`java -version` e `mvn -version` devem funcionar).

Na pasta do projeto:

```
mvn -q compile exec:java "-Dexec.args=exemplos/Pessoa.java"
```

Opções, informadas depois do caminho do arquivo:

- `--arvore`: imprime a AST completa do arquivo, sem gerar nada;
- `--tostring`: além dos getters e setters, gera o método `toString` (extensão).

```
mvn -q compile exec:java "-Dexec.args=exemplos/Pessoa.java --arvore"
mvn -q compile exec:java "-Dexec.args=exemplos/Livro.java --tostring"
```

No PowerShell, as aspas em volta de todo o argumento `-Dexec.args=...` são necessárias.

### Problemas resolvidos na configuração

- **`mvn` não reconhecido:** a pasta `bin` do Maven precisa estar na variável `Path`, e o terminal precisa ser reaberto.
- **`JAVA_HOME is not defined correctly`:** a variável `JAVA_HOME` deve apontar para a pasta do JDK, sem `\bin` no final.
- **`ClassNotFoundException: tp2.Main`:** o Maven só compila o que está em `src/main/java`, e a pasta do pacote (`tp2`) precisa corresponder à linha `package tp2;`.

## Estrutura do projeto

```
pom.xml                      configuração do Maven e dependências
src/main/java/tp2/Main.java  código da ferramenta
exemplos/                    arquivos de entrada
saida/                       arquivos gerados pela ferramenta
```

## Exemplo 1: classe simples

Entrada (`exemplos/Pessoa.java`):

```java
public class Pessoa {
    private String nome;
    private int idade;
}
```

Resultado no terminal:

```
Classe: Pessoa
  Gerado: getNome
  Gerado: setNome
  Gerado: getIdade
  Gerado: setIdade
Arquivo gerado: saida\Pessoa.java
```

Artefato gerado (`saida/Pessoa.java`):

```java
public class Pessoa {

    private String nome;

    private int idade;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getIdade() {
        return idade;
    }

    public void setIdade(int idade) {
        this.idade = idade;
    }
}
```

Elementos da AST utilizados: `CompilationUnit`, `ClassOrInterfaceDeclaration`, `FieldDeclaration`, `VariableDeclarator`, `MethodDeclaration`.

Ações realizadas: a ferramenta encontrou a classe e seus dois atributos. Como a classe não tinha nenhum método, criou quatro nós `MethodDeclaration`, copiando o nó de tipo de cada atributo para o retorno do getter e para o parâmetro do setter.

## Exemplo 2: classe que já tem alguns métodos

Entrada (`exemplos/Produto.java`):

```java
public class Produto {
    private String nome;
    private double preco;

    public String getNome() {
        return nome;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }
}
```

Resultado no terminal:

```
Classe: Produto
  Gerado: setNome
  Gerado: getPreco
Arquivo gerado: saida\Produto.java
```

Artefato gerado (`saida/Produto.java`):

```java
public class Produto {

    private String nome;

    private double preco;

    public String getNome() {
        return nome;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public double getPreco() {
        return preco;
    }
}
```

Elementos da AST utilizados: os mesmos do exemplo 1, com destaque para a consulta aos nós `MethodDeclaration` já existentes e à lista de parâmetros de cada um.

Ações realizadas: para cada atributo, a ferramenta procurou um método com o nome esperado e a quantidade certa de parâmetros. Encontrou `getNome` e `setPreco`, então gerou apenas `setNome` e `getPreco`. Como a busca é feita sobre declarações de método na árvore, um `getNome` escrito em um comentário ou em uma string não seria confundido com um método real.

## Exemplo 3: atributos `static` e `boolean`

Entrada (`exemplos/Conta.java`):

```java
public class Conta {
    private static int totalContas;
    private String titular;
    private double saldo;
    private boolean ativa;
}
```

Resultado no terminal:

```
Classe: Conta
  Ignorado (static): totalContas
  Gerado: getTitular
  Gerado: setTitular
  Gerado: getSaldo
  Gerado: setSaldo
  Gerado: isAtiva
  Gerado: setAtiva
Arquivo gerado: saida\Conta.java
```

Trecho do artefato gerado (`saida/Conta.java`):

```java
    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }
```

Elementos da AST utilizados: além dos anteriores, o modificador `static` do `FieldDeclaration` (consultado com `isStatic()`) e o nó `PrimitiveType`, usado para identificar o tipo `boolean`.

Ações realizadas: o atributo `totalContas` foi ignorado por ser estático. Para `ativa`, o nó de tipo foi comparado com o tipo primitivo `boolean`, e o getter recebeu o prefixo `is`, seguindo a convenção do Java.

## Extensão: atributos `final` e geração de `toString`

A extensão acrescenta duas funcionalidades, marcadas no código com o comentário `EXTENSAO`:

- **Nova verificação:** atributos `final` não recebem setter, pois não podem ser alterados depois de inicializados (um setter para eles nem compilaria).
- **Nova geração de código:** com a opção `--tostring`, a ferramenta gera também o método `toString`, listando os atributos do objeto.

Entrada (`exemplos/Livro.java`):

```java
public class Livro {
    private final String isbn;
    private String titulo;
    private int paginas;

    public Livro(String isbn) {
        this.isbn = isbn;
    }
}
```

Comando:

```
mvn -q compile exec:java "-Dexec.args=exemplos/Livro.java --tostring"
```

Resultado no terminal:

```
Classe: Livro
  Gerado: getIsbn
  Sem setter (final): isbn
  Gerado: getTitulo
  Gerado: setTitulo
  Gerado: getPaginas
  Gerado: setPaginas
  Gerado: toString
Arquivo gerado: saida\Livro.java
```

Trecho do artefato gerado (`saida/Livro.java`):

```java
    @Override
    public String toString() {
        return "Livro{" + "isbn=" + isbn + ", titulo=" + titulo + ", paginas=" + paginas + "}";
    }
```

Elementos da AST utilizados: o modificador `final` do `FieldDeclaration` (consultado com `isFinal()`), um novo `MethodDeclaration` com uma anotação (`@Override`) e a lista de atributos não estáticos coletada durante o percurso.

Ações realizadas: o atributo `isbn` recebeu apenas o getter. Ao final do percurso da classe, como não havia um `toString` sem parâmetros, a ferramenta criou o método, montando a expressão de retorno a partir dos nomes dos atributos. O construtor da classe é um nó `ConstructorDeclaration` e não interfere na verificação dos métodos.

## Referências

- JavaParser, biblioteca de análise sintática de código Java: https://javaparser.org e https://github.com/javaparser/javaparser
- Apache Maven, ferramenta de build: https://maven.apache.org
- Exec Maven Plugin, usado para executar a ferramenta: https://www.mojohaus.org/exec-maven-plugin/
- Eclipse Temurin, distribuição do JDK: https://adoptium.net
- Claude (Anthropic), assistente de IA usado como apoio da configuração do ambiente e na documentação: https://claude.ai
