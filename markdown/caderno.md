mensagem síncrona vs assíncrona

síncrona | A (online) <-> B (online)

Assíncrona

A -> OKb ... T1
B -> OKOKb ... T2

A -> QUEUE -> B

A: Produtor   |-> UNDERFLOW
B: Consumidor |-> OVERFLOW

BROKER

sincrona
clientes -- REQ -> Rest -> DQ (data quality) <- 5-10 segundos
        <- RES --
CADASTRO PF/PJ

timeout

Assíncrona

