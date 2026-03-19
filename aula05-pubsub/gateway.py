from flask import Flask, request, jsonify
from flask_cors import CORS
import stomp
import json
import uuid

app = Flask(__name__)
CORS(app)  # Permite requisicoes do frontend React (CORS)

def publicar_evento_pagamento(transacao):
    """
    Estabelece conexao com o ActiveMQ via STOMP e publica o evento.
    """
    conn = stomp.Connection([('localhost', 61613)])
    conn.connect('admin', 'admin', wait=True)

    # Converte o dicionario Python para uma string JSON
    mensagem_json = json.dumps(transacao)

    # Publica no Topico de pagamentos aprovados
    conn.send(body=mensagem_json, destination='/topic/pagamentos.aprovados')

    print(f"[GATEWAY] Pagamento aprovado. Evento difundido: {transacao['id']}")
    conn.disconnect()

@app.route('/api/pagamentos/processar', methods=['POST'])
def processar_pagamento():
    dados = request.json

    # Criacao do Objeto de Transferencia (Payload)
    transacao = {
        "id": str(uuid.uuid4()),
        "pedidoId": dados.get("pedidoId"),
        "cliente": dados.get("cliente"),
        "valor": dados.get("valor")
    }

    # Delegacao ao servico de mensageria (Desacoplamento)
    publicar_evento_pagamento(transacao)

    # Retorno HTTP 202 (Accepted) indicando processamento assíncrono
    return jsonify({
        "mensagem": "Pagamento em processamento assíncrono.",
        "id": transacao['id']
    }), 202

if __name__ == '__main__':
    # O Gateway roda na porta 5000
    app.run(debug=True, port=5000)
