import time
import stomp
import json

class LogisticaListener(stomp.ConnectionListener):
    def on_message(self, frame):
        transacao = json.loads(frame.body)
        print(f"[LOGISTICA] Separando itens no CD para o pedido: {transacao['pedidoId']}")
        time.sleep(1.5)  # Simula operacao de I/O no banco de dados de estoque

# Conexao com Client ID unico para persistencia
conn = stomp.Connection([('localhost', 61613)])
conn.set_listener('', LogisticaListener())
conn.connect('admin', 'admin', wait=True, client_id='node-logistica')

# Assinatura Duravel no Topico
conn.subscribe(
    destination='/topic/pagamentos.aprovados',
    id=1,
    ack='auto',
    headers={'activemq.subscriptionName': 'sub-logistica'}
)

print("[LOGISTICA] Servico iniciado. Aguardando liberacao de estoque...")
while True:
    time.sleep(1)
