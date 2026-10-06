public class Livro {

    private final String isbn;

    private String titulo;

    private int paginas;

    public Livro(String isbn) {
        this.isbn = isbn;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public int getPaginas() {
        return paginas;
    }

    public void setPaginas(int paginas) {
        this.paginas = paginas;
    }

    @Override
    public String toString() {
        return "Livro{" + "isbn=" + isbn + ", titulo=" + titulo + ", paginas=" + paginas + "}";
    }
}
