import time
import stomp
import json

class FaturamentoListener(stomp.ConnectionListener):
    def on_message(self, frame):
        transacao = json.loads(frame.body)
        print(f"[FATURAMENTO] Emitindo NF-e para o pedido: {transacao['pedidoId']}")
        time.sleep(2.5)  # Simula latencia de comunicacao com a SEFAZ

# Conexao com Client ID unico para persistencia
conn = stomp.Connection([('localhost', 61613)])
conn.set_listener('', FaturamentoListener())
conn.connect('admin', 'admin', wait=True, client_id='node-faturamento')

# Assinatura Duravel no Topico
conn.subscribe(
    destination='/topic/pagamentos.aprovados',
    id=1,
    ack='auto',
    headers={'activemq.subscriptionName': 'sub-faturamento'}
)

print("[FATURAMENTO] Servico iniciado. Aguardando pagamentos...")
while True:
    time.sleep(1)  # Mantem o processo ativo
