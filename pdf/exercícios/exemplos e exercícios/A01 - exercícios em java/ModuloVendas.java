package pdf.exercícios;
import java.util.concurrent.BlockingQueue;
public class ModuloVendas implements Runnable {
private final BlockingQueue<Pedido> fila;
public ModuloVendas(BlockingQueue<Pedido> fila) {
this.fila = fila;
}
@Override
public void run() {
try {
for (int i = 1; i <= 10; i++) {
Pedido p = new Pedido(i, "Cliente_" + i, 100.0 + i);
System.out.println("🛒 Venda realizada: " + p);
fila.put(p); // Enfileira instantaneamente
// Simula alta vazão de vendas (apenas 200ms entre vendas)
Thread.sleep(200);
}
// Envia sinal de parada (Poison Pill)
fila.put(new Pedido(0, "POISON_PILL", 0));
System.out.println("🏁 Módulo de Vendas finalizou o turno (Loja Fechada)!");
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
}
}
}