import java.util.concurrent.BlockingQueue;
public class EmissorNotaFiscal implements Runnable {
private final BlockingQueue<Pedido> fila;
public EmissorNotaFiscal(BlockingQueue<Pedido> fila) {
this.fila = fila;
}
@Override
public void run() {
try {
while (true) {
// take() bloqueia a thread se a fila estiver vazia (aguarda novos pedidos)
Pedido pedido = fila.take();
if (pedido.toString().contains("POISON_PILL")) {
System.out.println("🛑 Sistema de Notas Fiscais encerrado.");
break;
}
System.out.println("📄 Processando NF-e para: " + pedido);
// Simula lentidão da SEFAZ (1 a 2 segundos por nota)
Thread.sleep(1500);
System.out.println("✅ NF-e emitida com sucesso para Pedido #" + pedido);
}
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
}
}
}