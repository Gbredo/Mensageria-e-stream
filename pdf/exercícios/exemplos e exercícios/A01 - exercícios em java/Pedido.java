package pdf.exercícios;
public class Pedido {
private final int id;
private final String cliente;
private final double valor;
public Pedido(int id, String cliente, double valor) {
this.id = id;
this.cliente = cliente;
this.valor = valor;
}
@Override
public String toString() {
return "Pedido #" + id + " [" + cliente + "] - R$ " + valor;
}
}