import pytest
from unittest.mock import patch
from gateway import app

@pytest.fixture
def client():
    # Configura o cliente de testes nativo do Flask
    app.config['TESTING'] = True
    with app.test_client() as client:
        yield client

# Interceptamos a funcao de publicacao para que ela nao tente conectar na porta 61613
@patch('gateway.publicar_evento_pagamento')
def test_deve_receber_pagamento_e_retornar_http_202(mock_publicar, client):
    # 1. Arrange (Preparacao do cenario)
    payload_mock = {
        "pedidoId": "PED-999",
        "cliente": "Rafael Martins",
        "valor": 1500.00
    }

    # 2. Act (Acao da requisicao HTTP)
    response = client.post('/api/pagamentos/processar', json=payload_mock)

    # 3. Assert (Validacoes)
    assert response.status_code == 202
    assert "processamento" in response.get_json()['mensagem']

    # Verifica se a API de fato tentou repassar os dados para o Broker
    mock_publicar.assert_called_once()
    args, kwargs = mock_publicar.call_args
    assert args[0]['pedidoId'] == "PED-999"
    assert 'id' in args[0]  # Garante que o UUID foi gerado
