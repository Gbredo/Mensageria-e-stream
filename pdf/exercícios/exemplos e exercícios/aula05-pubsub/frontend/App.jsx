import React, { useState } from 'react';

function App() {
  const [pedidoId, setPedidoId] = useState('');
  const [cliente, setCliente] = useState('');
  const [valor, setValor] = useState('');
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);

  const handlePagamento = async (e) => {
    e.preventDefault();
    setLoading(true);
    setStatus(null);

    const transacao = { pedidoId, cliente, valor: parseFloat(valor) };

    try {
      // Aponta para a API Flask (garanta flask-cors ativo)
      const response = await fetch('http://localhost:5000/api/pagamentos/processar', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(transacao),
      });

      if (response.status === 202) {
        const data = await response.json();
        setStatus({ tipo: 'sucesso', mensagem: data.mensagem + ' ID: ' + data.id });
        setPedidoId(''); setCliente(''); setValor('');
      } else {
        setStatus({ tipo: 'erro', mensagem: 'Falha na comunicacao com o Gateway.' });
      }
    } catch (error) {
      setStatus({ tipo: 'erro', mensagem: 'Erro de rede: O servidor esta operante?' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '500px', margin: '50px auto', fontFamily: 'sans-serif' }}>
      <h2>E-commerce: Checkout</h2>
      <form onSubmit={handlePagamento} style={{ display: 'flex', flexDirection: 'column', gap: '15px' }}>
        <input
          type="text" placeholder="ID do Pedido (Ex: PED-100)" required
          value={pedidoId} onChange={e => setPedidoId(e.target.value)}
        />
        <input
          type="text" placeholder="Nome do Cliente" required
          value={cliente} onChange={e => setCliente(e.target.value)}
        />
        <input
          type="number" placeholder="Valor (R$)" step="0.01" required
          value={valor} onChange={e => setValor(e.target.value)}
        />
        <button type="submit" disabled={loading}
          style={{ padding: '10px', backgroundColor: '#0056b3', color: 'white', border: 'none', cursor: 'pointer' }}>
          {loading ? 'Processando...' : 'Confirmar Pagamento'}
        </button>
      </form>

      {status && (
        <div style={{
          marginTop: '20px', padding: '15px', borderRadius: '5px',
          backgroundColor: status.tipo === 'sucesso' ? '#d4edda' : '#f8d7da',
          color: status.tipo === 'sucesso' ? '#155724' : '#721c24'
        }}>
          <strong>Status:</strong> {status.mensagem}
        </div>
      )}
    </div>
  );
}

export default App;
