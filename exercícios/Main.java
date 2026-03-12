import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
public class Main {
public static void main(String[] args) {
// Capacidade da fila (Buffer). O que acontece se diminuirmos para 2?
BlockingQueue<Pedido> filaPedidos = new ArrayBlockingQueue<>(5);
Thread produtor = new Thread(new ModuloVendas(filaPedidos));
Thread consumidor = new Thread(new EmissorNotaFiscal(filaPedidos));
System.out.println("--- INICIANDO BLACK FRIDAY SIMULADA ---\n");
produtor.start();
consumidor.start();
}
}