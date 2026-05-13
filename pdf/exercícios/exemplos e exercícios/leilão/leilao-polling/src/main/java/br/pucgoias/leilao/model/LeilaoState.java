package br.pucgoias.leilao.model;

import java.util.ArrayList;
import java.util.List;

public class LeilaoState {

    private String item;
    private String descricao;
    private String imagemUrl;
    private double lanceMinimo;
    private double lanceAtual;
    private String maiorLancador;
    private long tempoRestante; // segundos
    private boolean ativo;
    private List<Lance> historico;
    private long totalRequisicoes; // contador para mostrar o custo do polling

    public LeilaoState() {
        this.historico = new ArrayList<>();
    }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getImagemUrl() { return imagemUrl; }
    public void setImagemUrl(String imagemUrl) { this.imagemUrl = imagemUrl; }

    public double getLanceMinimo() { return lanceMinimo; }
    public void setLanceMinimo(double lanceMinimo) { this.lanceMinimo = lanceMinimo; }

    public double getLanceAtual() { return lanceAtual; }
    public void setLanceAtual(double lanceAtual) { this.lanceAtual = lanceAtual; }

    public String getMaiorLancador() { return maiorLancador; }
    public void setMaiorLancador(String maiorLancador) { this.maiorLancador = maiorLancador; }

    public long getTempoRestante() { return tempoRestante; }
    public void setTempoRestante(long tempoRestante) { this.tempoRestante = tempoRestante; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public List<Lance> getHistorico() { return historico; }
    public void setHistorico(List<Lance> historico) { this.historico = historico; }

    public long getTotalRequisicoes() { return totalRequisicoes; }
    public void setTotalRequisicoes(long totalRequisicoes) { this.totalRequisicoes = totalRequisicoes; }
}
